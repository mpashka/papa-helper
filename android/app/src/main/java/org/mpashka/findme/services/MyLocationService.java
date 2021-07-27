package org.mpashka.findme.services;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;

import org.mpashka.findme.MyPreferences;
import org.mpashka.findme.R;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import dagger.hilt.android.qualifiers.ApplicationContext;
import timber.log.Timber;

@AndroidEntryPoint
public class MyLocationService implements MyListenableServiceInterface {

    private static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    @Inject
    MyPreferences preferences;

    private LocationManager locationManager;

    private PendingIntent locationPendingIntent;

    public MyLocationService(@ApplicationContext Context context, MyPreferences preferences) {
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.preferences = preferences;

        Intent intent = new Intent(context, MyLocationListener.class)
                .setAction(MyLocationListener.FUSE_LOCATION_UPDATE);
        locationPendingIntent = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public String[] getPermissions() {
        return PERMISSIONS;
    }

    @Override
    public void startListen() {
        locationManager.removeUpdates(locationPendingIntent);
        String locationProvider = preferences.getString(R.string.location_gen_provider_id, R.string.location_gen_provider_default);
        int timeSec = preferences.getInt(R.string.location_gen_time_id, R.integer.location_gen_time_default);
        float minDistance = preferences.getFloat(R.string.location_gen_min_distance_id, R.dimen.location_gen_min_distance_default);
        try {
            locationManager.requestLocationUpdates(locationProvider, 1000 * timeSec, minDistance, locationPendingIntent);
        } catch (SecurityException e) {
            Timber.e(e, "Not enough permissions");
        }
    }

    @Override
    public void stopListen() {
        locationManager.removeUpdates(locationPendingIntent);
    }
}
