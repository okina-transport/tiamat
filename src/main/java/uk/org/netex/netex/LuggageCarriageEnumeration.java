//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.05 at 07:41:01 PM CET 
//


package uk.org.netex.netex;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for LuggageCarriageEnumeration.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="LuggageCarriageEnumeration">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}NMTOKEN">
 *     &lt;enumeration value="unknown"/>
 *     &lt;enumeration value="noBaggageStorage"/>
 *     &lt;enumeration value="baggageStorage"/>
 *     &lt;enumeration value="luggageRacks"/>
 *     &lt;enumeration value="extraLargeLuggageRacks"/>
 *     &lt;enumeration value="baggageVan"/>
 *     &lt;enumeration value="noCycles"/>
 *     &lt;enumeration value="cyclesAllowed"/>
 *     &lt;enumeration value="cyclesAllowedInVan"/>
 *     &lt;enumeration value="cyclesAllowedInCarriage"/>
 *     &lt;enumeration value="cyclesAllowedWithReservation"/>
 *     &lt;enumeration value="vehicleTransport"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "LuggageCarriageEnumeration")
@XmlEnum
public enum LuggageCarriageEnumeration {

    @XmlEnumValue("unknown")
    UNKNOWN("unknown"),
    @XmlEnumValue("noBaggageStorage")
    NO_BAGGAGE_STORAGE("noBaggageStorage"),
    @XmlEnumValue("baggageStorage")
    BAGGAGE_STORAGE("baggageStorage"),
    @XmlEnumValue("luggageRacks")
    LUGGAGE_RACKS("luggageRacks"),
    @XmlEnumValue("extraLargeLuggageRacks")
    EXTRA_LARGE_LUGGAGE_RACKS("extraLargeLuggageRacks"),
    @XmlEnumValue("baggageVan")
    BAGGAGE_VAN("baggageVan"),
    @XmlEnumValue("noCycles")
    NO_CYCLES("noCycles"),
    @XmlEnumValue("cyclesAllowed")
    CYCLES_ALLOWED("cyclesAllowed"),
    @XmlEnumValue("cyclesAllowedInVan")
    CYCLES_ALLOWED_IN_VAN("cyclesAllowedInVan"),
    @XmlEnumValue("cyclesAllowedInCarriage")
    CYCLES_ALLOWED_IN_CARRIAGE("cyclesAllowedInCarriage"),
    @XmlEnumValue("cyclesAllowedWithReservation")
    CYCLES_ALLOWED_WITH_RESERVATION("cyclesAllowedWithReservation"),
    @XmlEnumValue("vehicleTransport")
    VEHICLE_TRANSPORT("vehicleTransport");
    private final String value;

    LuggageCarriageEnumeration(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static LuggageCarriageEnumeration fromValue(String v) {
        for (LuggageCarriageEnumeration c: LuggageCarriageEnumeration.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
