package org.rutebanken.tiamat.model.gbfs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StationInformation {
    @JsonProperty("station_id")
    private String stationId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("short_name")
    private String shortName;

    @JsonProperty("lat")
    private double latitude;

    @JsonProperty("lon")
    private double longitude;

    @JsonProperty("address")
    private String address;

    @JsonProperty("cross_street")
    private String crossStreet;

    @JsonProperty("post_code")
    private int postalCode;

    @JsonProperty("rental_methods")
    private List<String> rentalMethods;

    @JsonProperty("parking_type")
    private String parkingType;

    @JsonProperty("capacity")
    private int capacity;

    @JsonProperty("vehicle_capacity")
    private Map<String, Integer> vehicleCapacity;

    @JsonProperty("is_charging_station")
    private Boolean isChargingStation;

    @JsonProperty("rental_uri")
    private RentalUri rentalUri;

    public String getStationId() {
        return stationId;
    }

    public void setStationId(String stationId) {
        this.stationId = stationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCrossStreet() {
        return crossStreet;
    }

    public void setCrossStreet(String crossStreet) {
        this.crossStreet = crossStreet;
    }

    public int getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(int postalCode) {
        this.postalCode = postalCode;
    }

    public List<String> getRentalMethods() {
        return rentalMethods;
    }

    public void setRentalMethods(List<String> rentalMethods) {
        this.rentalMethods = rentalMethods;
    }

    public String getParkingType() {
        return parkingType;
    }

    public void setParkingType(String parkingType) {
        this.parkingType = parkingType;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public Map<String, Integer> getVehicleCapacity() {
        return vehicleCapacity;
    }

    public void setVehicleCapacity(Map<String, Integer> vehicleCapacity) {
        this.vehicleCapacity = vehicleCapacity;
    }

    public Boolean getChargingStation() {
        return isChargingStation;
    }

    public void setChargingStation(Boolean chargingStation) {
        isChargingStation = chargingStation;
    }

    public RentalUri getRentalUri() {
        return rentalUri;
    }

    public void setRentalUri(RentalUri rentalUri) {
        this.rentalUri = rentalUri;
    }
}
