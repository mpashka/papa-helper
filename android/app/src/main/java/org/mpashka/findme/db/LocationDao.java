package org.mpashka.findme.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.Collection;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;


@Dao
public interface LocationDao {

    @Query("SELECT * FROM location")
    List<LocationEntity> getAll();

    @Query("SELECT * FROM location WHERE saved = 0")
    Single<List<LocationEntity>> loadUnsaved();

    @Query("UPDATE location SET saved = 1 WHERE time in (:times)")
    Single<Integer> setSaved(Collection<Long> times);

    @Insert
    Single<Long> insert(LocationEntity locationEntity);

    @Query("SELECT COUNT(*) FROM location WHERE SAVED = 0")
    int getUnsavedCount();

    @Query("SELECT COUNT(*) FROM location WHERE SAVED = 1")
    int getSavedCount();

/*
    @Insert
    void insertAll(User... users);

    @Delete
    void delete(User user);
*/
}
