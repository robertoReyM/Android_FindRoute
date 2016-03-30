package com.robertoreym.findroute.models;

/**
 * Created by robertoreym on 30/03/16.
 */
public class Instruction {

    private int type;
    private String action;
    private String details;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
