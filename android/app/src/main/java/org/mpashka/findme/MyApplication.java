package org.mpashka.findme;

import android.app.Application;

import dagger.hilt.android.HiltAndroidApp;
import timber.log.Timber;

@HiltAndroidApp
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.plant(new Timber.DebugTree());
//        Timber.tag("findme");
        Timber.d("Application start. Debug:%s", BuildConfig.DEBUG);

        MyWorkManager.getInstance().startServices(this);
    }
}
