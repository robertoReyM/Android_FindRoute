package com.robertoreym.findroute.models;

import java.util.ArrayList;

/**
 * Created by robertoreym on 16/03/16.
 */
public class Result {

    private ArrayList<Trajectory> trajectories;

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
