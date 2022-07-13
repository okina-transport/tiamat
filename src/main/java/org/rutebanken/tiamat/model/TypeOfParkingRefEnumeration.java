package org.rutebanken.tiamat.model;

public enum TypeOfParkingRefEnumeration {

    SecureBikeParking("SecureBikeParking"),
    IndividualBox("IndividualBox");

    private final String value;

    TypeOfParkingRefEnumeration(String v) {
        value = v;
    }

    public static TypeOfParkingRefEnumeration fromValue(String v) {
        for (TypeOfParkingRefEnumeration typeOfParkingRefEnumeration : TypeOfParkingRefEnumeration.values()) {
            if (typeOfParkingRefEnumeration.value.equals(v)) {
                return typeOfParkingRefEnumeration;
            }
        }
        throw new IllegalArgumentException(v);
    }

    public String value() {
        return value;
    }
}
