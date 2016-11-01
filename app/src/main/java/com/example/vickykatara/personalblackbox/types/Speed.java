package com.example.vickykatara.personalblackbox.types;

import android.location.Location;

/**
 * Created by Vicky Katara on 31-Oct-16.
 */

public class Speed {

    public static final double MINIMUM_DRIVING_SPEED = 4.4704; // = 10 mph
    public static final double MINIMUM_WALKING_SPEED = 1.3858; // = 3 mph

    private Location src, dest;

    public Speed(Location src, Location dest) {
        this.src = src;
        this.dest = dest;
    }

    public static Speed fromOld(Speed other, Location dest) {
        return new Speed(other.dest, dest);
    }

    public double getSpeed() {
        return dest.distanceTo(src)/((dest.getTime()-src.getTime())*1.0);
    }

    public double getDistance() {
        return dest.distanceTo(src);
    }
}