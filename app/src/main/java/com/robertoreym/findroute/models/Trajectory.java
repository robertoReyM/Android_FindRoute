package com.robertoreym.findroute.models;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

/**
 * Created by robertoreym on 16/03/16.
 */
public class Trajectory {

    private float distance;
    private List<LatLng> points;

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public List<LatLng> getPoints() {
        return points;
    }

    public void setPoints(List<LatLng> points) {
        this.points = points;
    }
}
