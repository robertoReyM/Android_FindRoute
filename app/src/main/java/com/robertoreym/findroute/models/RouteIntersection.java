package com.robertoreym.findroute.models;

import java.util.ArrayList;

/**
 * Created by robertoreym on 17/03/16.
 */
public class RouteIntersection {

    private String routeID;
    private ArrayList<PointIntersection> pointIntersections;

    public String getRouteID() {
        return routeID;
    }

    public void setRouteID(String routeID) {
        this.routeID = routeID;
    }

    public ArrayList<PointIntersection> getPointIntersections() {
        return pointIntersections;
    }

    public void setPointIntersections(ArrayList<PointIntersection> pointIntersections) {
        this.pointIntersections = pointIntersections;
    }
}
