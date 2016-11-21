//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.05 at 07:41:01 PM CET 
//


package org.rutebanken.tiamat.model;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlType;


/**
 * Type for SCHEDULED STOP POINT VIEW.
 * 
 * <p>Java class for Zone_DerivedViewStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Zone_DerivedViewStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}DerivedViewStructure">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.netex.org.uk/netex}ZoneRef" minOccurs="0"/>
 *         &lt;element name="Name" type="{http://www.netex.org.uk/netex}MultilingualStringEntity" minOccurs="0"/>
 *         &lt;element ref="{http://www.netex.org.uk/netex}TypeOfZoneRef" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Zone_DerivedViewStructure", propOrder = {
    "zoneRef",
    "name",
    "typeOfZoneRef"
})
public class Zone_DerivedViewStructure
    extends DerivedViewStructure
{

    protected JAXBElement<? extends ZoneRefStructure> zoneRef;
    protected MultilingualStringEntity name;
    protected TypeOfZoneRefStructure typeOfZoneRef;

    /**
     * Gets the value of the zoneRef property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link StopAreaRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link AdministrativeZoneRef }{@code >}
     *     {@link JAXBElement }{@code <}{@link ZoneRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link AccessZoneRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link TransportAdministrativeZoneRef }{@code >}
     *     
     */
    public JAXBElement<? extends ZoneRefStructure> getZoneRef() {
        return zoneRef;
    }

    /**
     * Sets the value of the zoneRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link StopAreaRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link AdministrativeZoneRef }{@code >}
     *     {@link JAXBElement }{@code <}{@link ZoneRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link FareZoneRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link AccessZoneRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link TariffZoneRef }{@code >}
     *     {@link JAXBElement }{@code <}{@link TransportAdministrativeZoneRef }{@code >}
     *     
     */
    public void setZoneRef(JAXBElement<? extends ZoneRefStructure> value) {
        this.zoneRef = value;
    }

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link MultilingualStringEntity }
     *     
     */
    public MultilingualStringEntity getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link MultilingualStringEntity }
     *     
     */
    public void setName(MultilingualStringEntity value) {
        this.name = value;
    }

    /**
     * Gets the value of the typeOfZoneRef property.
     * 
     * @return
     *     possible object is
     *     {@link TypeOfZoneRefStructure }
     *     
     */
    public TypeOfZoneRefStructure getTypeOfZoneRef() {
        return typeOfZoneRef;
    }

    /**
     * Sets the value of the typeOfZoneRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link TypeOfZoneRefStructure }
     *     
     */
    public void setTypeOfZoneRef(TypeOfZoneRefStructure value) {
        this.typeOfZoneRef = value;
    }

}
