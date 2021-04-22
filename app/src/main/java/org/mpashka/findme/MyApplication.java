package org.mpashka.findme;

import android.app.Application;

import timber.log.Timber;

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
