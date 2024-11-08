package org.rutebanken.tiamat.model;

public enum TypeOfWorkEnumeration {

    OTHER("autre"),
    SILO_ONLY("silo seul"),
    UNDERGROUND_ONLY("souterrain seul"),
    SURFACE_ENCLOSED("enclos_en_surface"),
    WORK("ouvrage");
    private final String value;

    TypeOfWorkEnumeration(String v) {
        value = v;
    }

    public static TypeOfWorkEnumeration fromValue(String v) {
        for (TypeOfWorkEnumeration c : TypeOfWorkEnumeration.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

    public String value() { return value; }
}
