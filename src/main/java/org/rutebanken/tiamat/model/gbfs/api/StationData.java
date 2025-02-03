package org.rutebanken.tiamat.model.gbfs.api;

import org.rutebanken.tiamat.model.gbfs.StationInformation;

import java.util.List;

public class StationData {
    private List<StationInformation> stations;

    public List<StationInformation> getStations() {
        return stations;
    }

    public void setStations(List<StationInformation> stations) {
        this.stations = stations;
    }
}
