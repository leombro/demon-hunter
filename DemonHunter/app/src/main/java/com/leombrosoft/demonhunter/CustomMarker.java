package com.leombrosoft.demonhunter;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by neku on 20/10/15.
 */
public class CustomMarker {
    private int dbKey = -1;
    private double longitude;
    private double latitude;
    private double distance;
    private int demonRef;

    public CustomMarker(LatLng location, double distance, int demonID) {
        latitude = location.latitude;
        longitude = location.longitude;
        this.distance = distance;
        demonRef = demonID;
    }

    public CustomMarker(int dbk, double lat, double lng, double distance, int demonID) {
        dbKey = dbk;
        latitude = lat;
        longitude = lng;
        this.distance = distance;
        demonRef = demonID;
    }

    public double getDistance() {
        return distance;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public int getDemonID() {
        return demonRef;
    }

    public int getDbKey() {
        return dbKey;
    }

    public LatLng getLatLng() {
        return new LatLng(latitude, longitude);
    }
}
