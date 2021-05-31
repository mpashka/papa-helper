package org.mpashka.findme.db.io;

import org.mpashka.findme.db.LocationEntity;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface SaveApi {
    @POST("/locations")
    Completable save(@Body SaveEntity save);
}
