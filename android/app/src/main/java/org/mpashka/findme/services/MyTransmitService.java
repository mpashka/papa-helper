package org.mpashka.findme.services;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.jetbrains.annotations.NotNull;
import org.mpashka.findme.MyPreferences;
import org.mpashka.findme.R;
import org.mpashka.findme.Utils;
import org.mpashka.findme.db.LocationDao;
import org.mpashka.findme.db.LocationEntity;
import org.mpashka.findme.db.io.SaveApi;
import org.mpashka.findme.db.io.SaveEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.AndroidEntryPoint;
import dagger.hilt.android.qualifiers.ApplicationContext;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.SingleSource;
import io.reactivex.SingleTransformer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;

@Singleton
public class MyTransmitService {

    private LocationDao locationDao;
    private MyPreferences preferences;
    private ConnectivityManager connectivityManager;
    private MyState state;
    private SaveApi saveApi;

    @Inject
    public MyTransmitService(@ApplicationContext Context context, LocationDao locationDao, MyPreferences preferences, MyState state) {
        this.locationDao = locationDao;
        this.preferences = preferences;
        this.state = state;
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        createApi();

        locationDao
                .getTotalCount()
//                .observeOn(Schedulers.io())
                .flatMap(c -> locationDao
                        .getPendingCount()
                        .map(u -> new long[]{c, u})
//                        .subscribeOn(Schedulers.io())
                )
                .subscribeOn(Schedulers.io())
                .subscribe(v -> this.state.init(v[0], v[1]));
    }

    public void createApi() {
        Timber.i("Create save API");
        saveApi = createRetrofitClient()
                .create(SaveApi.class);
    }

    public Single<SaveEntity> checkAndTransmitLocations(LocationEntity lastLocation) {
        Timber.i("transmitLocations()");

        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        int sendSec = preferences.getInt(R.string.send_interval_id, R.integer.send_interval_default);
        long now = System.currentTimeMillis();
        if (!isConnected) {
            Timber.i("not connected! Saving to the database");
            return saveToLocal(lastLocation);
        } else if (now < state.getLastTransmitTime() + sendSec * 1000) {
            Timber.i("Not enough time passed! Saving to the database");
            return saveToLocal(lastLocation);
        } else if (state.getPending() == 0) {
            Timber.i("Sending single location");
            SaveEntity saveEntity = new SaveEntity()
                    .setLocations(Collections.singletonList(lastLocation));
            return transmit(lastLocation, saveEntity);
        } else {
            Timber.i("Sending %s pending location + last", state.getPending());
            return Single.just(new SaveEntity())
                    .compose(loadPending(lastLocation))
                    .flatMap(saveEntity -> transmit(lastLocation, saveEntity));
        }
    }

    private SingleTransformer<SaveEntity, SaveEntity> loadPending(LocationEntity lastLocation) {
        return s -> s
                .flatMap(saveEntity -> {
                    Timber.i("Loading pending locations...");
                    return locationDao.loadPending()
                            .map(locations -> {
                                Timber.i("Locations loaded %s", locations.size());
                                for (LocationEntity location : locations) {
                                    location.setSaved(true);
                                }
                                List<LocationEntity> allLocations = new ArrayList<>(locations.size() + 1);
                                allLocations.addAll(locations);
                                if (lastLocation != null) {
                                    allLocations.add(lastLocation);
                                }
                                return saveEntity.setLocations(allLocations);
                            });
                });
    }

    @NotNull
    private Single<SaveEntity> saveToLocal(LocationEntity lastLocation) {
        return locationDao.insert(lastLocation)
                .map(l -> {
                    Timber.i("Saved successfully");
                    lastLocation.setSaved(true);
                    state.addPending();
                    return new SaveEntity().setLocations(Collections.singletonList(lastLocation));
                });
    }

    public Single<SaveEntity> transmitPending() {
        return Single.just(new SaveEntity())
                .compose(loadPending(null))
                .flatMap(s -> transmit(null, s));
    }

    private Single<SaveEntity> transmit(LocationEntity lastLocation, SaveEntity saveEntity) {
        int retry = preferences.getInt(R.string.send_retry_id, R.integer.send_retry_default);
        return saveApi.save(saveEntity)
                .retry(retry)
                .toSingleDefault(saveEntity)
                .flatMap(v -> {
                    Timber.i("Transmitted successfully. Updating db.");
                    state.onTransmit(saveEntity.getLocations().size());
                    List<LocationEntity> locations = saveEntity.getLocations();
                    List<Long> pendingTransmittedIds = new ArrayList<>(locations.size() - 1);
                    for (LocationEntity location : saveEntity.getLocations()) {
                        if (location.isSaved()) {
                            pendingTransmittedIds.add(location.workTime);
                        }
                        location.setTransmitted(true);
                    }
                    return pendingTransmittedIds.size() > 0
                            ? locationDao.setTransmitted(pendingTransmittedIds).map(u -> saveEntity)
                            : Single.just(saveEntity);
                })
                .onErrorResumeNext(e -> {
                    Timber.e(e, "Transmit error. Saving location to the local database");
                    return lastLocation != null ? saveToLocal(lastLocation) : Single.just(new SaveEntity());
                });
    }

    private Retrofit createRetrofitClient() {
        Timber.i("Reload retrofit");
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

