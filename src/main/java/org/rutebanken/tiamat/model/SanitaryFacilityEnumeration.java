//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.05 at 07:41:01 PM CET 
//


package org.rutebanken.tiamat.model;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for SanitaryFacilityEnumeration.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="SanitaryFacilityEnumeration">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="none"/>
 *     &lt;enumeration value="toilet"/>
 *     &lt;enumeration value="wheelChairAccessToilet"/>
 *     &lt;enumeration value="shower"/>
 *     &lt;enumeration value="washingAndChangeFacilities"/>
 *     &lt;enumeration value="babyChange"/>
 *     &lt;enumeration value="wheelchairBabyChange"/>
 *     &lt;enumeration value="shoeShiner"/>
 *     &lt;enumeration value="other"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "SanitaryFacilityEnumeration")
public enum SanitaryFacilityEnumeration {

    NONE("none"),
    TOILET("toilet"),
    WHEEL_CHAIR_ACCESS_TOILET("wheelChairAccessToilet"),
    SHOWER("shower"),
    WASHING_AND_CHANGE_FACILITIES("washingAndChangeFacilities"),
    BABY_CHANGE("babyChange"),
    WHEELCHAIR_BABY_CHANGE("wheelchairBabyChange"),
    SHOE_SHINER("shoeShiner"),
    OTHER("other");
    private final String value;

    SanitaryFacilityEnumeration(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static SanitaryFacilityEnumeration fromValue(String v) {
        for (SanitaryFacilityEnumeration c: SanitaryFacilityEnumeration.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
