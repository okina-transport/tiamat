package org.rutebanken.tiamat.model.csv;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum ParkingCsvHeader {

    ID("id"),
    NAME("name"),
    INSEE("insee"),
    ADDRESS("address"),
    URL("url"),
    USER_TYPE("userType"),
    FREE("free"),
    NB_OF_PLACES("nbOfPlaces"),
    NB_OF_PR("nbOfPr"),
    DISABLED_PARKING_NB("disabledParkingNb"),
    ELECTRIC_VEHICLE_NB("electricVehicleNb"),
    BIKE_NB("bikeNb"),
    ELECTRIC_BIKES_NB("electricBikesNb"),
    CAR_SHARING_NB("carSharingNb"),
    MOTORCYCLE_NB("motorcycleNb"),
    CAR_POOLING_NB("carPoolingNb"),
    MAX_HEIGHT("maxHeight"),
    SIRET_NUMBER("siretNumber"),
    XLONG("Xlong"),
    YLAT("Ylat"),
    DISABLED_PARKING_PRICE("disabledParkingPrice"),
    ONE_HOUR_PRICE("oneHourPrice"),
    TWO_HOURS_PRICE("twoHoursPrice"),
    THREE_HOURS_PRICE("threeHoursPrice"),
    FOUR_HOURS_PRICE("fourHoursPrice"),
    TWENTY_FOUR_HOURS_PRICE("twentyFourHoursPrice"),
    RESIDENT_SUBSCRIPTION("residentSubscription"),
    NON_RESIDENT_SUBSCRIPTION("nonResidentSubscription"),
    WORK_TYPE("workType"),
    INFO("info"),
    OPERATOR("operator");

    private final String value;

    ParkingCsvHeader(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static List<String> headerNames() {
        return Arrays.stream(values()).map(ParkingCsvHeader::value).collect(Collectors.toList());
    }
}