package org.mpashka.findme;

import android.Manifest;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import java.time.Instant;

import timber.log.Timber;

public class MyLocationService extends Service {

    private boolean isRunning = false;
    private DBHelper dbHelper;

    public void onCreate() {
        super.onCreate();
        isRunning = false;
        Context deviceContext = createDeviceProtectedStorageContext();
        dbHelper = new DBHelper(deviceContext);
        Timber.d("onCreate");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Timber.d("onStartCommand");
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Timber.d("Not enough permissions");
            Context context = getApplicationContext();
            Toast.makeText(context, "Not enough permissions", Toast.LENGTH_SHORT).show();
        } else if (!isRunning || intent.getBooleanExtra(MyWorkManager.SETTINGS_ACTION, false)) {
            startListen();
        } else {
            Timber.d("Service already running");
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public void onDestroy() {
        stopListen();
        dbHelper.close();
        super.onDestroy();
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
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues cv = new ContentValues();
            Instant now = Instant.now();
            cv.put("time", now.toEpochMilli());
            cv.put("lat", location.getLatitude());
            cv.put("long", location.getLongitude());
            cv.put("accuracy", location.getAccuracy());
            cv.put("battery", Utils.readChargeLevel(getApplicationContext()));
            db.insert("location", null, cv);
            db.close();
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
        MyPreferences preferences = new MyPreferences(this);

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationManager.removeUpdates(locationListener);
        String locationProvider = preferences.getString(R.string.location_provider_id, R.string.location_provider_default);
        int timeSec = preferences.getInt(R.string.location_time_id, R.integer.location_time_default);
        float minDistance = preferences.getFloat(R.string.location_min_distance_id, R.dimen.location_min_distance_default);
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