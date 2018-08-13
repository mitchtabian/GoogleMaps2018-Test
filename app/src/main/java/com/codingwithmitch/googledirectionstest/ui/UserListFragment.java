package com.codingwithmitch.googledirectionstest.ui;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import com.codingwithmitch.googledirectionstest.R;
import com.codingwithmitch.googledirectionstest.adapters.UserRecyclerAdapter;
import com.codingwithmitch.googledirectionstest.models.User;
import com.codingwithmitch.googledirectionstest.models.UserLocation;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;

import static com.codingwithmitch.googledirectionstest.Constants.MAPVIEW_BUNDLE_KEY;

public class UserListFragment extends Fragment implements
        OnMapReadyCallback,
        UserRecyclerAdapter.UserListRecyclerClickListener
{

    private static final String TAG = "UserListFragment";

    //widgets
    private RecyclerView mUserListRecyclerView;
    private MapView mMapView;


    //vars
    private ArrayList<User> mUserList = new ArrayList<>();
    private ArrayList<UserLocation> mUserLocations = new ArrayList<>();
    private UserRecyclerAdapter mUserRecyclerAdapter;
    private FirebaseFirestore mDb;
    private GoogleMap mGoogleMap;
    private LatLngBounds mMapBoundary;


    public static UserListFragment newInstance(){
        return new UserListFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null){
            mUserList = getArguments().getParcelableArrayList(getString(R.string.intent_user_list));
            mUserLocations = getArguments().getParcelableArrayList(getString(R.string.intent_user_locations));
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view  = inflater.inflate(R.layout.fragment_user_list, container, false);
        mUserListRecyclerView = view.findViewById(R.id.user_list_recycler_view);

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

    private void initUserListRecyclerView(){
        mUserRecyclerAdapter = new UserRecyclerAdapter(mUserList, this);
        mUserListRecyclerView.setAdapter(mUserRecyclerAdapter);
        mUserListRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    }



    private void addMapMarkers(){
        if(mGoogleMap != null){
            for(UserLocation userLocation: mUserLocations){
                mGoogleMap.addMarker(
                        new MarkerOptions().position(
                                new LatLng(userLocation.getGeo_point().getLatitude(), userLocation.getGeo_point().getLongitude())
                        ).title(userLocation.getUser().getUsername()));
            }
            setCameraView();
        }
    }

    /**
     * Determines the view boundary then sets the camera
     */
    private void setCameraView(){

        // Set a random boundary to start
        double bottomBoundary = mUserLocations.get(0).getGeo_point().getLatitude() - 0.1;
        double leftBoundary = mUserLocations.get(0).getGeo_point().getLongitude() - 0.1;
        double topBoundary = mUserLocations.get(0).getGeo_point().getLatitude() + 0.1;
        double rightBoundary = mUserLocations.get(0).getGeo_point().getLongitude() + 0.1;

        for(UserLocation userLocation: mUserLocations){

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
    }

    @Override
    public void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mMapView.onDestroy();
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


}



















