package com.example.vickykatara.personalblackbox;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.vickykatara.personalblackbox.types.AbsoluteEmergency;
import com.example.vickykatara.personalblackbox.types.DangerousSituation;
import com.example.vickykatara.personalblackbox.types.Distraction;

import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    public static final double PRESSURE_CHANGE_THRESHOLD = 0.000015;
    private static final boolean DEBUG_MODE_ON = true;

    private boolean pressureNotCapturedYet = false;

    private boolean mockPressure = false;
    private boolean mockSound = false;
    private boolean mockCall = false;
    private boolean mockDriving = false;
    private boolean mockOrientation = false;
    private boolean mockKeyboardDrawn = false;

    private double lastCapturedPressure;
    private double lastCapturedSound;
    private Location lastCapturedLocation;

    private Set<AbsoluteEmergency> absoluteEmergencyList;
    private Set<DangerousSituation> dangerousSituationsList;
    private Set<Distraction> distractionsList;

    private long recordBeginTimeStamp;

    private SensorManager mSensorManager;
    private Sensor mPressure;

    private boolean noPressureSensor = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.absoluteEmergencyList = new HashSet<>();
        this.dangerousSituationsList = new HashSet<>();
        this.distractionsList = new HashSet<>();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mPressure = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        if(mPressure == null)
            noPressureSensor = true;

        ((Switch) findViewById(R.id.pressureSwitch)).setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            mockPressure = true;
                            ((TextView) findViewById(R.id.pressureTextView)).setText(R.string.yes_mocked);
                            checkEmergencySituations();
                        } else {
                            mockPressure = false;
                        }
                    }
                }
        );

        ((Switch) findViewById(R.id.soundSwitch)).setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            mockSound = true;
                            ((TextView) findViewById(R.id.soundTextView)).setText(R.string.yes_mocked);
                            checkEmergencySituations();
                        } else {
                            mockSound = false;
                        }
                    }
                }
        );

        ((Switch) findViewById(R.id.ongoingCallSwitch)).setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            mockCall = true;
                            ((TextView) findViewById(R.id.onGoingCallTextView)).setText(R.string.yes_mocked);
                            checkEmergencySituations();
                        } else {
                            mockCall = false;
                        }
                    }
                }
        );

        ((Switch) findViewById(R.id.drivingSwitch)).setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            mockDriving = true;
                            ((TextView) findViewById(R.id.drivingTextView)).setText(R.string.yes_mocked);
                            checkEmergencySituations();
                        } else {
                            mockDriving = false;
                        }
                    }
                }
        );

        ((Switch) findViewById(R.id.orientationSwitch)).setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            mockOrientation = true;
                            ((TextView) findViewById(R.id.orientationTextView)).setText(R.string.yes_mocked);
                            checkEmergencySituations();
                        } else {
                            mockOrientation = false;
                        }
                    }
                }
        );

        ((Switch) findViewById(R.id.keyboardSwitch)).setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            mockKeyboardDrawn = true;
                            ((TextView) findViewById(R.id.orientationTextView)).setText(R.string.yes_mocked);
                            checkEmergencySituations();
                        } else {
                            mockKeyboardDrawn = false;
                        }
                    }
                }
        );

    }

    void checkEmergencySituations() {
        if(DEBUG_MODE_ON) makeAlertDialog("Checking Emergency");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        if (sensor.getType() == Sensor.TYPE_PRESSURE) {
            checkPressureChange(event.values[0]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void checkPressureChange(float newPressureValue) {
        if(percentChange(lastCapturedPressure, newPressureValue) > PRESSURE_CHANGE_THRESHOLD && pressureNotCapturedYet) {
            absoluteEmergencyList.add(AbsoluteEmergency.PRESSURE_CHANGE);
            checkEmergencySituations();
        } else {
            absoluteEmergencyList.remove(AbsoluteEmergency.PRESSURE_CHANGE);
            pressureNotCapturedYet = true;
        }
        lastCapturedPressure = newPressureValue;
        updatePressureStrings();
    }

    private double percentChange(double lastCapturedPressure, double newPressureValue) {
        if(DEBUG_MODE_ON) makeAlertDialog("Old Hg:"+lastCapturedPressure+", new Hg: "+newPressureValue);
        return Math.abs(lastCapturedPressure-newPressureValue)/lastCapturedPressure;
    }

    private void updatePressureStrings() {
        if(noPressureSensor) {
            ((TextView) findViewById(R.id.pressureTextView)).setText("No Pressure Sensor");
            return;
        }
        if (mockPressure == false) {
            ((TextView) findViewById(R.id.pressureTextView)).setText(String.format("%.5f", 0.0295301d*lastCapturedPressure) + " mmHg");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mPressure, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        // Be sure to unregister the sensor when the activity pauses.
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    public void makeAlertDialog(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
