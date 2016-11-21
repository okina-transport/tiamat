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
 * <p>Java class for CarServiceFacilityEnumeration.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="CarServiceFacilityEnumeration">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}NMTOKEN">
 *     &lt;enumeration value="unknown"/>
 *     &lt;enumeration value="carWash"/>
 *     &lt;enumeration value="valetPark"/>
 *     &lt;enumeration value="carValetClean"/>
 *     &lt;enumeration value="oilChange"/>
 *     &lt;enumeration value="engineWarming"/>
 *     &lt;enumeration value="petrol"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "CarServiceFacilityEnumeration")
public enum CarServiceFacilityEnumeration {

    UNKNOWN("unknown"),
    CAR_WASH("carWash"),
    VALET_PARK("valetPark"),
    CAR_VALET_CLEAN("carValetClean"),
    OIL_CHANGE("oilChange"),
    ENGINE_WARMING("engineWarming"),
    PETROL("petrol");
    private final String value;

    CarServiceFacilityEnumeration(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static CarServiceFacilityEnumeration fromValue(String v) {
        for (CarServiceFacilityEnumeration c: CarServiceFacilityEnumeration.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
