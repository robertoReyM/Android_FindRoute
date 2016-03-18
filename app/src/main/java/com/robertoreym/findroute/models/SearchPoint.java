package com.robertoreym.findroute.models;

import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;

/**
 * Created by robertoreym on 17/03/16.
 */
public class SearchPoint {
    private LatLng position;
    private HashMap<String,Route> availableRoutes;
    private HashMap<String,Stop> closestStops;

    public LatLng getPosition() {
        return position;
    }

    public void setPosition(LatLng position) {
        this.position = position;
    }

    public HashMap<String, Route> getAvailableRoutes() {
        return availableRoutes;
    }

    public void setAvailableRoutes(HashMap<String, Route> availableRoutes) {
        this.availableRoutes = availableRoutes;
    }

    public HashMap<String, Stop> getClosestStops() {
        return closestStops;
    }

    public void setClosestStops(HashMap<String, Stop> closestStops) {
        this.closestStops = closestStops;
    }
}
