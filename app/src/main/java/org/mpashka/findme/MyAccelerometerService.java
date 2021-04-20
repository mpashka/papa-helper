package org.mpashka.findme;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.IBinder;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import timber.log.Timber;

public class MyAccelerometerService extends Service {

    private Instant endTime;

    public void onCreate() {
        super.onCreate();
        Timber.d("onCreate");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Timber.d("onStartCommand");
        startListen();
        return super.onStartCommand(intent, flags, startId);
    }

    public void onDestroy() {
        super.onDestroy();
        stopListen();
        Timber.d("onDestroy");
    }

    public IBinder onBind(Intent intent) {
        Timber.d("onBind");
        return null;
    }

    class MyListener implements SensorEventListener2 {
        private int count;
        private double sum;
        private double max;

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
            Timber.d("onSensorChanged. %s, %s, %s", event.values[0], event.values[1], event.values[2]);
            double value = Math.sqrt(event.values[0]*event.values[0] + event.values[1]*event.values[1] + event.values[2]*event.values[2]);
            count++;
            sum +=  value;
            max = Math.max(max, value);
            if (Instant.now().isAfter(endTime)) {
                Timber.d("Stop sensors");
                stopListen();
                stopSelf();
                report(max, sum / count);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Timber.d("onAccuracyChanged %s", accuracy);
        }
    }

    private MyListener accelListener = new MyListener();

    private void startListen() {
        Timber.d("startListen()");
        accelListener.reset();
        Context deviceContext = createDeviceProtectedStorageContext();
        SharedPreferences devicePreferences = deviceContext.getSharedPreferences(MyWorkManager.SETTINGS_NAME, Context.MODE_PRIVATE);
//        int pollPeriodSec = devicePreferences.getInt(getString(R.string.poll_period_id), getResources().getInteger(R.integer.poll_period_default));
        int pollTimeSec = devicePreferences.getInt(getString(R.string.poll_time_id), getResources().getInteger(R.integer.poll_time_default));
        endTime = Instant.now().plus(Duration.of(pollTimeSec, ChronoUnit.SECONDS));
        int pollSampling = devicePreferences.getInt(getString(R.string.poll_sampling_id), getResources().getInteger(R.integer.poll_sampling_default));

        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.unregisterListener(accelListener);
        List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_LINEAR_ACCELERATION);
        // SensorManager.SENSOR_DELAY_NORMAL
        sensors.forEach(s -> sensorManager.registerListener(accelListener, s, pollSampling));
    }

    private void stopListen() {
        Timber.d("stopListen()");
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.unregisterListener(accelListener);
    }

    private void report(double max, double avg) {
    }
}
