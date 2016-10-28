package com.example.vickykatara.personalblackbox.types;

/**
 * Created by Vicky Katara on 28-Oct-16.
 */
public enum DangerousSituation {
    DRIVING("driving"), RUNNING("running");

    private String label;

    DangerousSituation(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return this.label;
    }
}
