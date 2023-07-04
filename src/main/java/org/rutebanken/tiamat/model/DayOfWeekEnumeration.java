package org.rutebanken.tiamat.model;

public enum DayOfWeekEnumeration {


    MONDAY("Monday"),
    TUESDAY("Tuesday"),
    WEDNESDAY("Wednesday"),
    THURSDAY("Thursday"),
    FRIDAY("Friday"),
    SATURDAY("Saturday"),
    SUNDAY("Sunday"),
    EVERYDAY("Everyday"),
    WEEKDAYS("Weekdays"),
    WEEKEND("Weekend"),
    NONE("none");

    private final String value;

    DayOfWeekEnumeration(String v) {
        value = v;
    }

    public static DayOfWeekEnumeration fromValue(String v) {
        DayOfWeekEnumeration[] var1 = values();
        int var2 = var1.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            DayOfWeekEnumeration c = var1[var3];
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
