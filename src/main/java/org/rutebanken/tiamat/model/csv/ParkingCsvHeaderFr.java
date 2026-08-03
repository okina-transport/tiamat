package org.rutebanken.tiamat.model.csv;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum ParkingCsvHeaderFr {

    ID("id"),
    NAME("nom"),
    INSEE("insee"),
    ADDRESS("adresse"),
    URL("url"),
    USER_TYPE("type_usagers"),
    FREE("gratuit"),
    NB_OF_PLACES("nb_places"),
    NB_OF_PR("nb_pr"),
    DISABLED_PARKING_NB("nb_pmr"),
    ELECTRIC_VEHICLE_NB("nb_voitures_electriques"),
    BIKE_NB("nb_velo"),
    ELECTRIC_BIKES_NB("nb_2r_el"),
    CAR_SHARING_NB("nb_autopartage"),
    MOTORCYCLE_NB("nb_2_rm"),
    CAR_POOLING_NB("nb_covoit"),
    MAX_HEIGHT("hauteur_max"),
    SIRET_NUMBER("num_siret"),
    XLONG("Xlong"),
    YLAT("Ylat"),
    DISABLED_PARKING_PRICE("tarif_pmr"),
    ONE_HOUR_PRICE("tarif_1h"),
    TWO_HOURS_PRICE("tarif_2h"),
    THREE_HOURS_PRICE("tarif_3h"),
    FOUR_HOURS_PRICE("tarif_4h"),
    TWENTY_FOUR_HOURS_PRICE("tarif_24h"),
    RESIDENT_SUBSCRIPTION("abo_resident"),
    NON_RESIDENT_SUBSCRIPTION("abo_non_resident"),
    WORK_TYPE("type_ouvrage"),
    INFO("info"),
    OPERATOR("operator");

    private final String value;

    ParkingCsvHeaderFr(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static List<String> headerNames() {
        return Arrays.stream(values()).map(ParkingCsvHeaderFr::value).collect(Collectors.toList());
    }
}