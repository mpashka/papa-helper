package org.mpashka.findme;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;

import java.util.List;

import timber.log.Timber;

public class MyLocationService extends Service {

    private boolean isRunning = false;

    public void onCreate() {
        super.onCreate();
        isRunning = false;
        Timber.d("onCreate");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Timber.d("onStartCommand");
        if (!isRunning || intent.getBooleanExtra(MyWorkManager.SETTINGS_ACTION, false)) {
            startListen();
        } else {
            Timber.d("Service already running");
        }
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

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Timber.d("onLocationChanged %s", location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Timber.d("onStatusChanged %s/%s/%s", provider, status, extras);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Timber.d("onProviderEnabled %s", provider);
        }

        @Override
        public void onProviderDisabled(String provider) {
            Timber.d("onProviderDisabled %s", provider);
        }
    };

    private void startListen() {
        Timber.d("startListen()");
        isRunning = true;
        Context deviceContext = createDeviceProtectedStorageContext();
        SharedPreferences devicePreferences = deviceContext.getSharedPreferences(MyWorkManager.SETTINGS_NAME, Context.MODE_PRIVATE);

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationManager.removeUpdates(locationListener);
        String locationProvider = devicePreferences.getString(getString(R.string.location_id), getString(R.string.location_default));
        int timeSec = devicePreferences.getInt(getString(R.string.location_time_id), getResources().getInteger(R.integer.location_time_default));
        float minDistance = devicePreferences.getFloat(getString(R.string.location_min_distance_id), getResources().getFloat(R.dimen.location_min_distance_default));
        try {
            locationManager.requestLocationUpdates(locationProvider, 1000 * timeSec, minDistance, locationListener);
        } catch (SecurityException e) {
            Timber.e(e, "Not enough permissions");
        }
    }

    private void stopListen() {
        Timber.d("stopListen()");
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationManager.removeUpdates(locationListener);
        isRunning = false;
    }
}