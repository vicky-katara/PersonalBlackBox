package com.example.vickykatara.personalblackbox;

import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.os.Message;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.example.vickykatara.personalblackbox.MainActivity.DEBUG_MODE_ON;

/**
 * Created by Vicky Katara on 01-Nov-16.
 */

public class BlackBoxDataCapture implements Runnable{

//    private static final long TOTAL_TIME_TO_RECORD_NANO_SECS = 5*1000000L;
    private static final long TOTAL_TIME_TO_RECORD_NANO_SECS = 10000000000L;
    private static final long TIME_STEP_SIZE_MILLISECS = 100L;

    private static long currentStartTimeStamp = -1;

    private SimpleDateFormat sdfFileName = new SimpleDateFormat("yyyy_MMM_dd_HH_mm_ss_SSS");
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MMM/dd HH:mm:ss.SSS");

    private static boolean isRecording = false;

    private static MainActivity mainActivity;

    private static BlackBoxDataCapture instance;

    private BlackBoxDataCapture() {
        new Thread(this).start();
    }

    public static void startOrContinueRecording(MainActivity mainActivity) {
        BlackBoxDataCapture.mainActivity = mainActivity;
        if(instance == null || isRecording == false) {
            sendString("Beginning Recording ...");
            currentStartTimeStamp = System.nanoTime();
            instance = new BlackBoxDataCapture();
        } else {
            // instance exists and is recording
            sendString("Continued Recording ...");
            currentStartTimeStamp = System.nanoTime();
        }
    }

    @Override
    public void run() {
        Looper.prepare();
        StringBuilder sb = new StringBuilder();
        String fileNameTimeStamp = getFileNameTimeStamp();
        isRecording = true;
        String times = "Current Time: "+System.nanoTime()+" - End Time: "+(currentStartTimeStamp + TOTAL_TIME_TO_RECORD_NANO_SECS);
        if(DEBUG_MODE_ON) sendString("Start"+times);
//        System.out.println(" ------ times ---- "+times);
        while((currentStartTimeStamp + TOTAL_TIME_TO_RECORD_NANO_SECS) >= System.nanoTime()) {
            try {
                Thread.sleep(TIME_STEP_SIZE_MILLISECS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            String s;
            sb.append(s=getBlackBoxString());
            if(DEBUG_MODE_ON) sendString(s);
        }
        isRecording = false;
        writeToFile("BlackBox-"+fileNameTimeStamp+".txt", sb);
        sendString("Local File Write Complete");
        String times2 = "Current Time: "+System.nanoTime()+" - End Time: "+(currentStartTimeStamp + TOTAL_TIME_TO_RECORD_NANO_SECS);
        if(DEBUG_MODE_ON) sendString("End"+times2);
//        System.out.println(" ------ times2 ---- "+times2);
        mainActivity.uploadToDrive("BlackBox-"+fileNameTimeStamp+".txt", sb);
        Looper.loop();
    }

    public void writeToFile(String fileName, StringBuilder contents) {
        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File (sdCard.getAbsolutePath() + "/blackboxdata");
        File toBeWritten = new File(dir, fileName);
        dir.mkdirs();
        try {
            FileOutputStream fos = new FileOutputStream(toBeWritten);
            fos.write(contents.toString().getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private String getBlackBoxString() {
        StringBuilder sb = new StringBuilder();
        //date Time
        sb.append("@");sb.append(getDateTime());sb.append(":");
        // location
        sb.append("location:");sb.append(mainActivity.lastCapturedLocation);sb.append(",");
        // light
        sb.append("light:");sb.append(mainActivity.lastCapturedLightIntensity);sb.append(",");
        // pressure
        sb.append("pressure:");sb.append(mainActivity.lastCapturedPressure);sb.append(",");
        // proximity
        sb.append("proximity:");sb.append(mainActivity.lastCapturedProximityValue);sb.append(",");
        // speed
        sb.append("speed:");sb.append(mainActivity.lastCapturedSpeed);sb.append(",");
        // sound
        sb.append("sound:");sb.append(mainActivity.lastCapturedSound);sb.append(",");
        // onCall
        sb.append("onCall:");sb.append(mainActivity.isOnCall);sb.append(",");
        // onCallWith
        sb.append("onCallWith:");sb.append(mainActivity.numberOnCallWith);sb.append(",");
        // texting
        sb.append("texting:");sb.append(mainActivity.isKeyboardDrawn);sb.append(",");
        // absoluteEmergencySet
        sb.append("absoluteEmergencySet:");sb.append(mainActivity.absoluteEmergencySet);sb.append(",");
        // dangerousSituationsSet
        sb.append("dangerousSituationsSet:");sb.append(mainActivity.dangerousSituationsSet);sb.append(",");
        // distractionSet
        sb.append("distractionSet:");sb.append(mainActivity.distractionSet);sb.append("\n");

        return sb.toString();
    }

    private String getDateTime() {
        return sdf.format(new Date());
    }
    private String getFileNameTimeStamp() { return sdfFileName.format(new Date()); }

    private static void sendString(String s) {
        Bundle bundle = new Bundle();
        bundle.putString("msg", s);

        Message message = new Message();
        message.setData(bundle);

        mainActivity.getBlackBoxDataHandler().sendMessage(message);

    }


}
