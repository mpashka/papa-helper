package org.mpashka.findme;

import android.app.Application;
import android.content.SharedPreferences;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.polidea.rxandroidble2.RxBleClient;

import org.mpashka.findme.db.MyDb;

import java.util.Timer;

import javax.inject.Inject;

import dagger.hilt.android.HiltAndroidApp;
import timber.log.Timber;

@HiltAndroidApp
public class MyApplication extends Application {

    @Inject
    MyDb db;

    @Inject
    MyPreferences preferences;

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.plant(new Timber.DebugTree());
//        Timber.tag("findme");
        Timber.d("Application start. Debug:%s", BuildConfig.DEBUG);
        generateFirebaseToken();
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
