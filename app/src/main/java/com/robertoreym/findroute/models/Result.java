package com.robertoreym.findroute.models;

import java.util.ArrayList;

/**
 * Created by robertoreym on 16/03/16.
 */
public class Result {

    private String routes;
    private ArrayList<Trajectory> trajectories;

    public String getRoutes() {
        return routes;
    }

    public void setRoutes(String routes) {
        this.routes = routes;
    }

    public float getDistance() {

        float totalDistance = 0;

        for(Trajectory trajectory: trajectories){
            totalDistance+=trajectory.getDistance();
        }
        return totalDistance;
    }


    public ArrayList<Trajectory> getTrajectories() {
        return trajectories;
    }

    public void setTrajectories(ArrayList<Trajectory> trajectories) {
        this.trajectories = trajectories;
    }

}
