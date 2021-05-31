package org.mpashka.findme.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.Collection;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;


@Dao
public interface AccelerometerDao {

    @Query("SELECT * FROM accelerometer")
    List<AccelerometerEntity> getAll();

    @Query("SELECT * FROM accelerometer WHERE saved = 0")
    Single<List<AccelerometerEntity>> loadUnsaved();

    @Query("UPDATE accelerometer SET saved = 1 WHERE time in (:times)")
    Single<Integer> setSaved(Collection<Long> times);

    @Insert
    Completable insert(AccelerometerEntity accelerometerEntity);

    @Query("SELECT COUNT(*) FROM accelerometer WHERE SAVED = 0")
    int getUnsavedCount();

    @Query("SELECT COUNT(*) FROM accelerometer WHERE SAVED = 1")
    int getSavedCount();

/*
    @Insert
    void insertAll(User... users);

    @Delete
    void delete(User user);
*/
}
