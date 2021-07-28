package org.mpashka.findme.server;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class SaveEntity {
    @JsonProperty("locations")
    private List<LocationEntity> locations;

    public List<LocationEntity> getLocations() {
        return locations;
    }

    public SaveEntity setLocations(List<LocationEntity> locations) {
        this.locations = locations;
        return this;
    }
}
