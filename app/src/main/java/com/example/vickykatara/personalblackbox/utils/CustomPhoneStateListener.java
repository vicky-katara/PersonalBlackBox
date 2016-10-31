package com.example.vickykatara.personalblackbox.utils;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.example.vickykatara.personalblackbox.MainActivity;
import com.example.vickykatara.personalblackbox.types.Distraction;

/**
 * Created by Vicky Katara on 30-Oct-16.
 */
public class CustomPhoneStateListener extends PhoneStateListener {
    private final Context context;
    private MainActivity mainActivity;

    public CustomPhoneStateListener(Context context, MainActivity mainActivity) {
        super();
        this.context = context;
        this.mainActivity = mainActivity;
    }

    @Override
    public void onCallStateChanged(int state, String incomingNumber) {
        super.onCallStateChanged(state, incomingNumber);

        switch (state) {
            case TelephonyManager.CALL_STATE_IDLE:
                //when Idle i.e no call
                markOffCall();
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                //when Off hook i.e in call
                //Make intent and start your service here
                markOnCall();
                break;
            case TelephonyManager.CALL_STATE_RINGING:
                //when Ringing
                markOnCall();
                break;
            default:
                break;
        }
    }

    private void markOnCall() {
        if(mainActivity == null) {
            System.out.println("<<<<<<<<<<<<<<<<<< mainActivity == null");
            return;
        }
        mainActivity.isOnCall = true;
        mainActivity.distractionSet.add(Distraction.ONGOING_CALL);
        mainActivity.updateOnCallStrings();
        mainActivity.checkEmergencySituations();
        mainActivity.makeAlertDialog("On Call");
    }

    private void markOffCall() {
        if(mainActivity == null) {
            System.out.println("<<<<<<<<<<<<<<<<<< mainActivity == null");
            return;
        }
        mainActivity.isOnCall = false;
        mainActivity.checkEmergencySituations();
        mainActivity.distractionSet.remove(Distraction.ONGOING_CALL);
        mainActivity.makeAlertDialog("Off Call");
    }
}
