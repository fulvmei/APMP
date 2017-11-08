package com.chengfu.fuexoplayer.samples;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.chengfu.fuexoplayer.AudioNotificationManager;

/**
 * Created by ChengFu on 2017/9/27.
 */

public class TestService extends Service {

    private AudioNotificationManager mAudioNotificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("TestService", "onCreate");
        mAudioNotificationManager = new AudioNotificationManager(this);

        mAudioNotificationManager.startNotification();
    }

    @Override
    public void onDestroy() {
        Log.e("TestService", "onDestroy");
        mAudioNotificationManager.stopNotification();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
