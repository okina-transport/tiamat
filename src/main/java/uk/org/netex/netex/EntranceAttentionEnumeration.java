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
 * <p>Java class for EntranceAttentionEnumeration.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="EntranceAttentionEnumeration">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="none"/>
 *     &lt;enumeration value="doorbell"/>
 *     &lt;enumeration value="helpPoint"/>
 *     &lt;enumeration value="intercom"/>
 *     &lt;enumeration value="other"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "EntranceAttentionEnumeration")
@XmlEnum
public enum EntranceAttentionEnumeration {

    @XmlEnumValue("none")
    NONE("none"),
    @XmlEnumValue("doorbell")
    DOORBELL("doorbell"),
    @XmlEnumValue("helpPoint")
    HELP_POINT("helpPoint"),
    @XmlEnumValue("intercom")
    INTERCOM("intercom"),
    @XmlEnumValue("other")
    OTHER("other");
    private final String value;

    EntranceAttentionEnumeration(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static EntranceAttentionEnumeration fromValue(String v) {
        for (EntranceAttentionEnumeration c: EntranceAttentionEnumeration.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
