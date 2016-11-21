//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.05 at 07:41:01 PM CET 
//


package org.rutebanken.tiamat.model;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * Type for a PATH LINK VIEW.
 * 
 * <p>Java class for PathJunction_VersionStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="PathJunction_VersionStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}Point_VersionStructure">
 *       &lt;sequence>
 *         &lt;group ref="{http://www.netex.org.uk/netex}PathJunctionGroup"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PathJunction_VersionStructure", propOrder = {
    "parentZoneRef",
    "publicUse",
    "covered",
    "gated",
    "lighting",
    "allAreasWheelchairAccessible",
    "personCapacity",
    "facilities",
    "label",
    "siteComponentRef"
})
@XmlSeeAlso({
    PathJunction.class
})
public class PathJunction_VersionStructure
    extends Point_VersionStructure
{

    protected ZoneRefStructure parentZoneRef;
    @XmlSchemaType(name = "string")
    protected PublicUseEnumeration publicUse;
    @XmlSchemaType(name = "string")
    protected CoveredEnumeration covered;
    @XmlSchemaType(name = "string")
    protected GatedEnumeration gated;
    @XmlSchemaType(name = "normalizedString")
    protected LightingEnumeration lighting;
    protected Boolean allAreasWheelchairAccessible;
    @XmlSchemaType(name = "nonNegativeInteger")
    protected BigInteger personCapacity;
    protected SiteFacilitySets_RelStructure facilities;
    protected MultilingualStringEntity label;
    protected SiteComponentRefStructure siteComponentRef;

    /**
     * Gets the value of the parentZoneRef property.
     * 
     * @return
     *     possible object is
     *     {@link ZoneRefStructure }
     *     
     */
    public ZoneRefStructure getParentZoneRef() {
        return parentZoneRef;
    }

    /**
     * Sets the value of the parentZoneRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link ZoneRefStructure }
     *     
     */
    public void setParentZoneRef(ZoneRefStructure value) {
        this.parentZoneRef = value;
    }

    /**
     * Gets the value of the publicUse property.
     * 
     * @return
     *     possible object is
     *     {@link PublicUseEnumeration }
     *     
     */
    public PublicUseEnumeration getPublicUse() {
        return publicUse;
    }

    /**
     * Sets the value of the publicUse property.
     * 
     * @param value
     *     allowed object is
     *     {@link PublicUseEnumeration }
     *     
     */
    public void setPublicUse(PublicUseEnumeration value) {
        this.publicUse = value;
    }

    /**
     * Gets the value of the covered property.
     * 
     * @return
     *     possible object is
     *     {@link CoveredEnumeration }
     *     
     */
    public CoveredEnumeration getCovered() {
        return covered;
    }

    /**
     * Sets the value of the covered property.
     * 
     * @param value
     *     allowed object is
     *     {@link CoveredEnumeration }
     *     
     */
    public void setCovered(CoveredEnumeration value) {
        this.covered = value;
    }

    /**
     * Gets the value of the gated property.
     * 
     * @return
     *     possible object is
     *     {@link GatedEnumeration }
     *     
     */
    public GatedEnumeration getGated() {
        return gated;
    }

    /**
     * Sets the value of the gated property.
     * 
     * @param value
     *     allowed object is
     *     {@link GatedEnumeration }
     *     
     */
    public void setGated(GatedEnumeration value) {
        this.gated = value;
    }

    /**
     * Gets the value of the lighting property.
     * 
     * @return
     *     possible object is
     *     {@link LightingEnumeration }
     *     
     */
    public LightingEnumeration getLighting() {
        return lighting;
    }

    /**
     * Sets the value of the lighting property.
     * 
     * @param value
     *     allowed object is
     *     {@link LightingEnumeration }
     *     
     */
    public void setLighting(LightingEnumeration value) {
        this.lighting = value;
    }

    /**
     * Gets the value of the allAreasWheelchairAccessible property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isAllAreasWheelchairAccessible() {
        return allAreasWheelchairAccessible;
    }

    /**
     * Sets the value of the allAreasWheelchairAccessible property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setAllAreasWheelchairAccessible(Boolean value) {
        this.allAreasWheelchairAccessible = value;
    }

    /**
     * Gets the value of the personCapacity property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getPersonCapacity() {
        return personCapacity;
    }

    /**
     * Sets the value of the personCapacity property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setPersonCapacity(BigInteger value) {
        this.personCapacity = value;
    }

    /**
     * Gets the value of the facilities property.
     * 
     * @return
     *     possible object is
     *     {@link SiteFacilitySets_RelStructure }
     *     
     */
    public SiteFacilitySets_RelStructure getFacilities() {
        return facilities;
    }

    /**
     * Sets the value of the facilities property.
     * 
     * @param value
     *     allowed object is
     *     {@link SiteFacilitySets_RelStructure }
     *     
     */
    public void setFacilities(SiteFacilitySets_RelStructure value) {
        this.facilities = value;
    }

    /**
     * Gets the value of the label property.
     * 
     * @return
     *     possible object is
     *     {@link MultilingualStringEntity }
     *     
     */
    public MultilingualStringEntity getLabel() {
        return label;
    }

    /**
     * Sets the value of the label property.
     * 
     * @param value
     *     allowed object is
     *     {@link MultilingualStringEntity }
     *     
     */
    public void setLabel(MultilingualStringEntity value) {
        this.label = value;
    }

    /**
     * Gets the value of the siteComponentRef property.
     * 
     * @return
     *     possible object is
     *     {@link SiteComponentRefStructure }
     *     
     */
    public SiteComponentRefStructure getSiteComponentRef() {
        return siteComponentRef;
    }

    /**
     * Sets the value of the siteComponentRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link SiteComponentRefStructure }
     *     
     */
    public void setSiteComponentRef(SiteComponentRefStructure value) {
        this.siteComponentRef = value;
    }

}
