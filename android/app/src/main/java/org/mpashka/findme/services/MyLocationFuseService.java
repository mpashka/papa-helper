package org.mpashka.findme.services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.CancellationTokenSource;

import org.mpashka.findme.MyPreferences;
import org.mpashka.findme.R;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import timber.log.Timber;

@Singleton
public class MyLocationFuseService implements MyListenableServiceInterface {

    private static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private Context context;
    private MyPreferences preferences;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest = LocationRequest.create();
    private PendingIntent locationPendingIntent;

    @Inject
    public MyLocationFuseService(@ApplicationContext Context context, MyPreferences preferences) {
        this.context = context;
        this.preferences = preferences;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

        Intent intent = new Intent(context, MyLocationListener.class)
                .setAction(MyLocationListener.FUSED_LOCATION_UPDATE);
        locationPendingIntent = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public String[] getPermissions() {
        return PERMISSIONS;
    }

    @Override
    @SuppressLint("MissingPermission")
    public void startListen() {
        locationRequest.setWaitForAccurateLocation(true)
                .setInterval(preferences.getInt(R.string.location_fuse_time_id, R.integer.location_fuse_time_default) * 60 * 1000)
                .setFastestInterval(preferences.getInt(R.string.location_fuse_min_time_id, R.integer.location_fuse_min_time_default) * 1000)
                .setPriority(getPriority())
                .setSmallestDisplacement(preferences.getFloat(R.string.location_fuse_min_distance_id, R.dimen.location_fuse_min_distance_default));

        fusedLocationClient.removeLocationUpdates(locationPendingIntent);
        fusedLocationClient.requestLocationUpdates(locationRequest, locationPendingIntent);
    }

    @Override
    public void stopListen() {
        fusedLocationClient.removeLocationUpdates(locationPendingIntent);
    }

    @SuppressLint("MissingPermission")
    public void fetchCurrentLocation() {
        CancellationToken cancellationToken = new CancellationTokenSource().getToken();
        fusedLocationClient.getCurrentLocation(getPriority(), cancellationToken)
                .addOnCompleteListener(t -> {
                    Location location = t.getResult();
                    Timber.i("Current location received %s", t);
                    Intent intent = new Intent(/*context, MyLocationListener.class*/)
                            .setAction(MyLocationListener.FUSED_CURRENT_LOCATION_UPDATE)
                            .putExtra(MyLocationListener.PARCEL_LOCATION, location);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                });
    }

    private int getPriority() {
        return preferences.getInt(R.string.location_fuse_priority_id, R.integer.location_fuse_priority_default);
    }
}
