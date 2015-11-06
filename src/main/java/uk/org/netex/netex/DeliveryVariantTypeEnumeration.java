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
 * <p>Java class for DeliveryVariantTypeEnumeration.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="DeliveryVariantTypeEnumeration">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}normalizedString">
 *     &lt;enumeration value="printed"/>
 *     &lt;enumeration value="textToSpeech"/>
 *     &lt;enumeration value="web"/>
 *     &lt;enumeration value="mobile"/>
 *     &lt;enumeration value="other"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "DeliveryVariantTypeEnumeration")
@XmlEnum
public enum DeliveryVariantTypeEnumeration {

    @XmlEnumValue("printed")
    PRINTED("printed"),
    @XmlEnumValue("textToSpeech")
    TEXT_TO_SPEECH("textToSpeech"),
    @XmlEnumValue("web")
    WEB("web"),
    @XmlEnumValue("mobile")
    MOBILE("mobile"),
    @XmlEnumValue("other")
    OTHER("other");
    private final String value;

    DeliveryVariantTypeEnumeration(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static DeliveryVariantTypeEnumeration fromValue(String v) {
        for (DeliveryVariantTypeEnumeration c: DeliveryVariantTypeEnumeration.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
