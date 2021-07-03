package org.mpashka.findme;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.room.Insert;

import org.mpashka.findme.db.LocationDao;
import org.mpashka.findme.db.LocationEntity;
import org.mpashka.findme.db.MyTransmitService;
import org.mpashka.findme.miband.MiBandManager;

import java.time.Instant;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

@AndroidEntryPoint
public class MyLocationService extends Service {

    @Inject
    MyTransmitService transmitService;

    @Inject
    LocationDao locationDao;

    @Inject
    MiBandManager miBandManager;

    private boolean isRunning = false;

    public void onCreate() {
        Timber.d("onCreate");
        super.onCreate();
        String channelId = getString(R.string.location_notification_channel_id);
        NotificationChannel notificationChannel = new NotificationChannel(channelId,
                getString(R.string.location_notification_channel_name),
                NotificationManager.IMPORTANCE_NONE);
        notificationChannel.setLightColor(Color.BLUE);
        notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(notificationChannel);

        startForeground(getResources().getInteger(R.integer.location_notification_peer_id),
                new NotificationCompat.Builder(this, channelId)
                        .setOngoing(true)
                        .setContentTitle(getString(R.string.location_notification_title))
                        .setContentText(getString(R.string.location_notification_text))
                        .setSmallIcon(R.drawable.ic_notification_location)
                        .setTicker(getString(R.string.location_notification_ticker))
                        .setPriority(NotificationCompat.PRIORITY_MIN)
                        .build()
        );

        isRunning = false;
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

            //noinspection CheckResult,ResultOfMethodCallIgnored
            miBandManager
                    .readMiBandInfo()
                    .observeOn(Schedulers.io())
                    .flatMap(miBandInfo -> locationDao.insert(new LocationEntity()
                            .setTime(Instant.now().toEpochMilli())
                            .setLocation(location)
                            .setBattery(Utils.readChargeLevel(getApplicationContext()))
                            .setMiBattery(miBandInfo.getBattery())
                            .setMiSteps(miBandInfo.getSteps())
                            .setMiHeart(miBandInfo.getHeartRate())
                    ))
                    .flatMapMaybe(l -> transmitService.checkAndTransmitLocations())
//                    .doOnComplete()
                    .subscribe(saveEntity -> Timber.d("Location saved successfully"),
                            e -> Timber.e(e, "Error saving location"));
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