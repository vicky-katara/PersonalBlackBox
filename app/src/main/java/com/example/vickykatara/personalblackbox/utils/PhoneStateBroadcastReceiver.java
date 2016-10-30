package com.example.vickykatara.personalblackbox.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.example.vickykatara.personalblackbox.MainActivity;

/**
 * Created by Vicky Katara on 30-Oct-16.
 */

public class PhoneStateBroadcastReceiver extends BroadcastReceiver {

    MainActivity mainActivity;
    public CustomPhoneStateListener customPhoneStateListener;


    public PhoneStateBroadcastReceiver() {

    }

    public PhoneStateBroadcastReceiver(MainActivity mainActivity) {
        super();
        this.mainActivity = mainActivity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(mainActivity != null)
        mainActivity.makeAlertDialog("On Receive: "+intent.getAction());
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen((customPhoneStateListener =  new CustomPhoneStateListener(context, mainActivity)), PhoneStateListener.LISTEN_CALL_STATE);
    }
}
