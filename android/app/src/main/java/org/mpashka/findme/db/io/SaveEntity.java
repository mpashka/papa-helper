package org.mpashka.findme.db.io;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.mpashka.findme.db.LocationEntity;

import java.util.List;

public class SaveEntity {
    @SerializedName("locations")
    @Expose
    private List<LocationEntity> locations;


    public List<LocationEntity> getLocations() {
        return locations;
    }

    public void setLocations(List<LocationEntity> locations) {
        this.locations = locations;
    }
}
