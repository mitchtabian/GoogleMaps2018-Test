package com.codingwithmitch.googledirectionstest;

public class Topics {

    // Making sure the device can use google maps
    // Enabling location information
    // Enabling Google Maps and other API's from Google Cloud Console
    // Getting started with a Google map (Using the API key and inflating a MapView
        //  (chose to use MapView instead of MapFragment https://developers.google.com/maps/documentation/android-sdk/map#the_map_object)
    // Get last known location and upload to Firebase
    // Continue retrieving last known location every 5 seconds using a service
    // Upload that location to Firebase
    // Retrieve location of everyone in a chatroom from Firebase every 5 seconds
    // Add markers for every person in chatroom
    // animating the camera movement
    // Setting view bounds
    // ClickListeners for markers
    // Retrieving distance and travel duration
    // Creating info windows when markers are clicked
    // Displaying distance and travel duration in an info window
    // Calculating different possible routes
    // Add polylines for routes
    // Hiding markers
    // removing markers
    // Initiating a trip in Google Maps app when a trip is selected
    // Enabling traffic information (Unfortunately there's not much we can do here)
    // Custom icons for the map (Creating a clusterItem and all that stuff)
            // https://developers.google.com/maps/documentation/android-sdk/utility/marker-clustering
            // https://stackoverflow.com/questions/32158927/android-google-map-icongenerator-making-a-transparent-marker-icon
    // Stopping the Location service when user has signed out (checking for null pointer in "saveUserLocation" method in service



    // MIGHT DO THIS:
    // 1) show how to get location simply using "getLastKnownLocation"
    // 2) show how to get location updates using a LocationRequest
    // 3) show how to get location updates using a service that automatically updates the database if app in background or foreground
    //  (This also uses the LocationRequest) If greater than API 26 we can use a background service that lasts until app is closed.


    // Notes:
    // 1) After polylines have been added, the positions will no longer change on the map
    //      This can be reset by pressing the reset button in the top left
}
