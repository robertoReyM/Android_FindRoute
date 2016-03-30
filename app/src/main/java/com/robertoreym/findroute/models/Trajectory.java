package com.robertoreym.findroute.models;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

/**
 * Created by robertoreym on 16/03/16.
 */
public class Trajectory {

    private float distance;
    private List<LatLng> points;
    private Stop source;
    private Stop destination;
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Stop getSource() {
        return source;
    }

    public void setSource(Stop source) {
        this.source = source;
    }

    public Stop getDestination() {
        return destination;
    }

    public void setDestination(Stop destination) {
        this.destination = destination;
    }

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
