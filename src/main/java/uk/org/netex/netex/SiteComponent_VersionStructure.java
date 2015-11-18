//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.05 at 07:41:01 PM CET 
//


package uk.org.netex.netex;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * A component of a SITE COMPONENT.
 * 
 * <p>Java class for SiteComponent_VersionStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SiteComponent_VersionStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}SiteElement_VersionStructure">
 *       &lt;sequence>
 *         &lt;group ref="{http://www.netex.org.uk/netex}SiteComponentGroup"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SiteComponent_VersionStructure", propOrder = {
    "siteRef",
    "levelRef",
    "classOfUseRef",
    "checkConstraints",
    "equipmentPlaces",
    "placeEquipments",
    "localServices"
})
@XmlSeeAlso({
    PointOfInterestComponent_VersionStructure.class,
    StopPlaceComponent_VersionStructure.class,
    ParkingComponent_VersionStructure.class,
    SiteEntrance_VersionStructure.class
})
@MappedSuperclass
public abstract class SiteComponent_VersionStructure
    extends SiteElement_VersionStructure
{

    @XmlElement(name = "SiteRef")
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    protected SiteRefStructure siteRef;

    @XmlElement(name = "LevelRef")
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    protected LevelRefStructure levelRef;

    @XmlElement(name = "ClassOfUseRef")
    @Transient
    protected ClassOfUseRef classOfUseRef;

    @Transient
    protected CheckConstraints_RelStructure checkConstraints;

    @Transient
    protected EquipmentPlaces_RelStructure equipmentPlaces;

    @Transient
    protected PlaceEquipments_RelStructure placeEquipments;

    @Transient
    protected LocalServices_RelStructure localServices;

    /**
     * Gets the value of the siteRef property.
     * 
     * @return
     *     possible object is
     *     {@link SiteRefStructure }
     *     
     */
    public SiteRefStructure getSiteRef() {
        return siteRef;
    }

    /**
     * Sets the value of the siteRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link SiteRefStructure }
     *     
     */
    public void setSiteRef(SiteRefStructure value) {
        this.siteRef = value;
    }

    /**
     * Gets the value of the levelRef property.
     * 
     * @return
     *     possible object is
     *     {@link LevelRefStructure }
     *     
     */
    public LevelRefStructure getLevelRef() {
        return levelRef;
    }

    /**
     * Sets the value of the levelRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link LevelRefStructure }
     *     
     */
    public void setLevelRef(LevelRefStructure value) {
        this.levelRef = value;
    }

    /**
     * Gets the value of the classOfUseRef property.
     * 
     * @return
     *     possible object is
     *     {@link ClassOfUseRef }
     *     
     */
    public ClassOfUseRef getClassOfUseRef() {
        return classOfUseRef;
    }

    /**
     * Sets the value of the classOfUseRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link ClassOfUseRef }
     *     
     */
    public void setClassOfUseRef(ClassOfUseRef value) {
        this.classOfUseRef = value;
    }

    /**
     * Gets the value of the checkConstraints property.
     * 
     * @return
     *     possible object is
     *     {@link CheckConstraints_RelStructure }
     *     
     */
    public CheckConstraints_RelStructure getCheckConstraints() {
        return checkConstraints;
    }

    /**
     * Sets the value of the checkConstraints property.
     * 
     * @param value
     *     allowed object is
     *     {@link CheckConstraints_RelStructure }
     *     
     */
    public void setCheckConstraints(CheckConstraints_RelStructure value) {
        this.checkConstraints = value;
    }

    /**
     * Gets the value of the equipmentPlaces property.
     * 
     * @return
     *     possible object is
     *     {@link EquipmentPlaces_RelStructure }
     *     
     */
    public EquipmentPlaces_RelStructure getEquipmentPlaces() {
        return equipmentPlaces;
    }

    /**
     * Sets the value of the equipmentPlaces property.
     * 
     * @param value
     *     allowed object is
     *     {@link EquipmentPlaces_RelStructure }
     *     
     */
    public void setEquipmentPlaces(EquipmentPlaces_RelStructure value) {
        this.equipmentPlaces = value;
    }

    /**
     * Gets the value of the placeEquipments property.
     * 
     * @return
     *     possible object is
     *     {@link PlaceEquipments_RelStructure }
     *     
     */
    public PlaceEquipments_RelStructure getPlaceEquipments() {
        return placeEquipments;
    }

    /**
     * Sets the value of the placeEquipments property.
     * 
     * @param value
     *     allowed object is
     *     {@link PlaceEquipments_RelStructure }
     *     
     */
    public void setPlaceEquipments(PlaceEquipments_RelStructure value) {
        this.placeEquipments = value;
    }

    /**
     * Gets the value of the localServices property.
     * 
     * @return
     *     possible object is
     *     {@link LocalServices_RelStructure }
     *     
     */
    public LocalServices_RelStructure getLocalServices() {
        return localServices;
    }

    /**
     * Sets the value of the localServices property.
     * 
     * @param value
     *     allowed object is
     *     {@link LocalServices_RelStructure }
     *     
     */
    public void setLocalServices(LocalServices_RelStructure value) {
        this.localServices = value;
    }

}
