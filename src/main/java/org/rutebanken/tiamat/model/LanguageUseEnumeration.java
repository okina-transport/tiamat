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
 * <p>Java class for LanguageUseEnumeration.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="LanguageUseEnumeration">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="normallyUsed"/>
 *     &lt;enumeration value="understood"/>
 *     &lt;enumeration value="native"/>
 *     &lt;enumeration value="spoken"/>
 *     &lt;enumeration value="written"/>
 *     &lt;enumeration value="read"/>
 *     &lt;enumeration value="other"/>
 *     &lt;enumeration value="allUses"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "LanguageUseEnumeration")
public enum LanguageUseEnumeration {

    NORMALLY_USED("normallyUsed"),
    UNDERSTOOD("understood"),
    NATIVE("native"),
    SPOKEN("spoken"),
    WRITTEN("written"),
    READ("read"),
    OTHER("other"),
    ALL_USES("allUses");
    private final String value;

    LanguageUseEnumeration(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static LanguageUseEnumeration fromValue(String v) {
        for (LanguageUseEnumeration c: LanguageUseEnumeration.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
