package org.rutebanken.tiamat.model;

public enum FareClassEnumeration {
    
    UNKNOWN("unknown"),
    FIRST_CLASS("firstClass"),
    SECOND_CLASS("secondClass"),
    THIRD_CLASS("thirdClass"),
    PREFERENTE("preferente"),
    PREMIUM_CLASS("premiumClass"),
    BUSINESS_CLASS("businessClass"),
    STANDARD_CLASS("standardClass"),
    TURISTA("turista"),
    ECONOMY_CLASS("economyClass"),
    ANY("any");

    private final String value;

    FareClassEnumeration(String v) {
        this.value = v;
    }

    public static FareClassEnumeration fromValue(String v) {
        for (FareClassEnumeration c : values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }

        throw new IllegalArgumentException(v);
    }

    public String value() {
        return this.value;
    }
}
