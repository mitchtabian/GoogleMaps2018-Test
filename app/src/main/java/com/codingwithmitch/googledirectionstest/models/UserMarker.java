package com.codingwithmitch.googledirectionstest.models;

import com.google.android.gms.maps.model.Marker;

public class UserMarker {

    private Marker marker;
    private User user;


    public UserMarker(Marker marker, User user) {
        this.marker = marker;
        this.user = user;
    }

    public UserMarker() {
    }


    public Marker getMarker() {
        return marker;
    }

    public void setMarker(Marker marker) {
        this.marker = marker;
    }

    public User getUser() {
        return user;
    }

    @Override
    public String toString() {
        return "UserMarker{" +
                "marker=" + marker +
                ", user=" + user +
                '}';
    }
}
