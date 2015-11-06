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
 * <p>Java class for SafetyFacilityEnumeration.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="SafetyFacilityEnumeration">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}normalizedString">
 *     &lt;enumeration value="ccTv"/>
 *     &lt;enumeration value="mobileCoverage"/>
 *     &lt;enumeration value="sosPoints"/>
 *     &lt;enumeration value="staffed"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "SafetyFacilityEnumeration")
@XmlEnum
public enum SafetyFacilityEnumeration {

    @XmlEnumValue("ccTv")
    CC_TV("ccTv"),
    @XmlEnumValue("mobileCoverage")
    MOBILE_COVERAGE("mobileCoverage"),
    @XmlEnumValue("sosPoints")
    SOS_POINTS("sosPoints"),
    @XmlEnumValue("staffed")
    STAFFED("staffed");
    private final String value;

    SafetyFacilityEnumeration(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static SafetyFacilityEnumeration fromValue(String v) {
        for (SafetyFacilityEnumeration c: SafetyFacilityEnumeration.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
