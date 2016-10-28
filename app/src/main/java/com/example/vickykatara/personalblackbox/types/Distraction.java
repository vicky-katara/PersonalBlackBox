package com.example.vickykatara.personalblackbox.types;

/**
 * Created by Vicky Katara on 28-Oct-16.
 */
public enum Distraction {
    ONGOING_CALL("ongoing_call"), TEXTING("texting");

    private String label;

    Distraction(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return this.label;
    }
}
