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

import timber.log.Timber;

public class MyWorkManager {
    public static final String RESTART_SERVICE = "_restart";
    public static final String ACCELEROMETER_SERVICE = "_accelerometer";
    public static final String SETTINGS_NAME = "org.mpashka.findme_preferences";
    public static final String SETTINGS_ACTION = "reload_settings";

    private static final MyWorkManager INSTANCE = new MyWorkManager();


    public static class RestartWorker extends Worker {
        public RestartWorker(@NonNull Context appContext, @NonNull WorkerParameters workerParams) {
            super(appContext, workerParams);
        }

        @NonNull
        @Override
        public Result doWork() {
            Timber.d("RestartWorker::doWork()");
            getInstance().startLocationService(getApplicationContext(), false);
            return Result.success();
        }
    }

    public static class StartAccelerometerWorker extends Worker {
        public StartAccelerometerWorker(@NonNull Context appContext, @NonNull WorkerParameters workerParams) {
            super(appContext, workerParams);
        }

        @NonNull
        @Override
        public Result doWork() {
            Timber.d("StartAccelerometerWorker::doWork()");
            getInstance().startAccelerometerService(getApplicationContext());
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
    public void scheduleCheck(Context context) {
        try {
            doScheduleCheck(context);
        } catch (Exception e) {
            Timber.e(e, "Error scheduleCheck()");
        }
    }

    private void doScheduleCheck(Context context) {
        Timber.d("scheduleCheck");
        Context deviceContext = context.createDeviceProtectedStorageContext();
        WorkManager workManager = WorkManager.getInstance(deviceContext);
        /*Operation enqueue = */
        int restartCheck = context.getResources().getInteger(R.integer.restart_check);
        workManager.enqueueUniquePeriodicWork(context.getString(R.string.app_id) + RESTART_SERVICE, ExistingPeriodicWorkPolicy.KEEP,
                new PeriodicWorkRequest.Builder(RestartWorker.class, Duration.of(restartCheck, ChronoUnit.SECONDS)).build());

        rescheduleAccelerometerCheck(deviceContext, false);
    }

    public void rescheduleAccelerometerCheck(Context context) {
        try {
            doRescheduleAccelerometerCheck(context);
        } catch (Exception e) {
            Timber.e(e, "Error rescheduleAccelerometerCheck()");
        }
    }

    private void doRescheduleAccelerometerCheck(Context context) {
        Context deviceContext = context.createDeviceProtectedStorageContext();
        rescheduleAccelerometerCheck(deviceContext, true);
    }

    private void rescheduleAccelerometerCheck(Context deviceContext, boolean restart) {
        WorkManager workManager = WorkManager.getInstance(deviceContext);
        SharedPreferences devicePreferences = deviceContext.getSharedPreferences(MyWorkManager.SETTINGS_NAME, Context.MODE_PRIVATE);
        int pollPeriodSec = devicePreferences.getInt(deviceContext.getString(R.string.poll_period_id), deviceContext.getResources().getInteger(R.integer.poll_period_default));
        workManager.enqueueUniquePeriodicWork(deviceContext.getString(R.string.app_id) + ACCELEROMETER_SERVICE,
                restart ? ExistingPeriodicWorkPolicy.REPLACE : ExistingPeriodicWorkPolicy.KEEP,
                new PeriodicWorkRequest.Builder(StartAccelerometerWorker.class, Duration.of(pollPeriodSec, ChronoUnit.SECONDS)).build());
    }

    public void restartLocationService(Context context) {
        startLocationService(context, true);
    }

    private void startLocationService(Context context, boolean reload) {
        Timber.d("startLocationService()");
        Intent service = new Intent(context, MyLocationService.class);
        if (reload) {
            service.putExtra(SETTINGS_ACTION, true);
        }
        context.startService(service);
    }

    public void startAccelerometerService(Context context) {
        Timber.d("startAccelerometerService()");
        context.startService(new Intent(context, MyAccelerometerService.class));
    }
}
