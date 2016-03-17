package com.robertoreym.findroute.models;

import java.util.ArrayList;

/**
 * Created by robertoreym on 16/03/16.
 */
public class Result {

    private float distance;
    private ArrayList<Trajectory> trajectories;

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public ArrayList<Trajectory> getTrajectories() {
        return trajectories;
    }

    public void setTrajectories(ArrayList<Trajectory> trajectories) {
        this.trajectories = trajectories;
    }
}
