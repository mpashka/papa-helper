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
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * see server.MyResource
 */
public interface SaveApi {
    @POST("/findme/save")
    Completable save(@Body SaveEntity save);

    @POST("/findme/location/add")
    Completable locationAdd(LocationEntity location);

    @GET("/findme/location/get")
    Single<List<LocationEntity>> locationGet(@Query("start") long start, @Query("stop") long stop);
}
