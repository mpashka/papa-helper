package org.mpashka.findme;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import timber.log.Timber;

public class MyService extends Service {

    public MyService() {
    }

    public void onCreate() {
        super.onCreate();
        Timber.d("onCreate");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Timber.d("onStartCommand");
        someTask();
        return super.onStartCommand(intent, flags, startId);
    }

    public void onDestroy() {
        super.onDestroy();
        Timber.d("onDestroy");
    }

    public IBinder onBind(Intent intent) {
        Timber.d("onBind");
        return null;
    }

    void someTask() {
    }
}