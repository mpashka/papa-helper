package org.mpashka.findme.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;

import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationResult;

import org.mpashka.findme.Utils;
import org.mpashka.findme.db.LocationEntity;
import org.mpashka.findme.miband.MiBandManager;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

@AndroidEntryPoint
public class MyLocationListener extends BroadcastReceiver {
    public static final String NORMAL_LOCATION_UPDATE = "org.mpashka.findme.action.SERVICE_LOCATION_UPDATE";
    public static final String FUSE_LOCATION_UPDATE = "org.mpashka.findme.action.FUSED_LOCATION_UPDATE";
    public static final String FUSE_CURRENT_LOCATION_UPDATE = "org.mpashka.findme.action.FUSED_CURRENT_LOCATION_UPDATE";
    public static final String ACTIVITY_UPDATE = "org.mpashka.findme.action.ACTIVITY_UPDATE";

    public static final String PARCEL_LOCATION = "org.mpashka.findme.parcel.LOCATION";

    private static final String PROVIDER_NORMAL = "loc";
    private static final String PROVIDER_FUSE = "fuse";
    private static final String PROVIDER_FUSE_CURRENT = "fuse_curr";
    private static final MiBandManager.MiBandInfo MIBAND_EMPTY = new MiBandManager.MiBandInfo();


    @Inject
    MyState state;

    @Inject
    MiBandManager miBandManager;

    @Inject
    MyAccelerometerService accelerometerService;

    @Inject
    MyTransmitService transmitService;

    @Inject
    MyWorkManager workManager;

/*
    public MyLocationListener(MyState state, MiBandManager miBandManager, MyAccelerometerService accelerometerService, MyTransmitService transmitService, MyWorkManager workManager) {
        this.state = state;
        this.miBandManager = miBandManager;
        this.accelerometerService = accelerometerService;
        this.transmitService = transmitService;
        this.workManager = workManager;
    }
*/

    @Override
    public void onReceive(Context context, Intent intent) {
        Timber.i("onReceive %s", intent);
        Bundle extras = intent.getExtras();
        if (extras == null) {
            Timber.i("    Intent has no extra");
            return;
        }

        switch (intent.getAction()) {
            case FUSE_LOCATION_UPDATE:
                onFusedLocation(context, intent);
                break;
            case FUSE_CURRENT_LOCATION_UPDATE:
                Location location = intent.getParcelableExtra(PARCEL_LOCATION);
                Timber.i("FUSED current Location %s [%s] -> %s/%s/%s", location, location.getProvider(), location.getAccuracy(), location.getLatitude(), location.getLongitude());
                insertLocation(context, PROVIDER_FUSE_CURRENT, location);
                break;
            case NORMAL_LOCATION_UPDATE:
                onLocation(context, extras);
                break;
            case ACTIVITY_UPDATE:
                onActivityTransition(intent);
                break;
        }
    }

    private void onFusedLocation(Context context, Intent intent) {
        // Checks for location availability changes.
        LocationAvailability locationAvailability = LocationAvailability.extractLocationAvailability(intent);
        if (locationAvailability == null || !locationAvailability.isLocationAvailable()) {
            Timber.i("Location services are no longer available %s!", locationAvailability);
//                return;
        }

        LocationResult locationResult = LocationResult.extractResult(intent);
        if (locationResult == null) {
            Timber.i("Location result is null!");
            return;
        }
        for (Location location : locationResult.getLocations()) {
            Timber.i("FUSED Location %s [%s] -> %s/%s/%s", location, location.getProvider(), location.getAccuracy(), location.getLatitude(), location.getLongitude());
            insertLocation(context, PROVIDER_FUSE, location);
        }
    }

    private void onLocation(Context context, Bundle extras) {
        Location location = (Location) extras.get(LocationManager.KEY_LOCATION_CHANGED);
        if (location != null) {
            Timber.i("Location %s [%s] -> %s/%s/%s", location, location.getProvider(), location.getAccuracy(), location.getLatitude(), location.getLongitude());
            insertLocation(context, PROVIDER_NORMAL, location);
        }
    }

    private void insertLocation(Context context, String provider, Location location) {
/*
        MyWorker.exec.execute(() -> MyDatabase
                .getInstance(context)
                .locationDao()
                .insert(new EntityLocation(provider, location)));
*/

        Timber.d("onLocationChanged %s", location);
        LocationEntity locationEntity = new LocationEntity()
                .setWorkProvider(provider)
                .setLocation(location)
                .setActivity(state.onCreateAndGetActivity())
                .setWorkTime(state.getLastCreateTime())
                .setBattery(Utils.readChargeLevel(context));

        Disposable subscribe = Single.just(locationEntity)
                .observeOn(Schedulers.io())
                .flatMap(l ->
                        miBandManager
                                .readMiBandInfo()
                                .onErrorReturnItem(MIBAND_EMPTY)
                                .observeOn(Schedulers.io())
                                .map(miBandInfo -> locationEntity
                                        .setMiBattery(miBandInfo.getBattery())
                                        .setMiSteps(miBandInfo.getSteps())
                                        .setMiHeart(miBandInfo.getHeartRate())
                                )
                )
                .compose(accelerometerService.fetchSensors())
                .flatMap(l -> transmitService.checkAndTransmitLocations(l))
                .subscribe(saveEntity -> Timber.d("Location saved successfully"),
                        e -> Timber.e(e, "Error saving location"));
    }

    private void onActivityTransition(Intent intent) {
        if (!ActivityTransitionResult.hasResult(intent)) {
            Timber.i("Activity update has no result!");
        }
        ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);
        if (result != null) {
            for (ActivityTransitionEvent event : result.getTransitionEvents()) {
                Timber.i("Activity transition: %s %s/%s[%s]", event, event.getActivityType(), event.getTransitionType(), event.getElapsedRealTimeNanos());
                if (event.getTransitionType() == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                    state.addActivity(1 << event.getActivityType());
                }
            }
        }
    }

}
