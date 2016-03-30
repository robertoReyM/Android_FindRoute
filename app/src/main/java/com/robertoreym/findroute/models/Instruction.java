package com.robertoreym.findroute.models;

/**
 * Created by robertoreym on 30/03/16.
 */
public class Instruction {

    private int type;
    private Stop destination;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Stop getDestination() {
        return destination;
    }

    public void setDestination(Stop destination) {
        this.destination = destination;
    }
}
