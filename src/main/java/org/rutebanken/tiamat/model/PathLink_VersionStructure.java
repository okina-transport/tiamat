

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
public abstract class PathLink_VersionStructure
    extends Link_VersionStructure
{

    protected PathLinkEndStructure from;
    protected PathLinkEndStructure to;
    protected MultilingualStringEntity description;
    protected AccessibilityAssessmentRefStructure accessibilityAssessmentRef;
    protected AccessibilityAssessment accessibilityAssessment;
    protected List<AccessModeEnumeration> accessModes;
    protected PublicUseEnumeration publicUse;
    protected CoveredEnumeration covered;
    protected GatedEnumeration gated;
    protected LightingEnumeration lighting;
    protected Boolean allAreasWheelchairAccessible;
    protected BigInteger personCapacity;
    protected SiteFacilitySets_RelStructure facilities;
    protected MultilingualStringEntity towards;
    protected MultilingualStringEntity back;
    protected BigInteger numberOfSteps;
    protected PathDirectionEnumeration allowedUse;
    protected TransitionEnumeration transition;
    protected AccessFeatureEnumeration accessFeatureType;
    protected PassageTypeEnumeration passageType;
    protected BigInteger maximumFlowPerMinute;
    protected TransferDurationStructure transferDuration;

    public PathLinkEndStructure getFrom() {
        return from;
    }

    public void setFrom(PathLinkEndStructure value) {
        this.from = value;
    }

    public PathLinkEndStructure getTo() {
        return to;
    }

    public void setTo(PathLinkEndStructure value) {
        this.to = value;
    }

    public MultilingualStringEntity getDescription() {
        return description;
    }

    public void setDescription(MultilingualStringEntity value) {
        this.description = value;
    }

    public AccessibilityAssessmentRefStructure getAccessibilityAssessmentRef() {
        return accessibilityAssessmentRef;
    }

    public void setAccessibilityAssessmentRef(AccessibilityAssessmentRefStructure value) {
        this.accessibilityAssessmentRef = value;
    }

    public AccessibilityAssessment getAccessibilityAssessment() {
        return accessibilityAssessment;
    }

    public void setAccessibilityAssessment(AccessibilityAssessment value) {
        this.accessibilityAssessment = value;
    }

    public List<AccessModeEnumeration> getAccessModes() {
        if (accessModes == null) {
            accessModes = new ArrayList<AccessModeEnumeration>();
        }
        return this.accessModes;
    }

    public PublicUseEnumeration getPublicUse() {
        return publicUse;
    }

    public void setPublicUse(PublicUseEnumeration value) {
        this.publicUse = value;
    }

    public CoveredEnumeration getCovered() {
        return covered;
    }

    public void setCovered(CoveredEnumeration value) {
        this.covered = value;
    }

    public GatedEnumeration getGated() {
        return gated;
    }

    public void setGated(GatedEnumeration value) {
        this.gated = value;
    }

    public LightingEnumeration getLighting() {
        return lighting;
    }

    public void setLighting(LightingEnumeration value) {
        this.lighting = value;
    }

    public Boolean isAllAreasWheelchairAccessible() {
        return allAreasWheelchairAccessible;
    }

    public void setAllAreasWheelchairAccessible(Boolean value) {
        this.allAreasWheelchairAccessible = value;
    }

    public BigInteger getPersonCapacity() {
        return personCapacity;
    }

    public void setPersonCapacity(BigInteger value) {
        this.personCapacity = value;
    }

    public SiteFacilitySets_RelStructure getFacilities() {
        return facilities;
    }

    public void setFacilities(SiteFacilitySets_RelStructure value) {
        this.facilities = value;
    }

    public MultilingualStringEntity getTowards() {
        return towards;
    }

    public void setTowards(MultilingualStringEntity value) {
        this.towards = value;
    }

    public MultilingualStringEntity getBack() {
        return back;
    }

    public void setBack(MultilingualStringEntity value) {
        this.back = value;
    }

    public BigInteger getNumberOfSteps() {
        return numberOfSteps;
    }

    public void setNumberOfSteps(BigInteger value) {
        this.numberOfSteps = value;
    }

    public PathDirectionEnumeration getAllowedUse() {
        return allowedUse;
    }

    public void setAllowedUse(PathDirectionEnumeration value) {
        this.allowedUse = value;
    }

    public TransitionEnumeration getTransition() {
        return transition;
    }

    public void setTransition(TransitionEnumeration value) {
        this.transition = value;
    }

    public AccessFeatureEnumeration getAccessFeatureType() {
        return accessFeatureType;
    }

    public void setAccessFeatureType(AccessFeatureEnumeration value) {
        this.accessFeatureType = value;
    }

    public PassageTypeEnumeration getPassageType() {
        return passageType;
    }

    public void setPassageType(PassageTypeEnumeration value) {
        this.passageType = value;
    }

    public BigInteger getMaximumFlowPerMinute() {
        return maximumFlowPerMinute;
    }

    public void setMaximumFlowPerMinute(BigInteger value) {
        this.maximumFlowPerMinute = value;
    }

    public TransferDurationStructure getTransferDuration() {
        return transferDuration;
    }

    public void setTransferDuration(TransferDurationStructure value) {
        this.transferDuration = value;
    }

}
