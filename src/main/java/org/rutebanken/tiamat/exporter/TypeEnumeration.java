package org.rutebanken.tiamat.exporter;

public enum TypeEnumeration {

    STOP_PLACE("stop place"),
    POI("point of interest"),
    PARKING("parking"),
    IMPORT_PARKING("import_parking"),
    IMPORT_POI("import_poi");

    private final String value;

    TypeEnumeration(String v) {
        value = v;
    }

    public static TypeEnumeration fromValue(String v) {
        for (TypeEnumeration c : TypeEnumeration.values()) {
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
