package com.example.vickykatara.personalblackbox.types;

/**
 * Created by Vicky Katara on 28-Oct-16.
 */
public enum AbsoluteEmergency {
    PRESSURE_CHANGE("pressure_change"), LARGE_NOISE("large_noise");

    private String label;

    AbsoluteEmergency(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return this.label;
    }
}
