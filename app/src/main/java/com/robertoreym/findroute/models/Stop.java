package com.robertoreym.findroute.models;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by robertoreym on 16/03/16.
 */
public class Stop {
    private String name;
    private LatLng position;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LatLng getPosition() {
        return position;
    }

    public void setPosition(LatLng position) {
        this.position = position;
    }
}
