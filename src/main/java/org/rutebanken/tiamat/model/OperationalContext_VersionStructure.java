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
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * Type for an OPERATIONAL CONTEXT.
 * 
 * <p>Java class for OperationalContext_VersionStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="OperationalContext_VersionStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}DataManagedObjectStructure">
 *       &lt;sequence>
 *         &lt;group ref="{http://www.netex.org.uk/netex}OperationalContextGroup"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "OperationalContext_VersionStructure", propOrder = {
    "name",
    "shortName",
    "privateCode",
    "organisationPartRef",
    "vehicleMode",
    "transportSubmode"
})
@XmlSeeAlso({
    OperationalContext.class
})
public class OperationalContext_VersionStructure
    extends DataManagedObjectStructure
{

    @XmlElement(name = "Name")
    protected MultilingualStringEntity name;
    @XmlElement(name = "ShortName")
    protected MultilingualStringEntity shortName;
    @XmlElement(name = "PrivateCode")
    protected PrivateCodeStructure privateCode;
    @XmlElementRef(name = "OrganisationPartRef", namespace = "http://www.netex.org.uk/netex", type = JAXBElement.class, required = false)
    protected JAXBElement<? extends OrganisationPartRefStructure> organisationPartRef;
    @XmlElement(name = "VehicleMode")
    @XmlSchemaType(name = "NMTOKEN")
    protected AllModesEnumeration vehicleMode;
    @XmlElement(name = "TransportSubmode")
    protected TransportSubmodeStructure transportSubmode;

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
     * Gets the value of the privateCode property.
     * 
     * @return
     *     possible object is
     *     {@link PrivateCodeStructure }
     *     
     */
    public PrivateCodeStructure getPrivateCode() {
        return privateCode;
    }

    /**
     * Sets the value of the privateCode property.
     * 
     * @param value
     *     allowed object is
     *     {@link PrivateCodeStructure }
     *     
     */
    public void setPrivateCode(PrivateCodeStructure value) {
        this.privateCode = value;
    }

    /**
     * Gets the value of the organisationPartRef property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link OrganisationalUnitRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link DepartmentRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link ControlCentreRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link OrganisationPartRefStructure }{@code >}
     *     
     */
    public JAXBElement<? extends OrganisationPartRefStructure> getOrganisationPartRef() {
        return organisationPartRef;
    }

    /**
     * Sets the value of the organisationPartRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link OrganisationalUnitRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link DepartmentRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link ControlCentreRefStructure }{@code >}
     *     {@link JAXBElement }{@code <}{@link OrganisationPartRefStructure }{@code >}
     *     
     */
    public void setOrganisationPartRef(JAXBElement<? extends OrganisationPartRefStructure> value) {
        this.organisationPartRef = value;
    }

    /**
     * Gets the value of the vehicleMode property.
     * 
     * @return
     *     possible object is
     *     {@link AllModesEnumeration }
     *     
     */
    public AllModesEnumeration getVehicleMode() {
        return vehicleMode;
    }

    /**
     * Sets the value of the vehicleMode property.
     * 
     * @param value
     *     allowed object is
     *     {@link AllModesEnumeration }
     *     
     */
    public void setVehicleMode(AllModesEnumeration value) {
        this.vehicleMode = value;
    }

    /**
     * Gets the value of the transportSubmode property.
     * 
     * @return
     *     possible object is
     *     {@link TransportSubmodeStructure }
     *     
     */
    public TransportSubmodeStructure getTransportSubmode() {
        return transportSubmode;
    }

    /**
     * Sets the value of the transportSubmode property.
     * 
     * @param value
     *     allowed object is
     *     {@link TransportSubmodeStructure }
     *     
     */
    public void setTransportSubmode(TransportSubmodeStructure value) {
        this.transportSubmode = value;
    }

}
