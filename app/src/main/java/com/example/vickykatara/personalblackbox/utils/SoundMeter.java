package com.example.vickykatara.personalblackbox.utils;

import android.media.MediaRecorder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;

import com.example.vickykatara.personalblackbox.MainActivity;

import java.io.IOException;

/**
 * Created by Vicky Katara on 28-Oct-16.
 */
public class SoundMeter {
    private MediaRecorder mRecorder = null;

    public void start() {
        if (mRecorder == null) {
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mRecorder.setMaxDuration(15000);
            mRecorder.setAudioSamplingRate(8000);
            mRecorder.setAudioEncodingBitRate(12200);
            mRecorder.setOutputFile("/dev/null");
            try {
                mRecorder.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mRecorder.start();
        }
    }

    public void stop() {
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
    }

    public double getAmplitude() {
        if (mRecorder != null) {

            double maxAmp = 0;
            int count = 0;
            while(++count < 20) {
                double someAmp = mRecorder.getMaxAmplitude();
                if(someAmp > maxAmp)
                    maxAmp = someAmp;
            }

//            double amp = mRecorder.getMaxAmplitude();
            if(MainActivity.DEBUG_MODE_ON) System.out.println("################# ----- "+maxAmp);
            return maxAmp;
        }
        else
            return 0;

    }
}
