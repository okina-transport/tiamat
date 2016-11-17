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
 * Type for DESTINATION DISPLAY VARIANT.
 * 
 * <p>Java class for DestinationDisplayVariant_VersionStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="DestinationDisplayVariant_VersionStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}DataManagedObjectStructure">
 *       &lt;sequence>
 *         &lt;group ref="{http://www.netex.org.uk/netex}DestinationDisplayVariantGroup"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DestinationDisplayVariant_VersionStructure", propOrder = {
    "destinationDisplayRef",
    "destinationDisplayVariantMediaType",
    "name",
    "shortName",
    "sideText",
    "frontText",
    "driverDisplayText",
    "vias"
})
@XmlSeeAlso({
    DestinationDisplayVariant.class
})
public class DestinationDisplayVariant_VersionStructure
    extends DataManagedObjectStructure
{

    @XmlElement(name = "DestinationDisplayRef")
    protected DestinationDisplayRefStructure destinationDisplayRef;
    @XmlElement(name = "DestinationDisplayVariantMediaType", required = true)
    @XmlSchemaType(name = "normalizedString")
    protected DeliveryVariantTypeEnumeration destinationDisplayVariantMediaType;
    @XmlElement(name = "Name")
    protected MultilingualStringEntity name;
    @XmlElement(name = "ShortName")
    protected MultilingualStringEntity shortName;
    @XmlElement(name = "SideText")
    protected MultilingualStringEntity sideText;
    @XmlElement(name = "FrontText")
    protected MultilingualStringEntity frontText;
    @XmlElement(name = "DriverDisplayText")
    protected MultilingualStringEntity driverDisplayText;
    protected Vias_RelStructure vias;

    /**
     * Gets the value of the destinationDisplayRef property.
     * 
     * @return
     *     possible object is
     *     {@link DestinationDisplayRefStructure }
     *     
     */
    public DestinationDisplayRefStructure getDestinationDisplayRef() {
        return destinationDisplayRef;
    }

    /**
     * Sets the value of the destinationDisplayRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link DestinationDisplayRefStructure }
     *     
     */
    public void setDestinationDisplayRef(DestinationDisplayRefStructure value) {
        this.destinationDisplayRef = value;
    }

    /**
     * Gets the value of the destinationDisplayVariantMediaType property.
     * 
     * @return
     *     possible object is
     *     {@link DeliveryVariantTypeEnumeration }
     *     
     */
    public DeliveryVariantTypeEnumeration getDestinationDisplayVariantMediaType() {
        return destinationDisplayVariantMediaType;
    }

    /**
     * Sets the value of the destinationDisplayVariantMediaType property.
     * 
     * @param value
     *     allowed object is
     *     {@link DeliveryVariantTypeEnumeration }
     *     
     */
    public void setDestinationDisplayVariantMediaType(DeliveryVariantTypeEnumeration value) {
        this.destinationDisplayVariantMediaType = value;
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
     * Gets the value of the shortName property.
     * 
     * @return
     *     possible object is
     *     {@link MultilingualStringEntity }
     *     
     */
    public MultilingualStringEntity getShortName() {
        return shortName;
    }

    /**
     * Sets the value of the shortName property.
     * 
     * @param value
     *     allowed object is
     *     {@link MultilingualStringEntity }
     *     
     */
    public void setShortName(MultilingualStringEntity value) {
        this.shortName = value;
    }

    /**
     * Gets the value of the sideText property.
     * 
     * @return
     *     possible object is
     *     {@link MultilingualStringEntity }
     *     
     */
    public MultilingualStringEntity getSideText() {
        return sideText;
    }

    /**
     * Sets the value of the sideText property.
     * 
     * @param value
     *     allowed object is
     *     {@link MultilingualStringEntity }
     *     
     */
    public void setSideText(MultilingualStringEntity value) {
        this.sideText = value;
    }

    /**
     * Gets the value of the frontText property.
     * 
     * @return
     *     possible object is
     *     {@link MultilingualStringEntity }
     *     
     */
    public MultilingualStringEntity getFrontText() {
        return frontText;
    }

    /**
     * Sets the value of the frontText property.
     * 
     * @param value
     *     allowed object is
     *     {@link MultilingualStringEntity }
     *     
     */
    public void setFrontText(MultilingualStringEntity value) {
        this.frontText = value;
    }

    /**
     * Gets the value of the driverDisplayText property.
     * 
     * @return
     *     possible object is
     *     {@link MultilingualStringEntity }
     *     
     */
    public MultilingualStringEntity getDriverDisplayText() {
        return driverDisplayText;
    }

    /**
     * Sets the value of the driverDisplayText property.
     * 
     * @param value
     *     allowed object is
     *     {@link MultilingualStringEntity }
     *     
     */
    public void setDriverDisplayText(MultilingualStringEntity value) {
        this.driverDisplayText = value;
    }

    /**
     * Gets the value of the vias property.
     * 
     * @return
     *     possible object is
     *     {@link Vias_RelStructure }
     *     
     */
    public Vias_RelStructure getVias() {
        return vias;
    }

    /**
     * Sets the value of the vias property.
     * 
     * @param value
     *     allowed object is
     *     {@link Vias_RelStructure }
     *     
     */
    public void setVias(Vias_RelStructure value) {
        this.vias = value;
    }

}
