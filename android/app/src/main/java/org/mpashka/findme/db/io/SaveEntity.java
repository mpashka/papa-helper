package org.mpashka.findme.db.io;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.mpashka.findme.db.AccelerometerEntity;
import org.mpashka.findme.db.LocationEntity;

import java.util.List;

public class SaveEntity {
    @SerializedName("locations")
    @Expose
    private List<LocationEntity> locations;

    @SerializedName("accelerations")
    @Expose
    private List<AccelerometerEntity> accelerations;

    public List<LocationEntity> getLocations() {
        return locations;
    }

    public SaveEntity setLocations(List<LocationEntity> locations) {
        this.locations = locations;
        return this;
    }

    public List<AccelerometerEntity> getAccelerations() {
        return accelerations;
    }

    public SaveEntity setAccelerations(List<AccelerometerEntity> accelerations) {
        this.accelerations = accelerations;
        return this;
    }
}
