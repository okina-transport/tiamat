//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.05 at 07:41:01 PM CET 
//


package org.rutebanken.tiamat.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * Type for a FLEXIBLE ROUTE.
 * 
 * <p>Java class for FlexibleRoute_VersionStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="FlexibleRoute_VersionStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}Route_VersionStructure">
 *       &lt;sequence>
 *         &lt;group ref="{http://www.netex.org.uk/netex}FlexibleRouteGroup"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FlexibleRoute_VersionStructure", propOrder = {
    "flexibleRouteType"
})
@XmlSeeAlso({
    FlexibleRoute.class
})
public class FlexibleRoute_VersionStructure
    extends Route_VersionStructure
{

    @XmlSchemaType(name = "string")
    protected FlexibleRouteTypeEnumeration flexibleRouteType;

    /**
     * Gets the value of the flexibleRouteType property.
     * 
     * @return
     *     possible object is
     *     {@link FlexibleRouteTypeEnumeration }
     *     
     */
    public FlexibleRouteTypeEnumeration getFlexibleRouteType() {
        return flexibleRouteType;
    }

    /**
     * Sets the value of the flexibleRouteType property.
     * 
     * @param value
     *     allowed object is
     *     {@link FlexibleRouteTypeEnumeration }
     *     
     */
    public void setFlexibleRouteType(FlexibleRouteTypeEnumeration value) {
        this.flexibleRouteType = value;
    }

}
