package org.mpashka.findme.services;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.AndroidEntryPoint;
import dagger.hilt.android.qualifiers.ApplicationContext;
import timber.log.Timber;

@Singleton
public class MyActivityService implements MyListenableServiceInterface {

    private static final String[] PERMISSIONS = {"com.google.android.gms.permission.ACTIVITY_RECOGNITION"};

    private ActivityRecognitionClient activityRecognitionClient;

    private PendingIntent activityPendingIntent;

    @Inject
    public MyActivityService(@ApplicationContext Context applicationContext) {
        this.activityRecognitionClient = ActivityRecognition.getClient(applicationContext);

        Intent intent = new Intent(applicationContext, MyLocationListener.class)
                .setAction(MyLocationListener.ACTIVITY_UPDATE);
        activityPendingIntent = PendingIntent.getBroadcast(applicationContext, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public String[] getPermissions() {
        return PERMISSIONS;
    }

    @Override
    public void startListen() {
        List<ActivityTransition> transitions = new ArrayList<>();
        for (int a = DetectedActivity.IN_VEHICLE; a <= DetectedActivity.RUNNING; a++) {
            if (a < DetectedActivity.UNKNOWN || a > DetectedActivity.WALKING) {
                transitions.add(new ActivityTransition.Builder()
                        .setActivityType(a)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build());
            }
        }

        Task<Void> task = activityRecognitionClient
                .requestActivityTransitionUpdates(new ActivityTransitionRequest(transitions), activityPendingIntent);
        task.addOnSuccessListener(r -> Timber.i("Activity transition result %s", r));
        task.addOnFailureListener(e -> Timber.e(e, "Error activity recognition"));
    }

    @Override
    public void stopListen() {
        activityRecognitionClient.removeActivityTransitionUpdates(activityPendingIntent);
    }
}
