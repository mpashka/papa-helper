package org.mpashka.findme.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.mpashka.findme.Utils;

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

    @ColumnInfo(name = "saved")
    public boolean saved;


}
