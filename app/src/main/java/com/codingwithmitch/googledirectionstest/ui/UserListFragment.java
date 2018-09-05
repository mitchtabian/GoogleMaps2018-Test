package com.codingwithmitch.googledirectionstest.ui;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.codingwithmitch.googledirectionstest.ICallback;
import com.codingwithmitch.googledirectionstest.R;
import com.codingwithmitch.googledirectionstest.adapters.UserRecyclerAdapter;
import com.codingwithmitch.googledirectionstest.models.PolylineData;
import com.codingwithmitch.googledirectionstest.models.User;
import com.codingwithmitch.googledirectionstest.models.UserLocation;
import com.codingwithmitch.googledirectionstest.util.ViewWeightAnimationWrapper;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.DistanceMatrixApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.PendingResult;
import com.google.maps.internal.PolylineEncoding;
import com.google.maps.model.AddressType;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.DistanceMatrixElement;
import com.google.maps.model.DistanceMatrixRow;
import com.google.maps.model.GeocodedWaypoint;
import com.google.maps.model.GeocodingResult;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.codingwithmitch.googledirectionstest.Constants.MAPVIEW_BUNDLE_KEY;

public class UserListFragment extends Fragment implements
        OnMapReadyCallback,
        UserRecyclerAdapter.UserListRecyclerClickListener,
        View.OnClickListener,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnInfoWindowClickListener,
        GoogleMap.OnPolylineClickListener
{

    private static final String TAG = "UserListFragment";

    private static final int MAP_LAYOUT_STATE_CONTRACTED = 0;
    private static final int MAP_LAYOUT_STATE_EXPANDED = 1;

    //widgets
    private RecyclerView mUserListRecyclerView;
    private MapView mMapView;
    private RelativeLayout mMapContainer;
    private ProgressBar mProgressBar;


    //vars
    private ArrayList<User> mUserList = new ArrayList<>();
    private ArrayList<UserLocation> mUserLocations = new ArrayList<>();
    private UserRecyclerAdapter mUserRecyclerAdapter;
    private FirebaseFirestore mDb;
    private GoogleMap mGoogleMap;
    private LatLngBounds mMapBoundary;
    private int mMapLayoutState = 0;
    private com.google.maps.model.LatLng mUserPosition ;
    private GeoApiContext mGeoApiContext;
    private ArrayList<PolylineData> mPolyLinesData = new ArrayList<>();



    public static UserListFragment newInstance(){
        return new UserListFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate: called.");
        if(getArguments() != null){
            final ArrayList<User> users = getArguments().getParcelableArrayList(getString(R.string.intent_user_list));
            mUserList.addAll(users);

            final ArrayList<UserLocation> locations = getArguments().getParcelableArrayList(getString(R.string.intent_user_locations));
            mUserLocations.addAll(locations);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view  = inflater.inflate(R.layout.fragment_user_list, container, false);
        mUserListRecyclerView = view.findViewById(R.id.user_list_recycler_view);
        mMapContainer = view.findViewById(R.id.map_container);
        mProgressBar = view.findViewById(R.id.progressBar);

        view.findViewById(R.id.btn_full_screen_map).setOnClickListener(this);

        mDb = FirebaseFirestore.getInstance();

        // *** IMPORTANT ***
        // MapView requires that the Bundle you pass contain _ONLY_ MapView SDK
        // objects or sub-Bundles.
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }
        mMapView = (MapView) view.findViewById(R.id.user_list_map);
        mMapView.onCreate(mapViewBundle);

        mMapView.getMapAsync(this);

        initUserListRecyclerView();
        hideSoftKeyboard();

        mGeoApiContext = new GeoApiContext.Builder()
                .apiKey(getString(R.string.google_maps_api_key))
                .build();
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle);
        }

        mMapView.onSaveInstanceState(mapViewBundle);
    }

    private void expandMapAnimation(){
        ViewWeightAnimationWrapper mapAnimationWrapper = new ViewWeightAnimationWrapper(mMapContainer);
        ObjectAnimator mapAnimation = ObjectAnimator.ofFloat(mapAnimationWrapper,
                "weight",
                mapAnimationWrapper.getWeight(),
                100);
        mapAnimation.setDuration(800);

        ViewWeightAnimationWrapper recyclerAnimationWrapper = new ViewWeightAnimationWrapper(mUserListRecyclerView);
        ObjectAnimator recyclerAnimation = ObjectAnimator.ofFloat(recyclerAnimationWrapper,
                "weight",
                mapAnimationWrapper.getWeight(),
                0);
        recyclerAnimation.setDuration(800);

        recyclerAnimation.start();
        mapAnimation.start();
    }

    private void contractMapAnimation(){
        ViewWeightAnimationWrapper mapAnimationWrapper = new ViewWeightAnimationWrapper(mMapContainer);
        ObjectAnimator mapAnimation = ObjectAnimator.ofFloat(mapAnimationWrapper,
                "weight",
                mapAnimationWrapper.getWeight(),
                50);
        mapAnimation.setDuration(800);

        ViewWeightAnimationWrapper recyclerAnimationWrapper = new ViewWeightAnimationWrapper(mUserListRecyclerView);
        ObjectAnimator recyclerAnimation = ObjectAnimator.ofFloat(recyclerAnimationWrapper,
                "weight",
                mapAnimationWrapper.getWeight(),
                50);
        recyclerAnimation.setDuration(800);

        recyclerAnimation.start();
        mapAnimation.start();
    }

    private void initUserListRecyclerView(){
        mUserRecyclerAdapter = new UserRecyclerAdapter(mUserList, this);
        mUserListRecyclerView.setAdapter(mUserRecyclerAdapter);
        mUserListRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    }



    private void addMapMarkers(){
        if(mGoogleMap != null){
            for(UserLocation userLocation: mUserLocations){

                try{
                    MarkerOptions markerOptions = new MarkerOptions().position(
                            new LatLng(userLocation.getGeo_point().getLatitude(), userLocation.getGeo_point().getLongitude())
                    ).title(userLocation.getUser().getUsername());
                    mGoogleMap.addMarker(markerOptions);

                    // set the current users location to global variable
                    if(FirebaseAuth.getInstance().getUid().equals(userLocation.getUser().getUser_id())){
                        mUserPosition = new com.google.maps.model.LatLng(
                                userLocation.getGeo_point().getLatitude(),
                                userLocation.getGeo_point().getLongitude()
                        );
                    }
                }catch (NullPointerException e){
                    Log.e(TAG, "addMapMarkers: NullPointerException: " + e.getMessage() );
                }

            }
            setCameraView();
            mGoogleMap.setOnMarkerClickListener(this);
            mGoogleMap.setOnInfoWindowClickListener(this);
        }
    }

    /**
     * Determines the view boundary then sets the camera
     */
    private void setCameraView(){

        // Set a random boundary to start
        double bottomBoundary = -90;
        double leftBoundary = -180;
        double topBoundary = 90;
        double rightBoundary = 180;

        for(UserLocation userLocation: mUserLocations){

            try{
                // Bottom Boundary
                if(userLocation.getGeo_point().getLatitude() < bottomBoundary){
                    bottomBoundary = userLocation.getGeo_point().getLatitude();
                }

                // Left Boundary
                if(userLocation.getGeo_point().getLongitude() < leftBoundary){
                    leftBoundary = userLocation.getGeo_point().getLongitude();
                }

                // Top Boundary
                if(userLocation.getGeo_point().getLatitude() > topBoundary){
                    topBoundary = userLocation.getGeo_point().getLatitude();
                }

                // Right Boundary
                if(userLocation.getGeo_point().getLongitude() > rightBoundary){
                    rightBoundary = userLocation.getGeo_point().getLongitude();
                }

            }catch (NullPointerException e){
                Log.e(TAG, "addMapMarkers: NullPointerException: " + e.getMessage() );
            }

        }
        mMapBoundary = new LatLngBounds(
                new LatLng(bottomBoundary,leftBoundary),
                new LatLng(topBoundary, rightBoundary)
        );

        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(mMapBoundary, 0));
    }

    @Override
    public void onUserSelected(int position) {
        Log.d(TAG, "onUserSelected: selected a user: " + mUserList.get(position).toString());
        String selectedUserId = mUserList.get(position).getUser_id();

        for(UserLocation userLocation: mUserLocations){
            if(selectedUserId.equals(userLocation.getUser().getUser_id())){
                mGoogleMap.animateCamera(
                        CameraUpdateFactory.newLatLng(
                                new LatLng(userLocation.getGeo_point().getLatitude(), userLocation.getGeo_point().getLongitude())),
                        600,
                        null
                );
                break;
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_full_screen_map:{

                if(mMapLayoutState == MAP_LAYOUT_STATE_CONTRACTED){
                    mMapLayoutState = MAP_LAYOUT_STATE_EXPANDED;
                    expandMapAnimation();
                }
                else if(mMapLayoutState == MAP_LAYOUT_STATE_EXPANDED){
                    mMapLayoutState = MAP_LAYOUT_STATE_CONTRACTED;
                    contractMapAnimation();
                }
                break;
            }
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if(marker.isInfoWindowShown()){
            marker.hideInfoWindow();
        }
        else{
            inflateMarkerInfoWindow(marker);
        }
        return false;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        Log.d(TAG, "onInfoWindowClick: marker id: " + marker.getId().replace("m", ""));

        final UserLocation userLocation = mUserLocations.get(Integer.parseInt(marker.getId().replace("m", "")));
        if(userLocation.getUser().getUser_id().equals(FirebaseAuth.getInstance().getUid())){
            marker.hideInfoWindow();
        }
        else{
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Calculate a path to " + userLocation.getUser().getUsername() + "?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                            calculateDirections(userLocation);
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                            dialog.cancel();
                        }
                    });
            final AlertDialog alert = builder.create();
            alert.show();
        }

    }

    /**
     * get duration and distance of route
     * @param userLocation
     */
    private void calculateDurationAndDistance(UserLocation userLocation){
        Log.d(TAG, "calculateDurationAndDistance: calculating duration and distance.");
        showProgressBar();

        com.google.maps.model.LatLng destination = new com.google.maps.model.LatLng(
                userLocation.getGeo_point().getLatitude(),
                userLocation.getGeo_point().getLongitude()
        );
        DistanceMatrixApiRequest matrix = new DistanceMatrixApiRequest(mGeoApiContext);

        matrix.origins(mUserPosition);
        matrix.destinations(destination).setCallback(new PendingResult.Callback<DistanceMatrix>() {
            @Override
            public void onResult(DistanceMatrix results) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                Log.d(TAG, "onResult: destination addresses: " + gson.toJson(results.destinationAddresses));
                Log.d(TAG, "onResult: origin addresses: " + gson.toJson(results.originAddresses));

                for(DistanceMatrixRow row: results.rows){
                    for(DistanceMatrixElement element: row.elements){
                        Log.d(TAG, "onResult: element: " + element.toString());
                    }
                }
                hideProgressBar();
            }

            @Override
            public void onFailure(Throwable e) {
                Log.e(TAG, "onFailure: " + e.getMessage() );
                hideProgressBar();
            }
        });
    }

    private void calculateDirections(UserLocation userLocation){
        Log.d(TAG, "calculateDirections: calculating directions.");

        com.google.maps.model.LatLng destination = new com.google.maps.model.LatLng(
                userLocation.getGeo_point().getLatitude(),
                userLocation.getGeo_point().getLongitude()
        );
        DirectionsApiRequest directions = new DirectionsApiRequest(mGeoApiContext);

        directions.alternatives(true);
        directions.origin(mUserPosition);
        directions.destination(destination).setCallback(new PendingResult.Callback<DirectionsResult>() {
            @Override
            public void onResult(DirectionsResult result) {

                addPolyLinesToMap(result);

                hideProgressBar();
            }

            @Override
            public void onFailure(Throwable e) {
                Log.e(TAG, "onFailure: " + e.getMessage() );
                hideProgressBar();
            }
        });
    }

    private void addPolyLinesToMap(final DirectionsResult result){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                int count = 0;

                for(DirectionsRoute route: result.routes){
                    Log.d(TAG, "onResult: leg: " + route.legs[0].toString());
                    count++;
                    List<com.google.maps.model.LatLng> decodedPath = PolylineEncoding.decode(route.overviewPolyline.getEncodedPath());

                    List<LatLng> newDecodedPath = new ArrayList<>();

                    // This loops through all the LatLng coordinates of ONE polyline.
                    // Uncomment the log for a demonstration
                    for(com.google.maps.model.LatLng latLng: decodedPath){

//                        Log.d(TAG, "run: latlng: " + latLng.toString());

                        newDecodedPath.add(new LatLng(
                                latLng.lat,
                                latLng.lng
                        ));
                    }
                    Polyline polyline = mGoogleMap.addPolyline(new PolylineOptions().addAll(newDecodedPath));
                    polyline.setColor(ContextCompat.getColor(getActivity(), R.color.darkGrey));
                    polyline.setClickable(true);
                    mPolyLinesData.add(new PolylineData(polyline, route.legs[0]));
                }
            }
        });
    }

    @Override
    public void onPolylineClick(Polyline polyline) {

        int index = 0;
        for(PolylineData polylineData: mPolyLinesData){
            index++;
            if(polyline.getId().equals(polylineData.getPolyline().getId())){
                polylineData.getPolyline().setColor(ContextCompat.getColor(getActivity(), R.color.blue1));
                polylineData.getPolyline().setZIndex(1);
                Log.d(TAG, "onPolylineClick: data: " + polylineData.getLeg().endLocation);

                LatLng endLocation = new LatLng(
                        polylineData.getLeg().endLocation.lat,
                        polylineData.getLeg().endLocation.lng
                );

                Marker marker = mGoogleMap.addMarker(new MarkerOptions()
                        .position(endLocation)
                        .title("Trip #" + index)
                        .snippet("Duration: " + polylineData.getLeg().duration
                                + "\n" + "Distance: " + polylineData.getLeg().distance
                        ));

                marker.showInfoWindow();
            }
            else{
                polylineData.getPolyline().setColor(ContextCompat.getColor(getActivity(), R.color.darkGrey));
                polylineData.getPolyline().setZIndex(0);
            }
        }

    }

    private void inflateMarkerInfoWindow(Marker marker){
        Log.d(TAG, "onInfoWindowClick: marker id: " + marker.getId().replace("m", ""));

        final UserLocation userLocation = mUserLocations.get(Integer.parseInt(marker.getId().replace("m", "")));
        if(userLocation.getUser().getUser_id().equals(FirebaseAuth.getInstance().getUid())){
            marker.setSnippet("This is you");
            marker.showInfoWindow();
        }
        else{
            marker.setSnippet("Calculate route to " + marker.getTitle() + "?");
            marker.showInfoWindow();
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
        mMapView.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mMapView.onStop();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mGoogleMap = map;
        addMapMarkers();
        mGoogleMap.setOnPolylineClickListener(this);
    }

    @Override
    public void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mMapView.onDestroy();
        if(mUserLocations != null){
            mUserLocations.clear();
        }
        if(mUserList != null){
            mUserList.clear();
        }
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }


    private void hideSoftKeyboard(){
        //Hide the soft keyboard
        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
    }

    private void showProgressBar(){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mProgressBar.setVisibility(View.VISIBLE);
            }
        });
    }

    private void hideProgressBar(){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mProgressBar.setVisibility(View.GONE);
            }
        });
    }
}



















