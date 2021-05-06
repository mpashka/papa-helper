package org.mpashka.findme.db.io;

import org.mpashka.findme.db.LocationEntity;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface SaveApi {
    @POST("/locations")
    Observable<Response<Void>> save(@Body SaveEntity save);
}
