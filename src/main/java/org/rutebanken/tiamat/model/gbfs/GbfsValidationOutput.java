package org.rutebanken.tiamat.model.gbfs;

import java.util.ArrayList;
import java.util.List;

public class GbfsValidationOutput {
    private List<String> errors = new ArrayList<>(3);

    private List<StationInformation> stations;

    private List<VehicleType> vehicleTypes;

    private SystemInformation systemInformation;

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public List<StationInformation> getStations() {
        return stations;
    }

    public void setStations(List<StationInformation> stations) {
        this.stations = stations;
    }

    public List<VehicleType> getVehicleTypes() {
        return vehicleTypes;
    }

    public void setVehicleTypes(List<VehicleType> vehicleTypes) {
        this.vehicleTypes = vehicleTypes;
    }

    public SystemInformation getSystemInformation() {
        return systemInformation;
    }

    public void setSystemInformation(SystemInformation systemInformation) {
        this.systemInformation = systemInformation;
    }

    public void updateValidationState(GbfsValidationOutput validation) {
        this.errors.addAll(validation.getErrors());
        if (validation.getStations() != null) {
            this.stations = validation.getStations();
        }
        if (validation.getVehicleTypes() != null) {
            this.vehicleTypes = validation.getVehicleTypes();
        }
        if (validation.getSystemInformation() != null) {
            this.systemInformation = validation.getSystemInformation();
        }
    }
}
