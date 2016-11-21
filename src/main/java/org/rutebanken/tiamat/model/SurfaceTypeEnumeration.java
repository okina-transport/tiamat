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
 * <p>Java class for SurfaceTypeEnumeration.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="SurfaceTypeEnumeration">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}normalizedString">
 *     &lt;enumeration value="asphalt"/>
 *     &lt;enumeration value="bricks"/>
 *     &lt;enumeration value="cobbles"/>
 *     &lt;enumeration value="earth"/>
 *     &lt;enumeration value="grass"/>
 *     &lt;enumeration value="looseSurface"/>
 *     &lt;enumeration value="pavingStones"/>
 *     &lt;enumeration value="roughSurface"/>
 *     &lt;enumeration value="smooth"/>
 *     &lt;enumeration value="other"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "SurfaceTypeEnumeration")
public enum SurfaceTypeEnumeration {

    ASPHALT("asphalt"),
    BRICKS("bricks"),
    COBBLES("cobbles"),
    EARTH("earth"),
    GRASS("grass"),
    LOOSE_SURFACE("looseSurface"),
    PAVING_STONES("pavingStones"),
    ROUGH_SURFACE("roughSurface"),
    SMOOTH("smooth"),
    OTHER("other");
    private final String value;

    SurfaceTypeEnumeration(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static SurfaceTypeEnumeration fromValue(String v) {
        for (SurfaceTypeEnumeration c: SurfaceTypeEnumeration.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
