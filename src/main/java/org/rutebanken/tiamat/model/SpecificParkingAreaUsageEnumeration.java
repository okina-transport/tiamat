package org.rutebanken.tiamat.model;

public enum SpecificParkingAreaUsageEnumeration {

    NONE("aucun"),
    CARPOOL("covoiturage"),
    CARSHARE("autopartage"),
    PARD_AND_RIDE("parkAndRide"),
    DISABLED("handicap");
    private final String value;

    SpecificParkingAreaUsageEnumeration(String v) {
        value = v;
    }

    public static SpecificParkingAreaUsageEnumeration fromValue(String v) {
        for (SpecificParkingAreaUsageEnumeration c : SpecificParkingAreaUsageEnumeration.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

    public String value() { return value; }
}
