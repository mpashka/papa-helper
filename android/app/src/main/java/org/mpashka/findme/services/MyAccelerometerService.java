package org.mpashka.findme.services;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;

import org.mpashka.findme.MyPreferences;
import org.mpashka.findme.db.LocationEntity;
import org.mpashka.findme.R;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.AndroidEntryPoint;
import dagger.hilt.android.qualifiers.ApplicationContext;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleTransformer;
import timber.log.Timber;

@Singleton
public class MyAccelerometerService {

    private MyPreferences preferences;
    private SensorManager sensorManager;
    private AccelerationListener accelerationListener = new AccelerationListener();

    @Inject
    public MyAccelerometerService(@ApplicationContext Context context, MyPreferences preferences) {
        this.preferences = preferences;
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }

    public SingleTransformer<LocationEntity, LocationEntity> fetchSensors() {
        return isEnabled() ? l -> {
            int pollTimeSec = preferences.getInt(R.string.accel_poll_time_id, R.integer.accel_poll_time_default);
            return l.doOnSuccess(l1 -> startListen())
                    .delay(pollTimeSec, TimeUnit.SECONDS)
                    .doOnSuccess(this::stopListenAndReport);
        } : l -> l;
    }

    private boolean isEnabled() {
        return preferences.getBoolean(R.string.accel_provider_enabled_id, R.bool.accel_provider_enabled_default);
    }

    private void startListen() {
        Timber.i("startListen()");
        accelerationListener.reset();
        int pollSampling = preferences.getInt(R.string.accel_poll_sampling_id, R.integer.accel_poll_sampling_default);

        sensorManager.unregisterListener(accelerationListener);
        List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_LINEAR_ACCELERATION);
        // SensorManager.SENSOR_DELAY_NORMAL
//            sensors.forEach(s -> sensorManager.registerListener(accelerationListener, s, pollSampling));
        for (Sensor sensor : sensors) {
            Timber.i("Sensor: %s", sensor);
            try {
                sensorManager.registerListener(accelerationListener, sensor, pollSampling);
            } catch (Exception e) {
                Timber.e(e, "Error start listen for sensor %s", sensor);
            }
        }
    }

    private void stopListenAndReport(LocationEntity l) {
        Timber.i("stopListen and report()");
        sensorManager.unregisterListener(accelerationListener);
        accelerationListener.report(l);
    }

    class AccelerationListener implements SensorEventListener2 {
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
            Timber.i("onFlushCompleted");
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
//            Timber.i("onSensorChanged. %s, %s, %s", event.values[0], event.values[1], event.values[2]);
            double value = Math.sqrt(event.values[0]*event.values[0] + event.values[1]*event.values[1] + event.values[2]*event.values[2]);
            count++;
            sum +=  value;
            max = Math.max(max, value);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Timber.i("onAccuracyChanged %s", accuracy);
        }

        public void report(LocationEntity locationEntity) {
            Timber.i("Total sensors: %s, %s, %s", count, sum, max);
            locationEntity.setAccelerometer(count, count > 0 ? sum / count : 0, max);
        }
    }
}
