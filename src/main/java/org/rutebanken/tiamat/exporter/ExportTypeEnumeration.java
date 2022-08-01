package org.rutebanken.tiamat.exporter;

import org.rutebanken.tiamat.model.AccessSpaceTypeEnumeration;

public enum ExportTypeEnumeration {

    STOP_PLACE("stop place"),
    POI("point of interest"),
    PARKING("parking");
    private final String value;

    ExportTypeEnumeration(String v) {
        value = v;
    }

    public static ExportTypeEnumeration fromValue(String v) {
        for (ExportTypeEnumeration c : ExportTypeEnumeration.values()) {
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
