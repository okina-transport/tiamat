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
 * <p>Java class for LuggageLockerFacilityEnumeration.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="LuggageLockerFacilityEnumeration">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}NMTOKEN">
 *     &lt;enumeration value="other"/>
 *     &lt;enumeration value="lockers"/>
 *     &lt;enumeration value="oversizeLockers"/>
 *     &lt;enumeration value="leftLuggageCounter"/>
 *     &lt;enumeration value="bikeRack"/>
 *     &lt;enumeration value="cloakroom"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "LuggageLockerFacilityEnumeration")
public enum LuggageLockerFacilityEnumeration {

    OTHER("other"),

    /**
     * pti23_17
     * 
     */
    LOCKERS("lockers"),
    OVERSIZE_LOCKERS("oversizeLockers"),
    LEFT_LUGGAGE_COUNTER("leftLuggageCounter"),
    BIKE_RACK("bikeRack"),
    CLOAKROOM("cloakroom");
    private final String value;

    LuggageLockerFacilityEnumeration(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static LuggageLockerFacilityEnumeration fromValue(String v) {
        for (LuggageLockerFacilityEnumeration c: LuggageLockerFacilityEnumeration.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
