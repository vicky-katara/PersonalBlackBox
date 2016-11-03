package com.example.vickykatara.personalblackbox;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
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
import com.example.vickykatara.personalblackbox.types.Speed;
import com.example.vickykatara.personalblackbox.utils.SoundMeter;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.DriveScopes;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity
        implements SensorEventListener,
                GoogleApiClient.ConnectionCallbacks,
                GoogleApiClient.OnConnectionFailedListener,
                LocationListener {

    private static final int REQUEST_CODE_RESOLUTION = 1;
    private static final  int REQUEST_CODE_OPENER = 2;

    public static final double PRESSURE_CHANGE_THRESHOLD = 0.0008;
    public static final boolean DEBUG_MODE_ON = false;
    private static final double SOUND_AMPLITUDE_THRESHOLD = 10000; // Range 0:32768

    private boolean pressureNotCapturedYet = true;
    private boolean soundNotCapturedYet = true;
    private boolean speedNotCapturedYet = true;

    private boolean mockPressure = false;
    private boolean mockSound = false;
    private boolean mockCall = false;
    private boolean mockDriving = false;
    private boolean mockWalking = false;
    private boolean mockKeyboardDrawn = false;

    double lastCapturedPressure, newPressure;
    int lastCapturedSound;
    Location firstLocation, lastCapturedLocation;
    Speed lastCapturedSpeed;
    public boolean isOnCall;
    boolean isKeyboardDrawn;

    Set<AbsoluteEmergency> absoluteEmergencySet;
    Set<DangerousSituation> dangerousSituationsSet;
    public Set<Distraction> distractionSet;

    private SensorManager mSensorManager;
    private Sensor mPressure, mLight, mProximity;

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

    private LocationRequest locationRequest;
    private Handler locationHandler;
    float lastCapturedLightIntensity;
    float lastCapturedProximityValue;
    private Handler blackBoxDataHandler;
    String numberOnCallWith;
    private PhoneStateListener phoneStateListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.absoluteEmergencySet = new HashSet<>();
        this.dangerousSituationsSet = new HashSet<>();
        this.distractionSet = new HashSet<>();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mPressure = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

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
                            absoluteEmergencySet.add(AbsoluteEmergency.PRESSURE_CHANGE);
                            checkEmergencySituations();
                        } else {
                            mockPressure = false;
                            absoluteEmergencySet.remove(AbsoluteEmergency.PRESSURE_CHANGE);
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
                            absoluteEmergencySet.add(AbsoluteEmergency.LARGE_NOISE);
                            checkEmergencySituations();
                        } else {
                            mockSound = false;
                            absoluteEmergencySet.remove(AbsoluteEmergency.LARGE_NOISE);
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
                            distractionSet.add(Distraction.ONGOING_CALL);
                            checkEmergencySituations();
                        } else {
                            mockCall = false;
                            distractionSet.remove(Distraction.ONGOING_CALL);
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
                            mockDriving();
                            unmockWalking();
                        } else {
                            unmockDriving();
                        }
                    }
                }
        );

        ((Switch) findViewById(R.id.walkingSwitch)).setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            mockWalking();
                            unmockDriving();
                        } else {
                            unmockWalking();
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
                            ((TextView) findViewById(R.id.keyboardTextView)).setText(R.string.yes_mocked);
                            distractionSet.add(Distraction.TEXTING);
                            checkEmergencySituations();
                        } else {
                            mockKeyboardDrawn = false;
                            distractionSet.remove(Distraction.TEXTING);
                        }
                        updateKeyboardStrings();
                    }
                }
        );


        createSoundHandler();
        createBlackBoxDataHandler();

        createCallListener();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
//        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();


        client = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(AppIndex.API)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .build();

        authorizeGoogleDrive();

        createLocationListener();

        createKeyboardListener();

        printAllStrings();
    }

    private void createKeyboardListener() {
        KeyboardVisibilityEvent.setEventListener(
                this,
                new KeyboardVisibilityEventListener() {
                    @Override
                    public void onVisibilityChanged(boolean isOpen) {
                        isKeyboardDrawn = isOpen;
                        if (DEBUG_MODE_ON) makeAlertDialog("The keyboard has been "+(isOpen ? "":"un")+"drawn");
                        checkKeyboardChange();
                    }
                });
    }

    private void checkKeyboardChange() {
        if (isKeyboardDrawn) {
            distractionSet.add(Distraction.TEXTING);
            checkEmergencySituations();
        } else {
            distractionSet.remove(Distraction.TEXTING);
            checkEmergencySituations();
        }
        updateKeyboardStrings();
    }

    private void updateKeyboardStrings() {
        if (mockKeyboardDrawn == false) {
            ((TextView) findViewById(R.id.keyboardTextView)).setText(isKeyboardDrawn ? "Yes" : "No");
        }
    }

    private void mockWalking() {
        mockWalking = true;
        ((TextView) findViewById(R.id.walkingTextView)).setText(R.string.yes_mocked);
        dangerousSituationsSet.add(DangerousSituation.WALKING);
        checkEmergencySituations();
    }

    private void mockDriving() {
        mockDriving = true;
        ((TextView) findViewById(R.id.drivingTextView)).setText(R.string.yes_mocked);
        dangerousSituationsSet.add(DangerousSituation.DRIVING);
        checkEmergencySituations();
    }

    private void unmockWalking() {
        mockWalking = false;
        dangerousSituationsSet.remove(DangerousSituation.WALKING);
        ((Switch) findViewById(R.id.walkingSwitch)).setChecked(false);
        updateDrivingWalkingStrings();
    }

    private void unmockDriving() {
        mockDriving = false;
        dangerousSituationsSet.remove(DangerousSituation.DRIVING);
        ((Switch) findViewById(R.id.drivingSwitch)).setChecked(false);
        updateDrivingWalkingStrings();
    }

    private void printAllStrings() {
        this.updatePressureStrings(false);
        this.updateSoundStrings(false);
        this.updateOnCallStrings();
        this.updateDrivingWalkingStrings();
        this.updateKeyboardStrings();
    }

    private void authorizeGoogleDrive() {
        List<String> scopeList = new ArrayList<>(1);
        scopeList.add(DriveScopes.DRIVE);
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(this,  scopeList);
        String accountName = credential.getSelectedAccountName();
        credential.setSelectedAccountName(accountName);
        com.google.api.services.drive.Drive service = new com.google.api.services.drive.Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential).build();
    }

    private String driveFileName;
    private String driveFileContents;

    public void uploadToDrive(String fileName, StringBuilder contents) {
        makeAlertDialog("Uploading "+fileName+" to drive");
        this.driveFileName = fileName;
        this.driveFileContents = contents.toString();
        // create new contents resource
        Drive.DriveApi.newDriveContents(client)
                .setResultCallback(driveContentsCallback);
    }

    private boolean fileOperation = true;

    final ResultCallback<DriveApi.DriveContentsResult> driveContentsCallback =
            new ResultCallback<DriveApi.DriveContentsResult>() {
                @Override
                public void onResult(DriveApi.DriveContentsResult result) {
                    if (result.getStatus().isSuccess()) {
                        if (fileOperation == true) {
                            if(DEBUG_MODE_ON) makeAlertDialog(" Creating new File ");
                            CreateFileOnGoogleDrive(result);
                        } else {
                            if(DEBUG_MODE_ON) makeAlertDialog(" Uploading to Existing File ");
                            OpenFileFromGoogleDrive();
                        }
                    }
                    else {
                        makeAlertDialog(" Failed ---% ");
                    }

                }
            };

    public void CreateFileOnGoogleDrive(DriveApi.DriveContentsResult result){
        final DriveContents driveContents = result.getDriveContents();
        if(DEBUG_MODE_ON) makeAlertDialog(" Starting Upload to New File ");
        // Perform I/O off the UI thread.
        new Thread() {
            @Override
            public void run() {
                // write content to DriveContents
                OutputStream outputStream = driveContents.getOutputStream();
                Writer writer = new OutputStreamWriter(outputStream);
                try {
                    writer.write(driveFileContents);
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                        .setTitle(driveFileName) // <--------------------------------------------------
                        .setMimeType("text/plain")
                        .setStarred(true).build();

                // create a file in root folder
                Drive.DriveApi.getRootFolder(client)
                        .createFile(client, changeSet, driveContents)
                        .setResultCallback(fileCallback);
            }
        }.start();

        if(DEBUG_MODE_ON) makeAlertDialog("Upload Complete");
    }

    final private ResultCallback<DriveFolder.DriveFileResult> fileCallback = new
            ResultCallback<DriveFolder.DriveFileResult>() {
                @Override
                public void onResult(DriveFolder.DriveFileResult result) {
                    if (result.getStatus().isSuccess()) {
                        makeAlertDialog("Upload Completed. ID: "+result.getDriveFile().getDriveId());
                        if(DEBUG_MODE_ON)
                            Toast.makeText(getApplicationContext(), "file created: "+""+
                                result.getDriveFile().getDriveId(), Toast.LENGTH_LONG).show();
                    }
                }
            };

    private void OpenFileFromGoogleDrive() {
        IntentSender intentSender = Drive.DriveApi
                .newOpenFileActivityBuilder()
                .setMimeType(new String[] { "text/plain", "text/html" })
                .build(client);
        try {
            startIntentSenderForResult(
                    intentSender, REQUEST_CODE_OPENER, null, 0, 0, 0);
        } catch (IntentSender.SendIntentException e) {
            e.printStackTrace();
        }
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

    private void createBlackBoxDataHandler() {
        checkPermission();
        blackBoxDataHandler = new Handler() {
            public void handleMessage(Message msg) {
                double amplitude = msg.getData().getDouble("amplitude");
                String messageStr = msg.getData().getString("msg");
                makeAlertDialog(messageStr);
            }
        };
    }

    public void checkEmergencySituations() {
        if(DEBUG_MODE_ON) makeAlertDialog(getSetReps());
        if( (absoluteEmergencySet.isEmpty() == false )||
                (dangerousSituationsSet.isEmpty() == false && distractionSet.isEmpty() == false )) {
            BlackBoxDataCapture.startOrContinueRecording(this);
        }
    }

    private String getSetReps() {
        return this.absoluteEmergencySet+"\n"+this.dangerousSituationsSet+"\n"+this.distractionSet;
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        if (sensor.getType() == Sensor.TYPE_PRESSURE && mockPressure == false) {
//            System.out.println(")))))))))))))))))))))))))) MockPressure:"+mockPressure);
            checkPressureChange(event.values[0]);
        } else if(sensor.getType() == Sensor.TYPE_LIGHT) {
            this.lastCapturedLightIntensity = event.values[0];
        } else if(sensor.getType() == Sensor.TYPE_PROXIMITY) {
            this.lastCapturedProximityValue = event.values[0];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void checkPressureChange(float newPressureValue) {
        newPressure = newPressureValue;
        if (percentChange(lastCapturedPressure, newPressureValue) > PRESSURE_CHANGE_THRESHOLD && pressureNotCapturedYet == false ) {
            if (DEBUG_MODE_ON) makeAlertDialog("Sudden Pressure Change Detected");
            absoluteEmergencySet.add(AbsoluteEmergency.PRESSURE_CHANGE);
            checkEmergencySituations();
            updatePressureStrings(true);
        } else {
            absoluteEmergencySet.remove(AbsoluteEmergency.PRESSURE_CHANGE);
            pressureNotCapturedYet = false;
            updatePressureStrings(false);
        }
        lastCapturedPressure = newPressure;
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
        if (amplitude > SOUND_AMPLITUDE_THRESHOLD && soundNotCapturedYet == false ) {
            absoluteEmergencySet.add(AbsoluteEmergency.LARGE_NOISE);
            updateSoundStrings(true);
            checkEmergencySituations();
        } else {
            absoluteEmergencySet.remove(AbsoluteEmergency.LARGE_NOISE);
            soundNotCapturedYet = false;
            updateSoundStrings(false);
        }
        lastCapturedSound = (int)Math.ceil(amplitude);
    }

    private void updateSoundStrings(boolean isDanger) {
        if (mockSound == false) {
            ((TextView) findViewById(R.id.soundTextView)).setText(String.format("%.2f", ((lastCapturedSound*140.0)/32768.0)) + " dB " + (isDanger ? " !!! " : ""));
        }
    }

    private void createCallListener() {
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String number) {
                checkCallEmergency(state, number);
            }
        };
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    private void checkCallEmergency(int state, String number) {
        switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                markOnCall(number);
                updateOnCallStrings();
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                markOnCall(number);
                break;
            case TelephonyManager.CALL_STATE_IDLE:
                markOffCall();
                break;
        }
    }

    private void markOnCall(String number) {
        isOnCall = true;
        numberOnCallWith = number;
        distractionSet.add(Distraction.ONGOING_CALL);
        updateOnCallStrings();
        checkEmergencySituations();
    }

    private void markOffCall() {
        isOnCall = false;
        this.numberOnCallWith = "";
        distractionSet.remove(Distraction.ONGOING_CALL);
        updateOnCallStrings();
    }

//    private void createCellBroadcastReceiver3() {
//
//        broadcastReceiver = new PhoneStateBroadcastReceiver(this);
//
//        callIntentFilter = new IntentFilter();
//        callIntentFilter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
//        callIntentFilter.addAction(Intent.ACTION_CALL);
//        callIntentFilter.addAction(Intent.EXTRA_PHONE_NUMBER);
//
//        registerReceiver(broadcastReceiver, callIntentFilter);
//
//        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
//
//        telephonyManager.listen(((PhoneStateBroadcastReceiver)broadcastReceiver).customPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
//    }

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
//                    distractionSet.add(Distraction.ONGOING_CALL);
//                    updateOnCallStrings();
//                    checkEmergencySituations();
//                } else {
//                    isOnCall = false;
//                    checkEmergencySituations();
//                    distractionSet.remove(Distraction.ONGOING_CALL);
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
            ((TextView) findViewById(R.id.onGoingCallTextView)).setText((isOnCall ? "Call with "+numberOnCallWith+"!!! " : "Not on a Call"));
        }
    }

    public void makeAlertDialog(String message) {
        if (message.toUpperCase().contains("EMER"))
            System.err.println("******             dialog" + message);
        else
            System.out.println("******             dialog" + message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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

//        registerReceiver(broadcastReceiver, callIntentFilter);
        if(phoneStateListener != null)
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
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

        if(phoneStateListener != null)
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        soundGenerator.interrupt();

//        unregisterReceiver(broadcastReceiver);

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
//        client.disconnect();

        if (client != null) {

            // disconnect Google API client connection
            client.disconnect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mPressure, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
        soundGenerator = new Thread(new SoundCaptureThread());
        soundGenerator.start();
        if(phoneStateListener != null)
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
//        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
//        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
//        registerReceiver(broadcastReceiver, callIntentFilter);
        if (client == null) {

            /**
             * Create the API client and bind it to an instance variable.
             * We use this instance as the callback for connection and connection failures.
             * Since no account name is passed, the user is prompted to choose.
             */
            client = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .addApi(AppIndex.API)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .build();
        }

        client.connect();
    }

    @Override
    protected void onPause() {
        // Be sure to unregister the sensor when the activity pauses.
        super.onPause();
        mSensorManager.unregisterListener(this);
        if(phoneStateListener != null)
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
//        unregisterReceiver(broadcastReceiver);

    }

    private void createLocationListener() {
        createLocationHandler();
        if (client == null) {
            makeAlertDialog("No Client");
        }

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10*1000);
        locationRequest.setFastestInterval(5*1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        final LocationListener listener = this;


        new AsyncTask<Void, Void, Void>(){

            boolean isListening = false;
            Location mLastLocation;

            @Override
            protected Void doInBackground(Void... params) {

                System.out.println(" &&&&&&&&&&&&&& do In Background &&&&&&&&&&&&&&&&&&&&& ");

                Looper.prepare();

                if ( DEBUG_MODE_ON ) makeAlertDialog(" -- Do in Background -- ");

                if (client != null && !client.isConnected() ) {
                    client.connect();
                }
                while(client.isConnecting());//wait

                if ( DEBUG_MODE_ON ) makeAlertDialog("client.isConnecting():"+client.isConnecting()+"client.isConnected():"+client.isConnected());

                if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    checkPermission();
                    makeAlertDialog("Permission Issues");
                }
                if (client.isConnected()) {
                    LocationServices
                            .FusedLocationApi
                            .requestLocationUpdates
                                    (client, locationRequest, listener);
                    if ( DEBUG_MODE_ON ) makeAlertDialog("Listening to Location now.");
                    mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                            client);
                    isListening = true;
                } else {
                    System.err.println(" Not Connected ");
                    if ( DEBUG_MODE_ON ) makeAlertDialog("Not Connected");
                }

                Looper.loop();
                return null;

            }
            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);
                if(DEBUG_MODE_ON)
                    makeAlertDialog("Listening to Location Now : " + mLastLocation.getLatitude()+":"+mLastLocation.getLongitude()+"@"+mLastLocation.getTime());
            }
        }.execute();
    }

    @Override
    public void onLocationChanged(Location location) {
        this.lastCapturedLocation = location;
        Bundle bundle = new Bundle();
        bundle.putParcelable("newLocation", location);

        Message message = new Message();
        message.setData(bundle);

        locationHandler.sendMessage(message);
    }

    private void createLocationHandler() {
        checkPermission();
        locationHandler = new Handler() {
            public void handleMessage(Message msg) {
                Location newLocation = (Location) msg.getData().getParcelable("newLocation");
                if (DEBUG_MODE_ON)
                    makeAlertDialog(newLocation.getLatitude()+":"+newLocation.getLongitude()+"@"+newLocation.getTime());
                if(speedNotCapturedYet && firstLocation == null) {
                    firstLocation = newLocation;
                    if (DEBUG_MODE_ON) makeAlertDialog("First Lcoation Captured");
                } else if(speedNotCapturedYet ){
                    lastCapturedSpeed = new Speed(firstLocation, newLocation);
                    speedNotCapturedYet = false;
                    if (DEBUG_MODE_ON) makeAlertDialog("First Speed Captured: "+lastCapturedSpeed.getSpeed());
                    checkSpeedEmergency();
                } else {
                    lastCapturedSpeed = Speed.fromOld(lastCapturedSpeed, newLocation);
                    if (DEBUG_MODE_ON)
                        makeAlertDialog("Speed Updated: "+lastCapturedSpeed.getSpeed());
                    checkSpeedEmergency();
                }
            }
        };
    }

    private void checkSpeedEmergency() {
        double currentSpeed = lastCapturedSpeed.getSpeed();
        if( currentSpeed >= Speed.MINIMUM_DRIVING_SPEED ) {
            dangerousSituationsSet.add(DangerousSituation.DRIVING);
            dangerousSituationsSet.remove(DangerousSituation.WALKING);
            updateDrivingWalkingStrings();
            checkEmergencySituations();
        } else if( currentSpeed >= Speed.MINIMUM_WALKING_SPEED ) {
            dangerousSituationsSet.add(DangerousSituation.WALKING);
            dangerousSituationsSet.remove(DangerousSituation.DRIVING);
            updateDrivingWalkingStrings();
            checkEmergencySituations();
        } else {
            dangerousSituationsSet.remove(DangerousSituation.DRIVING);
            dangerousSituationsSet.remove(DangerousSituation.WALKING);
            updateDrivingWalkingStrings();
            checkEmergencySituations();
        }
    }

    private void updateDrivingWalkingStrings() {
        double speed = -999;
        String speedStr;
        if(lastCapturedSpeed != null ) {
            speed = lastCapturedSpeed.getSpeed();
            speedStr = String.format("%.5f", speed);
            if(DEBUG_MODE_ON) makeAlertDialog("distance :"+lastCapturedSpeed.getDistance()+" speed:"+speed);
        } else {
            speedStr = "--";
        }
        if (mockDriving == false) {
            ((TextView) findViewById(R.id.drivingTextView)).setText(speedStr+" m/s"+(speed >= Speed.MINIMUM_DRIVING_SPEED ? " !!! ":""));
        }
        if (mockWalking == false) {
            ((TextView) findViewById(R.id.walkingTextView)).setText(speedStr+" m/s"+(speed >= Speed.MINIMUM_WALKING_SPEED ? " !!! ":""));
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {}

    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Called whenever the API client fails to connect.
        makeAlertDialog("GoogleApiClient connection failed: " + result.toString());
        if (!result.hasResolution()) {
            // show the localized error dialog.
            GoogleApiAvailability.getInstance().getErrorDialog(this, result.getErrorCode(), 0).show();
            return;
        }
        /**
         *  The failure has a resolution. Resolve it.
         *  Called typically when the app is not yet authorized, and an  authorization
         *  dialog is displayed to the user.
         */
        try {
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (IntentSender.SendIntentException e) {
            e.printStackTrace();
            makeAlertDialog("Exception while starting resolution activity");
        }
    }

    public Handler getBlackBoxDataHandler() {
        return blackBoxDataHandler;
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
        int accountsPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS);

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

        // If we don't have permissions, ask user for permissions
        if (accountsPermission != PackageManager.PERMISSION_GRANTED) {
            String[] PERMISSIONS_ACCOUNTS = {
                    Manifest.permission.GET_ACCOUNTS
            };
            int REQUEST_ACCOUNTS = 1;

            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_ACCOUNTS,
                    REQUEST_ACCOUNTS
            );
        }
    }
}
