package org.mpashka.findme.db;

import android.annotation.SuppressLint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.mpashka.findme.MyPreferences;
import org.mpashka.findme.R;
import org.mpashka.findme.db.io.SaveApi;
import org.mpashka.findme.db.io.SaveEntity;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import retrofit2.Response;
import timber.log.Timber;

public class MyTransmitService {

    private LocationDao locationDao;
    private AccelerometerDao accelerometerDao;

    private MyPreferences preferences;

    private ConnectivityManager connectivityManager;

    private SaveApi saveApi;

    private Instant nextCheck;

    public MyTransmitService(LocationDao locationDao, AccelerometerDao accelerometerDao,
                             MyPreferences preferences, ConnectivityManager connectivityManager,
                             SaveApi saveApi)
    {
        this.locationDao = locationDao;
        this.accelerometerDao = accelerometerDao;
        this.preferences = preferences;
        this.connectivityManager = connectivityManager;
        this.saveApi = saveApi;
    }

    @Inject
    public void transmitLocations() {
        Timber.d("transmitLocations()");
        Instant now = Instant.now();
        if (nextCheck != null && nextCheck.isBefore(now)) {
            Timber.d("not time to transmit yet");
            return;
        }

        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        if (!isConnected) {
            Timber.d("not connected!");
            return;
        }

        int sendSec = preferences.getInt(R.string.send_id, R.integer.send_default);
        nextCheck = now.plus(sendSec, ChronoUnit.SECONDS);

        //noinspection ResultOfMethodCallIgnored,CheckResult
        Single.just(new SaveEntity())
                .flatMap(saveEntity -> {
                    Timber.d("Loading locations...");
                    return locationDao.loadUnsaved()
                            .map(locations -> {
                                Timber.d("Locations loaded %s", locations.size());
                                return saveEntity.setLocations(locations);
                            });
                })
                .flatMap(saveEntity -> {
                    Timber.d("Loading accelerations...");
                    return accelerometerDao.loadUnsaved()
                            .map(accelerations -> {
                                Timber.d("Accelerations loaded %s", accelerations.size());
                                return saveEntity.setAccelerations(accelerations);
                            });
                })
                .flatMapMaybe(saveEntity -> {
                    Timber.d("Sending request %s / %s", saveEntity.getAccelerations().size(), saveEntity.getLocations().size());
                    if (saveEntity.getAccelerations().isEmpty() && saveEntity.getLocations().isEmpty()) {
                        Timber.d("No data to send");
                        return Maybe.empty();
                    }
                    return saveApi.save(saveEntity)
                            .toSingleDefault(saveEntity)
                            .toMaybe();
                })
                .flatMap(saveEntity -> {
                        List<LocationEntity> locations = saveEntity.getLocations();
                        if (locations.isEmpty()) {
                            return Maybe.just(saveEntity);
                        } else {
                            List<Long> locationIds = locations.stream()
                                    .map(l -> l.time)
                                    .collect(Collectors.toList());
                            return locationDao.setSaved(locationIds)
                                    .map(i -> saveEntity)
                                    .toMaybe();
                        }
                })
                .flatMap(saveEntity -> {
                        List<AccelerometerEntity> accelerations = saveEntity.getAccelerations();
                        if (accelerations.isEmpty()) {
                            return Maybe.just(saveEntity);
                        } else {
                            List<Long> accelerationIds = accelerations.stream()
                                    .map(l -> l.time)
                                    .collect(Collectors.toList());
                            return accelerometerDao.setSaved(accelerationIds)
                                    .map(i -> saveEntity)
                                    .toMaybe();
                        }
                })
                .subscribe(
                        saveEntity -> Timber.d("Saved successfully"),
                        e -> Timber.e(e, "Save error"),
                        () -> Timber.d("Save finally (no items to save)")
                );
    }

    public void setSaveApi(SaveApi saveApi) {
        this.saveApi = saveApi;
    }
}
