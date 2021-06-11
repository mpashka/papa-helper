package org.mpashka.findme.db;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.mpashka.findme.MyPreferences;
import org.mpashka.findme.R;
import org.mpashka.findme.db.io.SaveApi;
import org.mpashka.findme.db.io.SaveEntity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;

public class MyTransmitService {

    private LocationDao locationDao;
    private AccelerometerDao accelerometerDao;

    private MyPreferences preferences;

    private ConnectivityManager connectivityManager;

    private SaveApi saveApi;

    private Instant nextCheck;

    public MyTransmitService(LocationDao locationDao, AccelerometerDao accelerometerDao,
                             MyPreferences preferences, ConnectivityManager connectivityManager)
    {
        this.locationDao = locationDao;
        this.accelerometerDao = accelerometerDao;
        this.preferences = preferences;
        this.connectivityManager = connectivityManager;
        createApi();
    }

    public void createApi() {
        Timber.d("Create save API");
        saveApi = createRetrofitClient()
                .create(SaveApi.class);
    }

    public Maybe<SaveEntity> checkAndTransmitLocations() {
        Timber.d("transmitLocations()");
        Instant now = Instant.now();
        if (nextCheck != null && nextCheck.isBefore(now)) {
            Timber.d("not time to transmit yet");
            return Maybe.empty();
        }

        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        if (!isConnected) {
            Timber.d("not connected!");
            return Maybe.empty();
        }
        int sendSec = preferences.getInt(R.string.send_interval_id, R.integer.send_interval_default);
        nextCheck = now.plus(sendSec, ChronoUnit.SECONDS);
        return transmitLocations();
    }

    public Maybe<SaveEntity> transmitLocations() {
        //noinspection ResultOfMethodCallIgnored,CheckResult
        return Single.just(new SaveEntity())
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
                .doOnError(e -> Timber.e(e, "Save error"))
                .doOnSuccess(saveEntity -> Timber.d("Saved successfully"))
/*
                .subscribe(
                        saveEntity -> Timber.d("Saved successfully"),
                        e -> Timber.e(e, "Save error"),
                        () -> Timber.d("Save finally (no items to save)")
                );
*/
        ;
    }

    private Retrofit createRetrofitClient() {
        Timber.d("Reload retrofit");
        OkHttpClient.Builder client = new OkHttpClient.Builder();
        if (preferences.getBoolean(R.string.send_debug_http_id, R.bool.send_debug_http_default)) {
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            client.addInterceptor(interceptor);
        }

        RxJava2CallAdapterFactory rxAdapter =
                RxJava2CallAdapterFactory
                        .createWithScheduler(Schedulers.io());
        // Schedulers.io()
/*
          .addInterceptor(chain -> {
            Request newRequest =
                    chain.request().newBuilder()
                            .addHeader("Accept",
                                    "application/json,text/plain,* / *") <--
                            .addHeader("Content-Type",
                                    "application/json;odata.metadata=minimal")
                            .addHeader("Authorization", mToken)
                            .build();

            return chain.proceed(newRequest);
        });

        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                .create();

.addConverterFactory(GsonConverterFactory.create(gson))
*/

        String url = preferences.getString(R.string.send_url_id, R.string.send_url_default);
        return new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(rxAdapter)
                .client(client.build())
                .build();
    }

}
