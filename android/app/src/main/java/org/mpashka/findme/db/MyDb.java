package org.mpashka.findme.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {LocationEntity.class, AccelerometerEntity.class}, version = 1, exportSchema = true)
public abstract class MyDb extends RoomDatabase {
    public abstract LocationDao locationDao();
    public abstract AccelerometerDao accelerometerDao();
}
