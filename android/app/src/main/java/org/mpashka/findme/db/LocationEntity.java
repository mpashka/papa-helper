package org.mpashka.findme.db;

import android.location.Location;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Entity(tableName = "location")
public class LocationEntity {
    @PrimaryKey
    @ColumnInfo(name = "time")
    @SerializedName("time")
    @Expose
    public long time;

    @ColumnInfo(name = "lat")
    @SerializedName("lat")
    @Expose
    public double latitude;

    @ColumnInfo(name = "long")
    @SerializedName("long")
    @Expose
    public double longitude;

    @ColumnInfo(name = "accuracy")
    @SerializedName("accuracy")
    @Expose
    public float accuracy;

    @ColumnInfo(name = "battery")
    @SerializedName("battery")
    @Expose
    public int battery;

    @ColumnInfo(name = "mi_battery")
    @SerializedName("mi_battery")
    @Expose
    public int miBattery;

    @ColumnInfo(name = "mi_steps")
    @SerializedName("mi_steps")
    @Expose
    public int miSteps;

    @ColumnInfo(name = "mi_heart")
    @SerializedName("mi_heart")
    @Expose
    public int miHeart;

    @ColumnInfo(name = "saved", index = true)
    public boolean saved;

    public LocationEntity setTime(long time) {
        this.time = time;
        return this;
    }

    public LocationEntity setLocation(Location location) {
        this.latitude = location.getLatitude();
        this.longitude = location.getLongitude();
        this.accuracy = location.getAccuracy();
        return this;
    }

    public LocationEntity setLatitude(double latitude) {
        this.latitude = latitude;
        return this;
    }

    public LocationEntity setLongitude(double longitude) {
        this.longitude = longitude;
        return this;
    }

    public LocationEntity setAccuracy(float accuracy) {
        this.accuracy = accuracy;
        return this;
    }

    public LocationEntity setBattery(int battery) {
        this.battery = battery;
        return this;
    }

    public LocationEntity setMiBattery(int miBattery) {
        this.miBattery = miBattery;
        return this;
    }

    public LocationEntity setMiSteps(int miSteps) {
        this.miSteps = miSteps;
        return this;
    }

    public LocationEntity setMiHeart(int miHeart) {
        this.miHeart = miHeart;
        return this;
    }

    public LocationEntity setSaved(boolean saved) {
        this.saved = saved;
        return this;
    }
}
