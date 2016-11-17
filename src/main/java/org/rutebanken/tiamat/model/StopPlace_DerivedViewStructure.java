//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.06 at 10:37:32 AM CET 
//


package org.rutebanken.tiamat.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.NormalizedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * Type for STOP PLACE VIEW.
 * 
 * <p>Java class for StopPlace_DerivedViewStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="StopPlace_DerivedViewStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}DerivedViewStructure">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.netex.org.uk/netex}StopPlaceRef" minOccurs="0"/>
 *         &lt;element name="Name" type="{http://www.netex.org.uk/netex}MultilingualStringEntity" minOccurs="0"/>
 *         &lt;element name="placeTypes" type="{http://www.netex.org.uk/netex}typeOfPlaceRefs_RelStructure" minOccurs="0"/>
 *         &lt;element name="ShortName" type="{http://www.netex.org.uk/netex}MultilingualStringEntity" minOccurs="0"/>
 *         &lt;group ref="{http://www.netex.org.uk/netex}StopIdentifierGroup"/>
 *         &lt;element name="StopPlaceType" type="{http://www.netex.org.uk/netex}StopTypeEnumeration" minOccurs="0"/>
 *         &lt;element name="TransportMode" type="{http://www.netex.org.uk/netex}VehicleModeEnumeration" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "StopPlace_DerivedViewStructure", propOrder = {
    "stopPlaceRef",
    "name",
    "placeTypes",
    "shortName",
    "publicCode",
    "stopPlaceType",
    "transportMode"
})
@XmlSeeAlso({
    StopPlaceView.class
})
public class StopPlace_DerivedViewStructure
    extends DerivedViewStructure
{

    @XmlElement(name = "StopPlaceRef")
    protected StopPlaceReference stopPlaceRef;
    @XmlElement(name = "Name")
    protected MultilingualStringEntity name;
    protected TypeOfPlaceRefs_RelStructure placeTypes;
    @XmlElement(name = "ShortName")
    protected MultilingualStringEntity shortName;
    @XmlElement(name = "PublicCode")
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    @XmlSchemaType(name = "normalizedString")
    protected String publicCode;
    @XmlElement(name = "StopPlaceType")
    @XmlSchemaType(name = "string")
    protected StopTypeEnumeration stopPlaceType;
    @XmlElement(name = "TransportMode")
    @XmlSchemaType(name = "NMTOKEN")
    protected VehicleModeEnumeration transportMode;

    /**
     * Gets the value of the stopPlaceRef property.
     * 
     * @return
     *     possible object is
     *     {@link StopPlaceReference }
     *     
     */
    public StopPlaceReference getStopPlaceRef() {
        return stopPlaceRef;
    }

    /**
     * Sets the value of the stopPlaceRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link StopPlaceReference }
     *     
     */
    public void setStopPlaceRef(StopPlaceReference value) {
        this.stopPlaceRef = value;
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
     * Gets the value of the placeTypes property.
     * 
     * @return
     *     possible object is
     *     {@link TypeOfPlaceRefs_RelStructure }
     *     
     */
    public TypeOfPlaceRefs_RelStructure getPlaceTypes() {
        return placeTypes;
    }

    /**
     * Sets the value of the placeTypes property.
     * 
     * @param value
     *     allowed object is
     *     {@link TypeOfPlaceRefs_RelStructure }
     *     
     */
    public void setPlaceTypes(TypeOfPlaceRefs_RelStructure value) {
        this.placeTypes = value;
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
     * Gets the value of the publicCode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPublicCode() {
        return publicCode;
    }

    /**
     * Sets the value of the publicCode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPublicCode(String value) {
        this.publicCode = value;
    }

    /**
     * Gets the value of the stopPlaceType property.
     * 
     * @return
     *     possible object is
     *     {@link StopTypeEnumeration }
     *     
     */
    public StopTypeEnumeration getStopPlaceType() {
        return stopPlaceType;
    }

    /**
     * Sets the value of the stopPlaceType property.
     * 
     * @param value
     *     allowed object is
     *     {@link StopTypeEnumeration }
     *     
     */
    public void setStopPlaceType(StopTypeEnumeration value) {
        this.stopPlaceType = value;
    }

    /**
     * Gets the value of the transportMode property.
     * 
     * @return
     *     possible object is
     *     {@link VehicleModeEnumeration }
     *     
     */
    public VehicleModeEnumeration getTransportMode() {
        return transportMode;
    }

    /**
     * Sets the value of the transportMode property.
     * 
     * @param value
     *     allowed object is
     *     {@link VehicleModeEnumeration }
     *     
     */
    public void setTransportMode(VehicleModeEnumeration value) {
        this.transportMode = value;
    }

}
