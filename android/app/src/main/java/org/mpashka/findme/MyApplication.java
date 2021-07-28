package org.mpashka.findme;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.work.Configuration;

import com.google.firebase.messaging.FirebaseMessaging;

import org.jetbrains.annotations.NotNull;
import org.mpashka.findme.db.MyDb;
import org.mpashka.findme.services.MyWorkManager;

import javax.inject.Inject;

import dagger.hilt.android.HiltAndroidApp;
import timber.log.Timber;

@HiltAndroidApp
public class MyApplication extends Application implements Configuration.Provider {

    public static final String NAME = "org.mpashka.findme";

    @Inject
    MyDb db;

    @Inject
    MyPreferences preferences;

    @Inject
    MyWorkManager myWorkManager;

    @Inject
    HiltWorkerFactory workerFactory;

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.plant(new Timber.DebugTree());
//        Timber.tag("findme");
        Timber.i("Application start. Debug:%s", BuildConfig.DEBUG);
        generateFirebaseToken();
        myWorkManager.startIfNeeded();
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

    @NonNull
    @NotNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setMinimumLoggingLevel(Log.VERBOSE)
                .setWorkerFactory(workerFactory)
                .build();
    }

    private void generateFirebaseToken() {
        String fcmTokenProperty = getString(R.string.fcm_token);
        SharedPreferences preferences = this.preferences.getPreferences();
        if (!preferences.contains(fcmTokenProperty)) {
            FirebaseMessaging.getInstance().getToken()
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Timber.w(task.getException(), "Fetching FCM registration token failed ");
                            return;
                        }
                        String token = task.getResult();
                        preferences.edit()
                                .putString(fcmTokenProperty, token)
                                .apply();
                    });
        }
    }
}
