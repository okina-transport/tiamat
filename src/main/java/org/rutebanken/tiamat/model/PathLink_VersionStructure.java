//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.05 at 07:41:01 PM CET 
//


package org.rutebanken.tiamat.model;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * Type for a PATH LINK.
 * 
 * <p>Java class for PathLink_VersionStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="PathLink_VersionStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}Link_VersionStructure">
 *       &lt;sequence>
 *         &lt;group ref="{http://www.netex.org.uk/netex}PathLinkGroup"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PathLink_VersionStructure", propOrder = {
    "from",
    "to",
    "description",
    "accessibilityAssessmentRef",
    "accessibilityAssessment",
    "accessModes",
    "publicUse",
    "covered",
    "gated",
    "lighting",
    "allAreasWheelchairAccessible",
    "personCapacity",
    "facilities",
    "towards",
    "back",
    "numberOfSteps",
    "allowedUse",
    "transition",
    "accessFeatureType",
    "passageType",
    "maximumFlowPerMinute",
    "transferDuration"
})
@XmlSeeAlso({
    PathLink.class,
    SitePathLink_VersionStructure.class
})
public abstract class PathLink_VersionStructure
    extends Link_VersionStructure
{

    @XmlElement(name = "From", required = true)
    protected PathLinkEndStructure from;
    @XmlElement(name = "To", required = true)
    protected PathLinkEndStructure to;
    @XmlElement(name = "Description")
    protected MultilingualStringEntity description;
    @XmlElement(name = "AccessibilityAssessmentRef")
    protected AccessibilityAssessmentRefStructure accessibilityAssessmentRef;
    @XmlElement(name = "AccessibilityAssessment")
    protected AccessibilityAssessment accessibilityAssessment;
    @XmlList
    @XmlElement(name = "AccessModes")
    @XmlSchemaType(name = "anySimpleType")
    protected List<AccessModeEnumeration> accessModes;
    @XmlElement(name = "PublicUse", defaultValue = "all")
    @XmlSchemaType(name = "string")
    protected PublicUseEnumeration publicUse;
    @XmlElement(name = "Covered", defaultValue = "indoors")
    @XmlSchemaType(name = "string")
    protected CoveredEnumeration covered;
    @XmlElement(name = "Gated")
    @XmlSchemaType(name = "string")
    protected GatedEnumeration gated;
    @XmlElement(name = "Lighting", defaultValue = "wellLit")
    @XmlSchemaType(name = "normalizedString")
    protected LightingEnumeration lighting;
    @XmlElement(name = "AllAreasWheelchairAccessible", defaultValue = "true")
    protected Boolean allAreasWheelchairAccessible;
    @XmlElement(name = "PersonCapacity")
    @XmlSchemaType(name = "nonNegativeInteger")
    protected BigInteger personCapacity;
    protected SiteFacilitySets_RelStructure facilities;
    @XmlElement(name = "Towards")
    protected MultilingualStringEntity towards;
    @XmlElement(name = "Back")
    protected MultilingualStringEntity back;
    @XmlElement(name = "NumberOfSteps")
    @XmlSchemaType(name = "nonNegativeInteger")
    protected BigInteger numberOfSteps;
    @XmlElement(name = "AllowedUse")
    @XmlSchemaType(name = "NMTOKEN")
    protected PathDirectionEnumeration allowedUse;
    @XmlElement(name = "Transition")
    @XmlSchemaType(name = "NMTOKEN")
    protected TransitionEnumeration transition;
    @XmlElement(name = "AccessFeatureType")
    @XmlSchemaType(name = "string")
    protected AccessFeatureEnumeration accessFeatureType;
    @XmlElement(name = "PassageType")
    @XmlSchemaType(name = "string")
    protected PassageTypeEnumeration passageType;
    @XmlElement(name = "MaximumFlowPerMinute")
    @XmlSchemaType(name = "nonNegativeInteger")
    protected BigInteger maximumFlowPerMinute;
    @XmlElement(name = "TransferDuration")
    protected TransferDurationStructure transferDuration;

    /**
     * Gets the value of the from property.
     * 
     * @return
     *     possible object is
     *     {@link PathLinkEndStructure }
     *     
     */
    public PathLinkEndStructure getFrom() {
        return from;
    }

    /**
     * Sets the value of the from property.
     * 
     * @param value
     *     allowed object is
     *     {@link PathLinkEndStructure }
     *     
     */
    public void setFrom(PathLinkEndStructure value) {
        this.from = value;
    }

    /**
     * Gets the value of the to property.
     * 
     * @return
     *     possible object is
     *     {@link PathLinkEndStructure }
     *     
     */
    public PathLinkEndStructure getTo() {
        return to;
    }

    /**
     * Sets the value of the to property.
     * 
     * @param value
     *     allowed object is
     *     {@link PathLinkEndStructure }
     *     
     */
    public void setTo(PathLinkEndStructure value) {
        this.to = value;
    }

    /**
     * Gets the value of the description property.
     * 
     * @return
     *     possible object is
     *     {@link MultilingualStringEntity }
     *     
     */
    public MultilingualStringEntity getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     * 
     * @param value
     *     allowed object is
     *     {@link MultilingualStringEntity }
     *     
     */
    public void setDescription(MultilingualStringEntity value) {
        this.description = value;
    }

    /**
     * Gets the value of the accessibilityAssessmentRef property.
     * 
     * @return
     *     possible object is
     *     {@link AccessibilityAssessmentRefStructure }
     *     
     */
    public AccessibilityAssessmentRefStructure getAccessibilityAssessmentRef() {
        return accessibilityAssessmentRef;
    }

    /**
     * Sets the value of the accessibilityAssessmentRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link AccessibilityAssessmentRefStructure }
     *     
     */
    public void setAccessibilityAssessmentRef(AccessibilityAssessmentRefStructure value) {
        this.accessibilityAssessmentRef = value;
    }

    /**
     * Gets the value of the accessibilityAssessment property.
     * 
     * @return
     *     possible object is
     *     {@link AccessibilityAssessment }
     *     
     */
    public AccessibilityAssessment getAccessibilityAssessment() {
        return accessibilityAssessment;
    }

    /**
     * Sets the value of the accessibilityAssessment property.
     * 
     * @param value
     *     allowed object is
     *     {@link AccessibilityAssessment }
     *     
     */
    public void setAccessibilityAssessment(AccessibilityAssessment value) {
        this.accessibilityAssessment = value;
    }

    /**
     * Gets the value of the accessModes property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the accessModes property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAccessModes().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AccessModeEnumeration }
     * 
     * 
     */
    public List<AccessModeEnumeration> getAccessModes() {
        if (accessModes == null) {
            accessModes = new ArrayList<AccessModeEnumeration>();
        }
        return this.accessModes;
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
     * Gets the value of the towards property.
     * 
     * @return
     *     possible object is
     *     {@link MultilingualStringEntity }
     *     
     */
    public MultilingualStringEntity getTowards() {
        return towards;
    }

    /**
     * Sets the value of the towards property.
     * 
     * @param value
     *     allowed object is
     *     {@link MultilingualStringEntity }
     *     
     */
    public void setTowards(MultilingualStringEntity value) {
        this.towards = value;
    }

    /**
     * Gets the value of the back property.
     * 
     * @return
     *     possible object is
     *     {@link MultilingualStringEntity }
     *     
     */
    public MultilingualStringEntity getBack() {
        return back;
    }

    /**
     * Sets the value of the back property.
     * 
     * @param value
     *     allowed object is
     *     {@link MultilingualStringEntity }
     *     
     */
    public void setBack(MultilingualStringEntity value) {
        this.back = value;
    }

    /**
     * Gets the value of the numberOfSteps property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getNumberOfSteps() {
        return numberOfSteps;
    }

    /**
     * Sets the value of the numberOfSteps property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setNumberOfSteps(BigInteger value) {
        this.numberOfSteps = value;
    }

    /**
     * Gets the value of the allowedUse property.
     * 
     * @return
     *     possible object is
     *     {@link PathDirectionEnumeration }
     *     
     */
    public PathDirectionEnumeration getAllowedUse() {
        return allowedUse;
    }

    /**
     * Sets the value of the allowedUse property.
     * 
     * @param value
     *     allowed object is
     *     {@link PathDirectionEnumeration }
     *     
     */
    public void setAllowedUse(PathDirectionEnumeration value) {
        this.allowedUse = value;
    }

    /**
     * Gets the value of the transition property.
     * 
     * @return
     *     possible object is
     *     {@link TransitionEnumeration }
     *     
     */
    public TransitionEnumeration getTransition() {
        return transition;
    }

    /**
     * Sets the value of the transition property.
     * 
     * @param value
     *     allowed object is
     *     {@link TransitionEnumeration }
     *     
     */
    public void setTransition(TransitionEnumeration value) {
        this.transition = value;
    }

    /**
     * Gets the value of the accessFeatureType property.
     * 
     * @return
     *     possible object is
     *     {@link AccessFeatureEnumeration }
     *     
     */
    public AccessFeatureEnumeration getAccessFeatureType() {
        return accessFeatureType;
    }

    /**
     * Sets the value of the accessFeatureType property.
     * 
     * @param value
     *     allowed object is
     *     {@link AccessFeatureEnumeration }
     *     
     */
    public void setAccessFeatureType(AccessFeatureEnumeration value) {
        this.accessFeatureType = value;
    }

    /**
     * Gets the value of the passageType property.
     * 
     * @return
     *     possible object is
     *     {@link PassageTypeEnumeration }
     *     
     */
    public PassageTypeEnumeration getPassageType() {
        return passageType;
    }

    /**
     * Sets the value of the passageType property.
     * 
     * @param value
     *     allowed object is
     *     {@link PassageTypeEnumeration }
     *     
     */
    public void setPassageType(PassageTypeEnumeration value) {
        this.passageType = value;
    }

    /**
     * Gets the value of the maximumFlowPerMinute property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getMaximumFlowPerMinute() {
        return maximumFlowPerMinute;
    }

    /**
     * Sets the value of the maximumFlowPerMinute property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setMaximumFlowPerMinute(BigInteger value) {
        this.maximumFlowPerMinute = value;
    }

    /**
     * Gets the value of the transferDuration property.
     * 
     * @return
     *     possible object is
     *     {@link TransferDurationStructure }
     *     
     */
    public TransferDurationStructure getTransferDuration() {
        return transferDuration;
    }

    /**
     * Sets the value of the transferDuration property.
     * 
     * @param value
     *     allowed object is
     *     {@link TransferDurationStructure }
     *     
     */
    public void setTransferDuration(TransferDurationStructure value) {
        this.transferDuration = value;
    }

}
