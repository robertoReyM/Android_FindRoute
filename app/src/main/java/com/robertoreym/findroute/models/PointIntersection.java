package com.robertoreym.findroute.models;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by robertoreym on 16/03/16.
 */
public class PointIntersection {

    private String r1ID;
    private Stop r1Stop;
    private String r2ID;
    private Stop r2Stop;

    public String getR1ID() {
        return r1ID;
    }

    public void setR1ID(String r1ID) {
        this.r1ID = r1ID;
    }

    public Stop getR1Stop() {
        return r1Stop;
    }

    public void setR1Stop(Stop r1Stop) {
        this.r1Stop = r1Stop;
    }

    public String getR2ID() {
        return r2ID;
    }

    public void setR2ID(String r2ID) {
        this.r2ID = r2ID;
    }

    public Stop getR2Stop() {
        return r2Stop;
    }

    public void setR2Stop(Stop r2Stop) {
        this.r2Stop = r2Stop;
    }
}
