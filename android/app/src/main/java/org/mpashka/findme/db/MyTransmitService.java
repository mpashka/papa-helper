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
import io.reactivex.Single;
import retrofit2.Response;
import timber.log.Timber;

@Singleton
public class MyTransmitService {

    private MyDb db;

    private MyPreferences preferences;

    private ConnectivityManager connectivityManager;

    private SaveApi saveApi;

    private Instant nextCheck;

    @Inject
    public MyTransmitService(MyDb db, MyPreferences preferences, ConnectivityManager connectivityManager, SaveApi saveApi) {
        this.db = db;
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

        LocationDao locationDao = db.locationDao();
        //noinspection ResultOfMethodCallIgnored,CheckResult
        locationDao.loadUnsaved()
//                .doOnError(e -> Timber.e(e, "Database read error"))
                .flatMapSingle(u -> {
                    Timber.d("Send request %s", u.size());
                    SaveEntity saveEntity = new SaveEntity();
                    saveEntity.setLocations(u);
                    return Single.fromObservable(
                            saveApi.save(saveEntity)
                            .map(r -> new SaveResult(r, saveEntity)));

                })
//                .doOnError(e -> Timber.e(e, "Send error"))
                .flatMapSingle(r -> {
                    Response<Void> response = r.getResponse();
                    Timber.d("Send response %s", response);
                    if (response.isSuccessful()) {
                        List<LocationEntity> locations = r.getSaveEntity().getLocations();
                        locations.forEach(l -> l.saved = true);
                        return locationDao.setSaved(r.getSaveEntity()
                                .getLocations()
                                .stream()
                                .map(l -> l.time)
                                .collect(Collectors.toList()));
                    } else {
                        Timber.w("Can't send response: %s", response);
                        return Single.never();
                    }
//                    db.close();
                })
                .subscribe(
                        n -> {},
                        e -> Timber.e(e, "Save error"),
                        () -> {
                            Timber.d("Save finally");
//                    db.close();
                        }
                );
    }


    static class SaveResult {
        private Response<Void> response;
        private SaveEntity saveEntity;

        public SaveResult(Response<Void> response, SaveEntity saveEntity) {
            this.response = response;
            this.saveEntity = saveEntity;
        }

        public Response<Void> getResponse() {
            return response;
        }

        public SaveEntity getSaveEntity() {
            return saveEntity;
        }
    }
}
