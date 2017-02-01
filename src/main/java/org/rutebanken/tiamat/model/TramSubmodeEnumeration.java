package org.rutebanken.tiamat.model;

public enum TramSubmodeEnumeration {

    UNKNOWN("unknown"),
    UNDEFINED("undefined"),
    CITY_TRAM("cityTram"),
    LOCAL_TRAM("localTram"),
    REGIONAL_TRAM("regionalTram"),
    SIGHTSEEING_TRAM("sightseeingTram"),
    SHUTTLE_TRAM("shuttleTram"),
    TRAIN_TRAM("trainTram");
    private final String value;

    TramSubmodeEnumeration(String v) {
        value = v;
    }

    public static TramSubmodeEnumeration fromValue(String v) {
        for (TramSubmodeEnumeration c : TramSubmodeEnumeration.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

    public String value() {
        return value;
    }

}
