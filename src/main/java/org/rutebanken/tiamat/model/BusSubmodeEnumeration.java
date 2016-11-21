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
 * <p>Java class for BusSubmodeEnumeration.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="BusSubmodeEnumeration">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}NMTOKEN">
 *     &lt;enumeration value="unknown"/>
 *     &lt;enumeration value="undefined"/>
 *     &lt;enumeration value="localBus"/>
 *     &lt;enumeration value="regionalBus"/>
 *     &lt;enumeration value="expressBus"/>
 *     &lt;enumeration value="nightBus"/>
 *     &lt;enumeration value="postBus"/>
 *     &lt;enumeration value="specialNeedsBus"/>
 *     &lt;enumeration value="mobilityBus"/>
 *     &lt;enumeration value="mobilityBusForRegisteredDisabled"/>
 *     &lt;enumeration value="sightseeingBus"/>
 *     &lt;enumeration value="shuttleBus"/>
 *     &lt;enumeration value="highFrequencyBus"/>
 *     &lt;enumeration value="dedicatedLaneBus"/>
 *     &lt;enumeration value="schoolBus"/>
 *     &lt;enumeration value="schoolAndPublicServiceBus"/>
 *     &lt;enumeration value="railReplacementBus"/>
 *     &lt;enumeration value="demandAndResponseBus"/>
 *     &lt;enumeration value="airportLinkBus"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "BusSubmodeEnumeration")
public enum BusSubmodeEnumeration {

    UNKNOWN("unknown"),
    UNDEFINED("undefined"),
    LOCAL_BUS("localBus"),
    REGIONAL_BUS("regionalBus"),
    EXPRESS_BUS("expressBus"),
    NIGHT_BUS("nightBus"),
    POST_BUS("postBus"),
    SPECIAL_NEEDS_BUS("specialNeedsBus"),
    MOBILITY_BUS("mobilityBus"),
    MOBILITY_BUS_FOR_REGISTERED_DISABLED("mobilityBusForRegisteredDisabled"),
    SIGHTSEEING_BUS("sightseeingBus"),
    SHUTTLE_BUS("shuttleBus"),
    HIGH_FREQUENCY_BUS("highFrequencyBus"),
    DEDICATED_LANE_BUS("dedicatedLaneBus"),
    SCHOOL_BUS("schoolBus"),
    SCHOOL_AND_PUBLIC_SERVICE_BUS("schoolAndPublicServiceBus"),
    RAIL_REPLACEMENT_BUS("railReplacementBus"),
    DEMAND_AND_RESPONSE_BUS("demandAndResponseBus"),
    AIRPORT_LINK_BUS("airportLinkBus");
    private final String value;

    BusSubmodeEnumeration(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static BusSubmodeEnumeration fromValue(String v) {
        for (BusSubmodeEnumeration c: BusSubmodeEnumeration.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
