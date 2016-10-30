package com.example.vickykatara.personalblackbox;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.BundleCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.vickykatara.personalblackbox.types.AbsoluteEmergency;
import com.example.vickykatara.personalblackbox.types.DangerousSituation;
import com.example.vickykatara.personalblackbox.types.Distraction;
import com.example.vickykatara.personalblackbox.utils.PhoneStateBroadcastReceiver;
import com.example.vickykatara.personalblackbox.utils.SoundMeter;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity
        implements SensorEventListener {

    public static final double PRESSURE_CHANGE_THRESHOLD = 0.0008;
    public static final boolean DEBUG_MODE_ON = true;
    private static final double SOUND_AMPLITUDE_THRESHOLD = 10000; // Range 0:32768

    private boolean pressureNotCapturedYet = false;
    private boolean soundNotCapturedYet = false;

    private boolean mockPressure = false;
    private boolean mockSound = false;
    private boolean mockCall = false;
    private boolean mockDriving = false;
    private boolean mockOrientation = false;
    private boolean mockKeyboardDrawn = false;

    private double lastCapturedPressure, newPressure;
    private int lastCapturedSound;
    private Location lastCapturedLocation;
    public boolean isOnCall;

    private Set<AbsoluteEmergency> absoluteEmergencyList;
    private Set<DangerousSituation> dangerousSituationsList;
    public Set<Distraction> distractionsList;

    private long recordBeginTimeStamp;

    private SensorManager mSensorManager;
    private Sensor mPressure;

    private boolean noPressureSensor = false;

    private Thread soundGenerator;
    private Handler soundHandler;

    private BroadcastReceiver broadcastReceiver;
//    private PhoneStateListener phoneStateListener;

    private TelephonyManager telephonyManager;
    private IntentFilter callIntentFilter;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.absoluteEmergencyList = new HashSet<>();
        this.dangerousSituationsList = new HashSet<>();
        this.distractionsList = new HashSet<>();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mPressure = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        if (mPressure == null)
            noPressureSensor = true;

        soundGenerator = new Thread(new SoundCaptureThread());
        soundGenerator.start();

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
                            updatePressureStrings(true);
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
                            updateSoundStrings(true);
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
                            updateOnCallStrings();
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


        createSoundHandler();

        createCellBroadcastReceiver();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        printAllStrings();
    }

    private void printAllStrings() {
        this.updatePressureStrings(false);
        this.updateSoundStrings(false);
        this.updateOnCallStrings();
    }

    private void createSoundHandler() {
        checkPermission();
        soundHandler = new Handler() {
            public void handleMessage(Message msg) {
                double amplitude = msg.getData().getDouble("amplitude");
                if (amplitude > 0)
                    checkSoundEmergency(amplitude);
            }
        };
    }

    public void checkEmergencySituations() {
//        if(DEBUG_MODE_ON) makeAlertDialog("Checking Emergency");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        if (sensor.getType() == Sensor.TYPE_PRESSURE && mockPressure == false) {
//            System.out.println(")))))))))))))))))))))))))) MockPressure:"+mockPressure);
            checkPressureChange(event.values[0]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void checkPressureChange(float newPressureValue) {
        newPressure = newPressureValue;
        if (percentChange(lastCapturedPressure, newPressureValue) > PRESSURE_CHANGE_THRESHOLD && pressureNotCapturedYet) {
            if (DEBUG_MODE_ON) makeAlertDialog("Sudden Pressure Change Detected");
            absoluteEmergencyList.add(AbsoluteEmergency.PRESSURE_CHANGE);
            checkEmergencySituations();
            updatePressureStrings(true);
        } else {
            absoluteEmergencyList.remove(AbsoluteEmergency.PRESSURE_CHANGE);
            pressureNotCapturedYet = true;
            updatePressureStrings(false);
        }
        lastCapturedPressure = newPressureValue;
    }

    private double percentChange(double lastCapturedPressure, double newPressureValue) {
        return Math.abs(lastCapturedPressure - newPressureValue) / lastCapturedPressure;
    }

    private void updatePressureStrings(boolean isDanger) {
        if (noPressureSensor) {
            ((TextView) findViewById(R.id.pressureTextView)).setText("No Pressure Sensor");
            return;
        }
        if (mockPressure == false) {
            ((TextView) findViewById(R.id.pressureTextView)).setText(String.format("%.3f", newPressure) + " hPa (millibar)" + (isDanger ? " !!! " : ""));
        }
    }

    private void checkSoundEmergency(double amplitude) {
        if (amplitude > SOUND_AMPLITUDE_THRESHOLD && soundNotCapturedYet) {
            absoluteEmergencyList.add(AbsoluteEmergency.LARGE_NOISE);
            updateSoundStrings(true);
            checkEmergencySituations();
        } else {
            absoluteEmergencyList.remove(AbsoluteEmergency.LARGE_NOISE);
            soundNotCapturedYet = true;
            updateSoundStrings(false);
        }
        lastCapturedSound = (int)Math.ceil(amplitude);
    }

    private void updateSoundStrings(boolean isDanger) {
        if (mockSound == false) {
            ((TextView) findViewById(R.id.soundTextView)).setText(String.format("%.2f", ((lastCapturedSound*140.0)/32768.0)) + " dB " + (isDanger ? " !!! " : ""));
        }
    }

    private void createCellBroadcastReceiver() {

        broadcastReceiver = new PhoneStateBroadcastReceiver(this);

        callIntentFilter = new IntentFilter();
        callIntentFilter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
        callIntentFilter.addAction(Intent.ACTION_CALL);
        callIntentFilter.addAction(Intent.EXTRA_PHONE_NUMBER);

        registerReceiver(broadcastReceiver, callIntentFilter);

        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        telephonyManager.listen(((PhoneStateBroadcastReceiver)broadcastReceiver).customPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

//    private void createCellBroadcastReceiver2() {
//
//        phoneStateListener = new PhoneStateListener() {
//            @Override
//            public void onCallStateChanged(int state, String incomingNumber) {
//                switch (state) {
//                    case TelephonyManager.CALL_STATE_RINGING:
//                        // called when someone is ringing to this phone
//                        makeAlertDialog("Incoming: "+incomingNumber);
//                        break;
//                }
//            }
//        };
//
//        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
//        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
//
//        broadcastReceiver = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                String action = intent.getAction();
//                System.out.println(" >>>>>>>>>>>>>>>>>>>>>>>>>>>>> onReceive: "+action);
//                if (action.equalsIgnoreCase(Intent.ACTION_NEW_OUTGOING_CALL) || action.equalsIgnoreCase(Intent.ACTION_CALL)) {
//                    isOnCall = true;
//                    distractionsList.add(Distraction.ONGOING_CALL);
//                    updateOnCallStrings();
//                    checkEmergencySituations();
//                } else {
//                    isOnCall = false;
//                    checkEmergencySituations();
//                    distractionsList.remove(Distraction.ONGOING_CALL);
//                }
//            }
//        };
//
//        callIntentFilter = new IntentFilter();
//        callIntentFilter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
//        callIntentFilter.addAction(Intent.ACTION_CALL);
//
//        registerReceiver(broadcastReceiver, callIntentFilter);
//        //        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
//    }

    public void updateOnCallStrings() {
        if (mockCall == false) {
            ((TextView) findViewById(R.id.onGoingCallTextView)).setText((isOnCall ? "On Call !!! " : "Not on a Call"));
        }
    }

    public void makeAlertDialog(String message) {
        if (message.toUpperCase().contains("EMER"))
            System.err.println("******             dialog" + message);
        else
            System.out.println("******             dialog" + message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        registerReceiver(broadcastReceiver, callIntentFilter);

//        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        soundGenerator = new Thread(new SoundCaptureThread());
        soundGenerator.start();
//        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
//        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

//        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        soundGenerator.interrupt();

//        unregisterReceiver(broadcastReceiver);

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mPressure, SensorManager.SENSOR_DELAY_NORMAL);
        soundGenerator = new Thread(new SoundCaptureThread());
        soundGenerator.start();
//        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
//        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        registerReceiver(broadcastReceiver, callIntentFilter);
    }

    @Override
    protected void onPause() {
        // Be sure to unregister the sensor when the activity pauses.
        super.onPause();
        mSensorManager.unregisterListener(this);
//        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        unregisterReceiver(broadcastReceiver);
    }

    private class SoundCaptureThread implements Runnable {
        SoundMeter meter = new SoundMeter();

        @Override
        public void run() {
            while (true) {
                if (mockSound) {
                    try {
                        Thread.sleep(5000);
                        continue;
                    } catch (InterruptedException e) {
                    }
                }
                try {
                    meter.start();
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                }

                double amplitude = meter.getAmplitude();
                try {
                    meter.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
                Bundle bundle = new Bundle();
                bundle.putDouble("amplitude", amplitude);

                Message message = new Message();
                message.setData(bundle);

                soundHandler.sendMessage(message);

                try {
                    Thread.sleep(200); // change to 60000
                } catch (InterruptedException e) {
                }
            }
        }
    }

    private void checkPermission() {
        int storagePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int audioPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        int locationPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int phonePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE);

        // If we don't have permissions, ask user for permissions
        if (storagePermission != PackageManager.PERMISSION_GRANTED) {
            String[] PERMISSIONS_STORAGE = {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            int REQUEST_EXTERNAL_STORAGE = 1;

            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }

        if (audioPermission != PackageManager.PERMISSION_GRANTED) {
            String[] PERMISSIONS_STORAGE = {
                    Manifest.permission.RECORD_AUDIO,
            };
            int REQUEST_RECORD_AUDIO = 1;

            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    REQUEST_RECORD_AUDIO
            );
        }

        if (locationPermission != PackageManager.PERMISSION_GRANTED) {
            String[] PERMISSIONS_STORAGE = {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.LOCATION_HARDWARE,
                    Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS
            };
            int REQUEST_LOCATION = 1;

            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    REQUEST_LOCATION
            );
        }

        if (phonePermission != PackageManager.PERMISSION_GRANTED) {
            String[] PERMISSIONS_STORAGE = {
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.PROCESS_OUTGOING_CALLS
            };
            int REQUEST_PHONE_UPDATES = 1;

            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    REQUEST_PHONE_UPDATES
            );
        }
    }
}
