package org.mpashka.findme.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

// , indices = {@Index(value = {"saved"}, name = "saved_index")}
@Entity(tableName = "accelerometer")
public class AccelerometerEntity {

    @PrimaryKey
    @ColumnInfo(name = "time")
    @SerializedName("time")
    @Expose
    public long time;


    @ColumnInfo(name = "avg")
    @SerializedName("avg")
    @Expose
    public double average;

    @ColumnInfo(name = "max")
    @SerializedName("max")
    @Expose
    public double maximum;

    @ColumnInfo(name = "battery")
    @SerializedName("battery")
    @Expose
    public int battery;

    @ColumnInfo(name = "saved", index = true)
    public boolean saved;


    public AccelerometerEntity setTime(long time) {
        this.time = time;
        return this;
    }

    public AccelerometerEntity setAverage(double average) {
        this.average = average;
        return this;
    }

    public AccelerometerEntity setMaximum(double maximum) {
        this.maximum = maximum;
        return this;
    }

    public AccelerometerEntity setBattery(int battery) {
        this.battery = battery;
        return this;
    }
}
