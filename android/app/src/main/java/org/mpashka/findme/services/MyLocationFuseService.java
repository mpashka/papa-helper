package org.mpashka.findme.services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.CancellationTokenSource;

import org.mpashka.findme.MyPreferences;
import org.mpashka.findme.R;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import dagger.hilt.android.qualifiers.ApplicationContext;
import timber.log.Timber;

@AndroidEntryPoint
public class MyLocationFuseService implements MyListenableServiceInterface {

    private static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    @ApplicationContext
    Context context;

    @Inject
    MyPreferences preferences;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest = LocationRequest.create();
    private PendingIntent locationPendingIntent;

    public MyLocationFuseService(@ApplicationContext Context context, MyPreferences preferences) {
        this.context = context;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        this.preferences = preferences;

        Intent intent = new Intent(context, MyLocationListener.class)
                .setAction(MyLocationListener.NORMAL_LOCATION_UPDATE);
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
                .addOnCompleteListener(l -> {
                    Timber.i("Current location received %s", l);
                    Intent intent = new Intent(context, MyLocationListener.class)
                            .setAction(MyLocationListener.FUSE_CURRENT_LOCATION_UPDATE)
                            .putExtra(MyLocationListener.PARCEL_LOCATION, l.getResult());
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                });
    }

    private int getPriority() {
        return preferences.getInt(R.string.location_fuse_priority_id, R.integer.location_fuse_priority_default);
    }
}
