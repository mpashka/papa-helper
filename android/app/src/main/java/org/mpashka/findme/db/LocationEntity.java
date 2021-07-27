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
    @ColumnInfo(name = "work_time")
    @SerializedName("work_time")
    @Expose
    public long workTime;

    @ColumnInfo(name = "time")
    @SerializedName("time")
    @Expose
    public long time;

    @ColumnInfo(name = "work_provider")
    @SerializedName("work_provider")
    @Expose
    public String workProvider;

    @ColumnInfo(name = "provider")
    @SerializedName("provider")
    @Expose
    public String provider;

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

    @ColumnInfo(name = "accel_avg")
    @SerializedName("accel_avg")
    @Expose
    public double accelerometerAverage;

    @ColumnInfo(name = "accel_max")
    @SerializedName("accel_max")
    @Expose
    public double accelerometerMaximum;

    @ColumnInfo(name = "accel_count")
    @SerializedName("accel_count")
    @Expose
    public int accelerometerCount;

    @ColumnInfo(name = "activity")
    @SerializedName("activity")
    @Expose
    public int activity;

    @ColumnInfo(name = "transmitted", index = true)
    public boolean transmitted;

    public boolean saved;

    public LocationEntity setWorkTime(long workTime) {
        this.workTime = workTime;
        return this;
    }

    public LocationEntity setWorkProvider(String workProvider) {
        this.workProvider = workProvider;
        return this;
    }

    public LocationEntity setLocation(Location location) {
        this.latitude = location.getLatitude();
        this.longitude = location.getLongitude();
        this.accuracy = location.getAccuracy();
        this.provider = location.getProvider();
        this.time = location.getTime();
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

    public LocationEntity setAccelerometer(int count, double avg, double max) {
        this.accelerometerCount = count;
        this.accelerometerAverage = avg;
        this.accelerometerMaximum = max;
        return this;
    }

    public LocationEntity setActivity(int activity) {
        this.activity = activity;
        return this;
    }

    public boolean isTransmitted() {
        return transmitted;
    }

    public LocationEntity setTransmitted(boolean transmitted) {
        this.transmitted = transmitted;
        return this;
    }

    public boolean isSaved() {
        return saved;
    }

    public LocationEntity setSaved(boolean saved) {
        this.saved = saved;
        return this;
    }
}
