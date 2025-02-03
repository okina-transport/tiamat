package org.rutebanken.tiamat.model.gbfs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VehicleType {

    @JsonProperty("vehicle_type_id")
    private String vehicleTypeId;

    @JsonProperty("form_factor")
    private String formFactor;

    @JsonProperty("propulsion_type")
    private String propulsionType;

    @JsonProperty("name")
    private String name;

    @JsonProperty("default_reserve_time")
    private int defaultReserveTime;

    @JsonProperty("return_constraint")
    private String returnConstraint;

    public String getVehicleTypeId() {
        return vehicleTypeId;
    }

    public void setVehicleTypeId(String vehicleTypeId) {
        this.vehicleTypeId = vehicleTypeId;
    }

    public String getFormFactor() {
        return formFactor;
    }

    public void setFormFactor(String formFactor) {
        this.formFactor = formFactor;
    }

    public String getPropulsionType() {
        return propulsionType;
    }

    public void setPropulsionType(String propulsionType) {
        this.propulsionType = propulsionType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDefaultReserveTime() {
        return defaultReserveTime;
    }

    public void setDefaultReserveTime(int defaultReserveTime) {
        this.defaultReserveTime = defaultReserveTime;
    }

    public String getReturnConstraint() {
        return returnConstraint;
    }

    public void setReturnConstraint(String returnConstraint) {
        this.returnConstraint = returnConstraint;
    }
}
