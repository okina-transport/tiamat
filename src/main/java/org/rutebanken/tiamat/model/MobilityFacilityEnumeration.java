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
 * <p>Java class for MobilityFacilityEnumeration.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="MobilityFacilityEnumeration">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}NMTOKEN">
 *     &lt;enumeration value="unknown"/>
 *     &lt;enumeration value="lowFloor"/>
 *     &lt;enumeration value="stepFreeAccess"/>
 *     &lt;enumeration value="suitableForWheelchairs"/>
 *     &lt;enumeration value="suitableForHeaviliyDisabled"/>
 *     &lt;enumeration value="boardingAssistance"/>
 *     &lt;enumeration value="onboardAssistance"/>
 *     &lt;enumeration value="unaccompaniedMinorAssistance"/>
 *     &lt;enumeration value="tactilePatformEdges"/>
 *     &lt;enumeration value="tactileGuidingStrips"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "MobilityFacilityEnumeration")
public enum MobilityFacilityEnumeration {


    /**
     * pti255_4
     * 
     */
    UNKNOWN("unknown"),

    /**
     * pti23_16_1
     * 
     */
    LOW_FLOOR("lowFloor"),

    /**
     * pti23_16_3
     * 
     */
    STEP_FREE_ACCESS("stepFreeAccess"),

    /**
     * pti23_16_1
     * 
     */
    SUITABLE_FOR_WHEELCHAIRS("suitableForWheelchairs"),
    SUITABLE_FOR_HEAVILIY_DISABLED("suitableForHeaviliyDisabled"),

    /**
     * pti23_16_2
     * 
     */
    BOARDING_ASSISTANCE("boardingAssistance"),
    ONBOARD_ASSISTANCE("onboardAssistance"),
    UNACCOMPANIED_MINOR_ASSISTANCE("unaccompaniedMinorAssistance"),
    TACTILE_PATFORM_EDGES("tactilePatformEdges"),
    TACTILE_GUIDING_STRIPS("tactileGuidingStrips");
    private final String value;

    MobilityFacilityEnumeration(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static MobilityFacilityEnumeration fromValue(String v) {
        for (MobilityFacilityEnumeration c: MobilityFacilityEnumeration.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
