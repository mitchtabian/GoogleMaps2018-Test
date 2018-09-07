package com.codingwithmitch.googledirectionstest.ui;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.codingwithmitch.googledirectionstest.R;
import com.codingwithmitch.googledirectionstest.adapters.UserRecyclerAdapter;
import com.codingwithmitch.googledirectionstest.models.ClusterMarker;
import com.codingwithmitch.googledirectionstest.models.PolylineData;
import com.codingwithmitch.googledirectionstest.models.User;
import com.codingwithmitch.googledirectionstest.models.UserLocation;
import com.codingwithmitch.googledirectionstest.models.UserMarker;
import com.codingwithmitch.googledirectionstest.util.MyClusterManagerRenderer;
import com.codingwithmitch.googledirectionstest.util.ViewWeightAnimationWrapper;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.DistanceMatrixApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.algo.Algorithm;
import com.google.maps.android.clustering.algo.NonHierarchicalDistanceBasedAlgorithm;
import com.google.maps.internal.PolylineEncoding;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.DistanceMatrixElement;
import com.google.maps.model.DistanceMatrixRow;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;


import static com.codingwithmitch.googledirectionstest.Constants.MAPVIEW_BUNDLE_KEY;

public class UserListFragment extends Fragment implements
        OnMapReadyCallback,
        UserRecyclerAdapter.UserListRecyclerClickListener,
        View.OnClickListener,
        GoogleMap.OnInfoWindowClickListener,
        GoogleMap.OnPolylineClickListener
{

    private static final String TAG = "UserListFragment";

    private static final int MAP_LAYOUT_STATE_CONTRACTED = 0;
    private static final int MAP_LAYOUT_STATE_EXPANDED = 1;
    private static final int LOCATION_UPDATE_INTERVAL = 3000;

    //widgets
    private RecyclerView mUserListRecyclerView;
    private MapView mMapView;
    private RelativeLayout mMapContainer;
    private ProgressBar mProgressBar;


    //vars
    private ArrayList<User> mUserList = new ArrayList<>();
    private ArrayList<UserLocation> mUserLocations = new ArrayList<>();
    private UserRecyclerAdapter mUserRecyclerAdapter;
    private GoogleMap mGoogleMap;
    private LatLngBounds mMapBoundary;
    private int mMapLayoutState = 0;
    private com.google.maps.model.LatLng mUserPosition ;
    private GeoApiContext mGeoApiContext;
    private ArrayList<PolylineData> mPolyLinesData = new ArrayList<>();
    private ArrayList<ClusterMarker> mClusterMarkers = new ArrayList<>();
    private Marker mSelectedMarker = null;
    private ArrayList<Marker> mTripMarkers = new ArrayList<>();
    private Handler mHandler = new Handler();
    private Runnable mRunnable;
    private Boolean isUpdatingLocation = false;
    private ClusterManager<ClusterMarker> mClusterManager;
    private MyClusterManagerRenderer mClusterManagerRenderer;


    public static UserListFragment newInstance(){
        return new UserListFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "LifeCycle Event: onCreate: called. ");
        if(mUserLocations.size() == 0){ // make sure the list doesn't duplicate by navigating back
            if(getArguments() != null){
                final ArrayList<User> users = getArguments().getParcelableArrayList(getString(R.string.intent_user_list));
                mUserList.addAll(users);

                final ArrayList<UserLocation> locations = getArguments().getParcelableArrayList(getString(R.string.intent_user_locations));
                mUserLocations.addAll(locations);
            }
        }

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "LifeCycle Event: onCreateView: called. ");
        View view  = inflater.inflate(R.layout.fragment_user_list, container, false);
        mUserListRecyclerView = view.findViewById(R.id.user_list_recycler_view);
        mMapContainer = view.findViewById(R.id.map_container);
        mProgressBar = view.findViewById(R.id.progressBar);
        mMapView = view.findViewById(R.id.user_list_map);
        view.findViewById(R.id.btn_full_screen_map).setOnClickListener(this);
        view.findViewById(R.id.btn_reset_map).setOnClickListener(this);

        initGoogleMap(savedInstanceState);
        initUserListRecyclerView();
        hideSoftKeyboard();

        return view;
    }

    private void initGoogleMap(Bundle savedInstanceState){

        // *** IMPORTANT ***
        // MapView requires that the Bundle you pass contain _ONLY_ MapView SDK
        // objects or sub-Bundles.
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }
        mMapView.onCreate(mapViewBundle);
        mMapView.getMapAsync(this);

        if(mGeoApiContext == null){
            mGeoApiContext = new GeoApiContext.Builder()
                    .apiKey(getString(R.string.google_maps_api_key))
                    .build();
        }
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

    private void startUserLocationsRunnable(){
        mHandler.postDelayed(mRunnable = new Runnable() {
            @Override
            public void run() {
                // If a trip has NOT been calculated, continue updating locations
                Log.d(TAG, "run: polylinesdata size: " + mPolyLinesData.size());
                isUpdatingLocation = true;
                if(mPolyLinesData.size() == 0){
                    retrieveUserLocations();
                    mHandler.postDelayed(mRunnable, LOCATION_UPDATE_INTERVAL);
                }
            }
        }, LOCATION_UPDATE_INTERVAL);
    }

    private void retrieveUserLocations(){
        Log.d(TAG, "retrieveUserLocations: retrieving location of all users in the chatroom.");

        try{
            for(final ClusterMarker clusterMarker: mClusterMarkers){

                DocumentReference userLocationRef = FirebaseFirestore.getInstance()
                        .collection(getString(R.string.collection_user_locations))
                        .document(clusterMarker.getUser().getUser_id());

                userLocationRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(task.isSuccessful()){

                            final UserLocation updatedUserLocation = task.getResult().toObject(UserLocation.class);

                            // update the location
                            for (int i = 0; i < mClusterMarkers.size(); i++) {
                                try {
                                    if (mClusterMarkers.get(i).getUser().getUser_id().equals(updatedUserLocation.getUser().getUser_id())) {

                                        LatLng updatedLatLng = new LatLng(
                                                updatedUserLocation.getGeo_point().getLatitude(),
                                                updatedUserLocation.getGeo_point().getLongitude()
                                        );

                                        mClusterMarkers.get(i).setPosition(updatedLatLng);
                                        mClusterManagerRenderer.setUpdateMarker(mClusterMarkers.get(i));
                                    }


                                } catch (NullPointerException e) {
                                    Log.e(TAG, "retrieveUserLocations: NullPointerException: " + e.getMessage());
                                }
                            }
                        }
                    }
                });
            }
        }catch (IllegalStateException e){
            Log.e(TAG, "retrieveUserLocations: Fragment was destroyed during Firestore query. Ending query." + e.getMessage() );
        }

    }

    private void initUserListRecyclerView(){
        mUserRecyclerAdapter = new UserRecyclerAdapter(mUserList, this);
        mUserListRecyclerView.setAdapter(mUserRecyclerAdapter);
        mUserListRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    }


    private void addMapMarkers(){
        if(mGoogleMap != null){

            resetMap();

            if(mClusterManager == null){
                mClusterManager = new ClusterManager<ClusterMarker>(getActivity().getApplicationContext(), mGoogleMap);
            }
            if(mClusterManagerRenderer == null){
                mClusterManagerRenderer = new MyClusterManagerRenderer(
                        getActivity(),
                        mGoogleMap,
                        mClusterManager
                );
                mClusterManagerRenderer.setMinClusterSize(5);
                mClusterManager.setRenderer(mClusterManagerRenderer);
            }
            mGoogleMap.setOnInfoWindowClickListener(this);

            for(UserLocation userLocation: mUserLocations){

                Log.d(TAG, "addMapMarkers: location: " + userLocation.getGeo_point().toString());
                try{
                    String snippet = "";
                    if(userLocation.getUser().getUser_id().equals(FirebaseAuth.getInstance().getUid())){
                        snippet = "This is you";
                    }
                    else{
                        snippet = "Determine route to " + userLocation.getUser().getUsername() + "?";
                    }

                    int avatar = R.drawable.cwm_logo; // set the default avatar
                    try{
                        avatar = Integer.parseInt(userLocation.getUser().getAvatar());
                    }catch (NumberFormatException e){
                        Log.d(TAG, "addMapMarkers: no avatar for " + userLocation.getUser().getUsername() + ", setting default.");
                    }
                    ClusterMarker newClusterMarker = new ClusterMarker(
                            new LatLng(userLocation.getGeo_point().getLatitude(), userLocation.getGeo_point().getLongitude()),
                            userLocation.getUser().getUsername(),
                            snippet,
                            avatar,
                            userLocation.getUser()
                    );
                    mClusterManager.addItem(newClusterMarker);
                    mClusterMarkers.add(newClusterMarker);

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
            mClusterManager.setAlgorithm(new NonHierarchicalDistanceBasedAlgorithm<ClusterMarker>());
            mClusterManager.cluster();

            setCameraView();
        }
    }


    private void resetMap(){
        if(mGoogleMap != null) {
            mGoogleMap.clear();

            if(mClusterManager != null){
                mClusterManager.clearItems();
            }

            if (mClusterMarkers.size() > 0) {
                mClusterMarkers.clear();
                mClusterMarkers = new ArrayList<>();
            }

            if(mPolyLinesData.size() > 0){
                mPolyLinesData.clear();
                mPolyLinesData = new ArrayList<>();
            }
        }
    }

    /**
     * Determines the view boundary then sets the camera
     */
    private void setCameraView(){

        // Set a boundary to start
        double bottomBoundary = mUserPosition.lat - .1;
        double leftBoundary = mUserPosition.lng - .1;
        double topBoundary = mUserPosition.lat + .1;
        double rightBoundary = mUserPosition.lng + .1;

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

        for(ClusterMarker clusterMarker: mClusterMarkers){
            if(selectedUserId.equals(clusterMarker.getUser().getUser_id())){
                mGoogleMap.animateCamera(
                        CameraUpdateFactory.newLatLng(
                                new LatLng(clusterMarker.getPosition().latitude, clusterMarker.getPosition().longitude)),
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

            case R.id.btn_reset_map:{
                addMapMarkers();
                startUserLocationsRunnable();
                break;
            }
        }
    }

    @Override
    public void onInfoWindowClick(final Marker marker) {
        Log.d(TAG, "onInfoWindowClick: marker id: " + marker.getId().replace("m", ""));

        if(marker.getTitle().contains("Trip #")){
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Open Google Maps?")
                    .setCancelable(true)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                            String latitude = String.valueOf(marker.getPosition().latitude);
                            String longitude = String.valueOf(marker.getPosition().longitude);
                            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + latitude + "," + longitude);
                            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                            mapIntent.setPackage("com.google.android.apps.maps");

                            try{
                                if (mapIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                                    startActivity(mapIntent);
                                }
                            }catch (NullPointerException e){
                                Log.e(TAG, "onClick: NullPointerException: Couldn't open map." + e.getMessage() );
                                Toast.makeText(getActivity(), "Couldn't open map", Toast.LENGTH_SHORT).show();
                            }

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
        else{

            if(marker.getSnippet().equals("This is you")){
                marker.hideInfoWindow();
            }
            else{
                resetSelectedMarker();

                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(marker.getSnippet())
                        .setCancelable(true)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                                mSelectedMarker = marker;
                                calculateDirections(marker);
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
    }

    private void resetSelectedMarker(){
        if(mSelectedMarker != null){
            mSelectedMarker.setVisible(true);
            mSelectedMarker = null;
            removeTripMarkers();
        }
    }

    private void removeTripMarkers(){
        for(Marker marker: mTripMarkers){
            marker.remove();
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

    private void calculateDirections(Marker marker){
        Log.d(TAG, "calculateDirections: calculating directions.");

        com.google.maps.model.LatLng destination = new com.google.maps.model.LatLng(
                marker.getPosition().latitude,
                marker.getPosition().longitude
        );
        DirectionsApiRequest directions = new DirectionsApiRequest(mGeoApiContext);

        directions.alternatives(true);
        directions.origin(mUserPosition);
        Log.d(TAG, "calculateDirections: destination: " + destination.toString());
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
                Log.d(TAG, "run: result routes: " + result.routes.length);
                if(mPolyLinesData.size() > 0){
                    for(PolylineData polylineData: mPolyLinesData){
                        polylineData.getPolyline().remove();
                    }
                    mPolyLinesData.clear();
                    mPolyLinesData = new ArrayList<>();
                }

                double duration = 999999999;
                for(DirectionsRoute route: result.routes){
                    Log.d(TAG, "run: leg: " + route.legs[0].toString());
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

                    // highlight the fastest route and adjust camera
                    double tempDuration = route.legs[0].duration.inSeconds;
                    if(tempDuration < duration){
                        duration = tempDuration;
                        onPolylineClick(polyline);
                        zoomRoute(polyline.getPoints());
                    }

                    // hide the selected marker while polylines are visible
                    mSelectedMarker.setVisible(false);
                }
            }
        });
    }


    public void zoomRoute(List<LatLng> lstLatLngRoute) {

        if (mGoogleMap == null || lstLatLngRoute == null || lstLatLngRoute.isEmpty()) return;

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (LatLng latLngPoint : lstLatLngRoute)
            boundsBuilder.include(latLngPoint);

        int routePadding = 120;
        LatLngBounds latLngBounds = boundsBuilder.build();

        mGoogleMap.animateCamera(
                CameraUpdateFactory.newLatLngBounds(latLngBounds, routePadding),
                600,
                null
        );
    }



    @Override
    public void onPolylineClick(Polyline polyline) {

        int index = 0;
        for(PolylineData polylineData: mPolyLinesData){
            index++;
            Log.d(TAG, "onPolylineClick: toString: " + polylineData.toString());
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

                mTripMarkers.add(marker);

                marker.showInfoWindow();
            }
            else{
                polylineData.getPolyline().setColor(ContextCompat.getColor(getActivity(), R.color.darkGrey));
                polylineData.getPolyline().setZIndex(0);
            }
        }
    }

    private void stopLocationUpdates(){
        mHandler.removeCallbacks(mRunnable);
        isUpdatingLocation = false;
    }

    @Override
    public void onResume() {
        Log.d(TAG, "LifeCycle Event: onResume: called.");
        mMapView.onResume();
        super.onResume();
        if(isUpdatingLocation){
            startUserLocationsRunnable(); // update user locations every 'LOCATION_UPDATE_INTERVAL'
        }
    }

    @Override
    public void onStart() {
        Log.d(TAG, "LifeCycle Event: onStart: called.");
        mMapView.onStart();
        super.onStart();
    }

    @Override
    public void onStop() {
        Log.d(TAG, "LifeCycle Event: onStop: called.");
        mMapView.onStop();
        super.onStop();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        Log.d(TAG, "LifeCycle Event: onMapReady: called.");
        mGoogleMap = map;
        addMapMarkers();
//        mGoogleMap.setTrafficEnabled(true);
        mGoogleMap.setOnPolylineClickListener(this);
    }

    @Override
    public void onPause() {
        Log.d(TAG, "LifeCycle Event: onPause: called.");
        stopLocationUpdates(); // stop updating user locations
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "LifeCycle Event: onDestroy: called.");
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        Log.d(TAG, "LifeCycle Event: onLowMemory: called.");
        mMapView.onLowMemory();
        super.onLowMemory();
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

}



















