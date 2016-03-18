package com.robertoreym.findroute.models;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by robertoreym on 16/03/16.
 */
public class Route {
    private String id;
    private String polyline;
    private ArrayList<Stop> stops;
    private List<LatLng> points;
    private Stop closestStop;
    private HashMap<String,RouteIntersection> intersectedRoutes;

    public HashMap<String, RouteIntersection> getIntersectedRoutes() {
        return intersectedRoutes;
    }

    public void setIntersectedRoutes(HashMap<String, RouteIntersection> intersectedRoutes) {
        this.intersectedRoutes = intersectedRoutes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPolyline() {
        return polyline;
    }

    public void setPolyline(String polyline) {
        this.polyline = polyline;
    }

    public ArrayList<Stop> getStops() {
        return stops;
    }

    public void setStops(ArrayList<Stop> stops) {
        this.stops = stops;
    }

    public List<LatLng> getPoints() {
        return points;
    }

    public void setPoints(List<LatLng> points) {
        this.points = points;
    }

    public Stop getClosestStop() {
        return closestStop;
    }

    public void setClosestStop(Stop closestStop) {
        this.closestStop = closestStop;
    }
}
