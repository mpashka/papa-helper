package org.mpashka.findme;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.TimerTask;

import timber.log.Timber;

public class MyWorkManager {
    public static final String NAME = "org.mpashka.findme";
    public static final String RESTART_SERVICE = "_restart";
    public static final String ACCELEROMETER_SERVICE = "_accelerometer";
    public static final String SETTINGS_ACTION = "reload_settings";

    private static final MyWorkManager INSTANCE = new MyWorkManager();

    public MyWorkManager() {
        Timber.d("New::MyWorkManager %s/%s", Thread.currentThread().getName(), Thread.currentThread().getId());
    }

    public static class RestartWorker extends Worker {
        public RestartWorker(@NonNull Context appContext, @NonNull WorkerParameters workerParams) {
            super(appContext, workerParams);
        }

        @NonNull
        @Override
        public Result doWork() {
            Timber.d("RestartWorker::doWork()");
            Context context = getApplicationContext();
            context.startService(new Intent(context, MyLocationService.class));
            context.startService(new Intent(context, MyAccelerometerService.class));
            return Result.success();
        }
    }

    public static MyWorkManager getInstance() {
        return INSTANCE;
    }

    /**
     * Starts work manager if necessary
     * @param context
     */
    public void startServices(Context context) {
        try {
            doScheduleCheck(context);
        } catch (Exception e) {
            Timber.e(e, "Error scheduleCheck()");
        }
    }

    private void doScheduleCheck(Context context) {
        Timber.d("scheduleCheck");

        WorkManager workManager = WorkManager.getInstance(context);
        MyPreferences preferences = new MyPreferences(context);
        int restartCheckSec = preferences.getInt(R.string.restart_check_id, R.integer.restart_check_default);
        workManager.enqueueUniquePeriodicWork(context.getString(R.string.app_id) + RESTART_SERVICE, ExistingPeriodicWorkPolicy.KEEP,
                new PeriodicWorkRequest.Builder(RestartWorker.class, Duration.of(restartCheckSec, ChronoUnit.SECONDS)).build());

        context.startService(new Intent(context, MyLocationService.class));
        context.startService(new Intent(context, MyAccelerometerService.class));
    }

    public void rescheduleAccelerometerService(Context context) {
        try {
            Timber.d("rescheduleAccelerometerService()");
            Intent service = new Intent(context, MyLocationService.class);
            service.putExtra(SETTINGS_ACTION, true);
            context.startService(service);
        } catch (Exception e) {
            Timber.e(e, "Error rescheduleAccelerometerService()");
        }
    }

    public void restartLocationService(Context context) {
        try {
            Timber.d("restartLocationService()");
            Intent service = new Intent(context, MyLocationService.class);
            service.putExtra(SETTINGS_ACTION, true);
            context.startService(service);
        } catch (Exception e) {
            Timber.e(e, "Error restartLocationService");
        }
    }

}
