package org.mpashka.findme;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.os.IBinder;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class MyAccelerometerService extends Service {

    private ScheduledExecutorService timer;
    private Runnable timerTask;
    private ScheduledFuture<?> timerTaskFuture;
    private DBHelper dbHelper;
    private MyPreferences preferences;
    private AccelerationListener accelerationListener;

    public void onCreate() {
        super.onCreate();
        Timber.d("onCreate");
        dbHelper = new DBHelper(createDeviceProtectedStorageContext());
        timer = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "AccelerometerTimer"));
        preferences = new MyPreferences(this);
        timerTask = () -> {
            try {
                accelerationListener.startListen();
            } catch (Exception e) {
                Timber.e(e, "Error query db");
            }
        };
        accelerationListener = new AccelerationListener(dbHelper);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Timber.d("onStartCommand");
        boolean restart = intent.getBooleanExtra(MyWorkManager.SETTINGS_ACTION, false);
        if (restart && timerTaskFuture != null) {
            timerTaskFuture.cancel(false);
            timerTaskFuture = null;
        }
        if (timerTaskFuture == null) {
            int pollPeriodSec = preferences.getInt(R.string.poll_period_id, R.integer.poll_period_default);
            timerTaskFuture = timer.scheduleWithFixedDelay(timerTask, pollPeriodSec, pollPeriodSec, TimeUnit.SECONDS);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public void onDestroy() {
        Timber.d("onDestroy");
        super.onDestroy();
        if (timerTaskFuture != null) {
            timerTaskFuture.cancel(false);
        }
        timer.shutdown();
        accelerationListener.stopListen();
        dbHelper.close();
    }

    public IBinder onBind(Intent intent) {
        Timber.d("onBind");
        return null;
    }

    class AccelerationListener implements SensorEventListener2 {
        private Instant endTime;
        private int count;
        private double sum;
        private double max;
        private DBHelper dbHelper;

        public AccelerationListener(DBHelper dbHelper) {
            this.dbHelper = dbHelper;
        }

        public void reset() {
            count = 0;
            sum = 0;
            max = Double.MIN_NORMAL;
        }

        @Override
        public void onFlushCompleted(Sensor sensor) {
            Timber.d("onFlushCompleted");
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
//            Timber.d("onSensorChanged. %s, %s, %s", event.values[0], event.values[1], event.values[2]);
            double value = Math.sqrt(event.values[0]*event.values[0] + event.values[1]*event.values[1] + event.values[2]*event.values[2]);
            count++;
            sum +=  value;
            max = Math.max(max, value);
            if (Instant.now().isAfter(endTime)) {
                Timber.d("Stop sensors");
                stopListenAndReport(max, sum / count);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Timber.d("onAccuracyChanged %s", accuracy);
        }

        private void startListen() {
            Timber.d("startListen()");
            accelerationListener.reset();
//        int pollPeriodSec = devicePreferences.getInt(getString(R.string.poll_period_id), getResources().getInteger(R.integer.poll_period_default));
            int pollTimeSec = preferences.getInt(R.string.poll_time_id, R.integer.poll_time_default);
            endTime = Instant.now().plus(Duration.of(pollTimeSec, ChronoUnit.SECONDS));
            int pollSampling = preferences.getInt(R.string.poll_sampling_id, R.integer.poll_sampling_default);

            SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            sensorManager.unregisterListener(accelerationListener);
            List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_LINEAR_ACCELERATION);
            // SensorManager.SENSOR_DELAY_NORMAL
            sensors.forEach(s -> sensorManager.registerListener(accelerationListener, s, pollSampling));
        }

        public void stopListen() {
            Timber.d("stopListen()");
            SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            sensorManager.unregisterListener(accelerationListener);
        }

        private void stopListenAndReport(double max, double avg) {
            stopListen();
            Timber.d("report(%s, %s)", max, avg);
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues cv = new ContentValues();
            Instant now = Instant.now();
            cv.put("time", now.toEpochMilli());
            cv.put("avg", avg);
            cv.put("max", max);
            cv.put("battery", Utils.readChargeLevel(getApplicationContext()));
            db.insert("accelerometer", null, cv);
            db.close();
        }
    }

}
