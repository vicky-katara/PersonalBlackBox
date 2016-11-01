package com.example.vickykatara.personalblackbox.types;

import android.location.Location;
import android.os.Build;
import android.support.annotation.RequiresApi;

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return dest.distanceTo(src)/((dest.getElapsedRealtimeNanos() - src.getElapsedRealtimeNanos())*1000000.0);
        } else {
            return dest.distanceTo(src)/((dest.getTime()-src.getTime())*1000.0);
        }
    }

    public double getDistance() {
        return dest.distanceTo(src);
    }
}