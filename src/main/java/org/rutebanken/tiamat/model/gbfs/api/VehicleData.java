package org.rutebanken.tiamat.model.gbfs.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.rutebanken.tiamat.model.gbfs.VehicleType;

import java.util.List;

public class VehicleData {
    @JsonProperty("vehicle_types")
    private List<VehicleType> vehicleTypes;

    public List<VehicleType> getVehicleTypes() {
        return vehicleTypes;
    }

    public void setVehicleTypes(List<VehicleType> vehicleTypes) {
        this.vehicleTypes = vehicleTypes;
    }
}
