package org.mpashka.findme;

import android.app.Application;

import org.mpashka.findme.db.MyDb;

import java.util.Timer;

import javax.inject.Inject;

import dagger.hilt.android.HiltAndroidApp;
import timber.log.Timber;

@HiltAndroidApp
public class MyApplication extends Application {

    @Inject
    MyDb db;

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.plant(new Timber.DebugTree());
//        Timber.tag("findme");
        Timber.d("Application start. Debug:%s", BuildConfig.DEBUG);

        MyWorkManager.getInstance().startServices(this);
    }

    @Override
    public void onTerminate() {
        try {
            db.close();
        } catch (Exception e) {
            Timber.e(e, "Error db close");
        }
        super.onTerminate();
    }
}
