package org.mpashka.findme.db;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Update;

import java.util.Collection;
import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Single;


@Dao
interface LocationDao {

    @Query("SELECT * FROM location")
    List<LocationEntity> getAll();

    @Query("SELECT * FROM location WHERE saved = 0")
    Flowable<List<LocationEntity>> loadUnsaved();

    @Query("UPDATE location SET saved = 1 WHERE time in (:times)")
    Single<Integer> setSaved(Collection<Long> times);

/*
    @Insert
    void insertAll(User... users);

    @Delete
    void delete(User user);
*/
}
