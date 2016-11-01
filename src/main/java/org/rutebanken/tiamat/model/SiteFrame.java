package org.rutebanken.tiamat.model;

import com.google.common.base.MoreObjects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

public class SiteFrame
        extends Common_VersionFrameStructure {

    protected TopographicPlacesInFrame_RelStructure topographicPlaces = new TopographicPlacesInFrame_RelStructure();
    protected AddressesInFrame_RelStructure addresses;
    protected AccessesInFrame_RelStructure accesses;
    protected StopPlacesInFrame_RelStructure stopPlaces;
    protected FlexibleStopPlacesInFrame_RelStructure flexibleStopPlaces;
    protected PointsOfInterestInFrame_RelStructure pointsOfInterest;
    protected ParkingsInFrame_RelStructure parkings;
    protected NavigationPathsInFrame_RelStructure navigationPaths;
    protected PathLinksInFrame_RelStructure pathLinks;
    protected PathJunctionsInFrame_RelStructure pathJunctions;
    protected CheckConstraintInFrame_RelStructure checkConstraints;
    protected CheckConstraintDelaysInFrame_RelStructure checkConstraintDelays;
    protected CheckConstraintThroughputsInFrame_RelStructure checkConstraintThroughputs;
    protected PointOfInterestClassifications pointOfInterestClassifications;
    protected PointOfInterestClassificationHierarchiesInFrame_RelStructure pointOfInterestClassificationHierarchies;
    protected TariffZonesInFrame_RelStructure tariffZones;
    protected SiteFacilitySetsInFrame_RelStructure siteFacilitySets;

    /**
     * Gets the value of the topographicPlaces property.
     *
     * @return possible object is
     * {@link TopographicPlacesInFrame_RelStructure }
     */
    public TopographicPlacesInFrame_RelStructure getTopographicPlaces() {
        return topographicPlaces;
    }

    /**
     * Sets the value of the topographicPlaces property.
     *
     * @param value allowed object is
     *              {@link TopographicPlacesInFrame_RelStructure }
     */
    public void setTopographicPlaces(TopographicPlacesInFrame_RelStructure value) {
        this.topographicPlaces = value;
    }

    /**
     * Gets the value of the addresses property.
     *
     * @return possible object is
     * {@link AddressesInFrame_RelStructure }
     */
    public AddressesInFrame_RelStructure getAddresses() {
        return addresses;
    }

    /**
     * Sets the value of the addresses property.
     *
     * @param value allowed object is
     *              {@link AddressesInFrame_RelStructure }
     */
    public void setAddresses(AddressesInFrame_RelStructure value) {
        this.addresses = value;
    }

    /**
     * Gets the value of the accesses property.
     *
     * @return possible object is
     * {@link AccessesInFrame_RelStructure }
     */
    public AccessesInFrame_RelStructure getAccesses() {
        return accesses;
    }

    /**
     * Sets the value of the accesses property.
     *
     * @param value allowed object is
     *              {@link AccessesInFrame_RelStructure }
     */
    public void setAccesses(AccessesInFrame_RelStructure value) {
        this.accesses = value;
    }

    /**
     * Gets the value of the stopPlaces property.
     *
     * @return possible object is
     * {@link StopPlacesInFrame_RelStructure }
     */
    public StopPlacesInFrame_RelStructure getStopPlaces() {
        return stopPlaces;
    }

    /**
     * Sets the value of the stopPlaces property.
     *
     * @param value allowed object is
     *              {@link StopPlacesInFrame_RelStructure }
     */
    public void setStopPlaces(StopPlacesInFrame_RelStructure value) {
        this.stopPlaces = value;
    }

    /**
     * Gets the value of the flexibleStopPlaces property.
     *
     * @return possible object is
     * {@link FlexibleStopPlacesInFrame_RelStructure }
     */
    public FlexibleStopPlacesInFrame_RelStructure getFlexibleStopPlaces() {
        return flexibleStopPlaces;
    }

    /**
     * Sets the value of the flexibleStopPlaces property.
     *
     * @param value allowed object is
     *              {@link FlexibleStopPlacesInFrame_RelStructure }
     */
    public void setFlexibleStopPlaces(FlexibleStopPlacesInFrame_RelStructure value) {
        this.flexibleStopPlaces = value;
    }

    /**
     * Gets the value of the pointsOfInterest property.
     *
     * @return possible object is
     * {@link PointsOfInterestInFrame_RelStructure }
     */
    public PointsOfInterestInFrame_RelStructure getPointsOfInterest() {
        return pointsOfInterest;
    }

    /**
     * Sets the value of the pointsOfInterest property.
     *
     * @param value allowed object is
     *              {@link PointsOfInterestInFrame_RelStructure }
     */
    public void setPointsOfInterest(PointsOfInterestInFrame_RelStructure value) {
        this.pointsOfInterest = value;
    }

    /**
     * Gets the value of the parkings property.
     *
     * @return possible object is
     * {@link ParkingsInFrame_RelStructure }
     */
    public ParkingsInFrame_RelStructure getParkings() {
        return parkings;
    }

    /**
     * Sets the value of the parkings property.
     *
     * @param value allowed object is
     *              {@link ParkingsInFrame_RelStructure }
     */
    public void setParkings(ParkingsInFrame_RelStructure value) {
        this.parkings = value;
    }

    /**
     * Gets the value of the navigationPaths property.
     *
     * @return possible object is
     * {@link NavigationPathsInFrame_RelStructure }
     */
    public NavigationPathsInFrame_RelStructure getNavigationPaths() {
        return navigationPaths;
    }

    /**
     * Sets the value of the navigationPaths property.
     *
     * @param value allowed object is
     *              {@link NavigationPathsInFrame_RelStructure }
     */
    public void setNavigationPaths(NavigationPathsInFrame_RelStructure value) {
        this.navigationPaths = value;
    }

    /**
     * Gets the value of the pathLinks property.
     *
     * @return possible object is
     * {@link PathLinksInFrame_RelStructure }
     */
    public PathLinksInFrame_RelStructure getPathLinks() {
        return pathLinks;
    }

    /**
     * Sets the value of the pathLinks property.
     *
     * @param value allowed object is
     *              {@link PathLinksInFrame_RelStructure }
     */
    public void setPathLinks(PathLinksInFrame_RelStructure value) {
        this.pathLinks = value;
    }

    /**
     * Gets the value of the pathJunctions property.
     *
     * @return possible object is
     * {@link PathJunctionsInFrame_RelStructure }
     */
    public PathJunctionsInFrame_RelStructure getPathJunctions() {
        return pathJunctions;
    }

    /**
     * Sets the value of the pathJunctions property.
     *
     * @param value allowed object is
     *              {@link PathJunctionsInFrame_RelStructure }
     */
    public void setPathJunctions(PathJunctionsInFrame_RelStructure value) {
        this.pathJunctions = value;
    }

    /**
     * Gets the value of the checkConstraints property.
     *
     * @return possible object is
     * {@link CheckConstraintInFrame_RelStructure }
     */
    public CheckConstraintInFrame_RelStructure getCheckConstraints() {
        return checkConstraints;
    }

    /**
     * Sets the value of the checkConstraints property.
     *
     * @param value allowed object is
     *              {@link CheckConstraintInFrame_RelStructure }
     */
    public void setCheckConstraints(CheckConstraintInFrame_RelStructure value) {
        this.checkConstraints = value;
    }

    /**
     * Gets the value of the checkConstraintDelays property.
     *
     * @return possible object is
     * {@link CheckConstraintDelaysInFrame_RelStructure }
     */
    public CheckConstraintDelaysInFrame_RelStructure getCheckConstraintDelays() {
        return checkConstraintDelays;
    }

    /**
     * Sets the value of the checkConstraintDelays property.
     *
     * @param value allowed object is
     *              {@link CheckConstraintDelaysInFrame_RelStructure }
     */
    public void setCheckConstraintDelays(CheckConstraintDelaysInFrame_RelStructure value) {
        this.checkConstraintDelays = value;
    }

    /**
     * Gets the value of the checkConstraintThroughputs property.
     *
     * @return possible object is
     * {@link CheckConstraintThroughputsInFrame_RelStructure }
     */
    public CheckConstraintThroughputsInFrame_RelStructure getCheckConstraintThroughputs() {
        return checkConstraintThroughputs;
    }

    /**
     * Sets the value of the checkConstraintThroughputs property.
     *
     * @param value allowed object is
     *              {@link CheckConstraintThroughputsInFrame_RelStructure }
     */
    public void setCheckConstraintThroughputs(CheckConstraintThroughputsInFrame_RelStructure value) {
        this.checkConstraintThroughputs = value;
    }

    /**
     * Gets the value of the pointOfInterestClassifications property.
     *
     * @return possible object is
     * {@link PointOfInterestClassifications }
     */
    public PointOfInterestClassifications getPointOfInterestClassifications() {
        return pointOfInterestClassifications;
    }

    /**
     * Sets the value of the pointOfInterestClassifications property.
     *
     * @param value allowed object is
     *              {@link PointOfInterestClassifications }
     */
    public void setPointOfInterestClassifications(PointOfInterestClassifications value) {
        this.pointOfInterestClassifications = value;
    }

    /**
     * Gets the value of the pointOfInterestClassificationHierarchies property.
     *
     * @return possible object is
     * {@link PointOfInterestClassificationHierarchiesInFrame_RelStructure }
     */
    public PointOfInterestClassificationHierarchiesInFrame_RelStructure getPointOfInterestClassificationHierarchies() {
        return pointOfInterestClassificationHierarchies;
    }

    /**
     * Sets the value of the pointOfInterestClassificationHierarchies property.
     *
     * @param value allowed object is
     *              {@link PointOfInterestClassificationHierarchiesInFrame_RelStructure }
     */
    public void setPointOfInterestClassificationHierarchies(PointOfInterestClassificationHierarchiesInFrame_RelStructure value) {
        this.pointOfInterestClassificationHierarchies = value;
    }

    /**
     * Gets the value of the tariffZones property.
     *
     * @return possible object is
     * {@link TariffZonesInFrame_RelStructure }
     */
    public TariffZonesInFrame_RelStructure getTariffZones() {
        return tariffZones;
    }

    /**
     * Sets the value of the tariffZones property.
     *
     * @param value allowed object is
     *              {@link TariffZonesInFrame_RelStructure }
     */
    public void setTariffZones(TariffZonesInFrame_RelStructure value) {
        this.tariffZones = value;
    }

    /**
     * Gets the value of the siteFacilitySets property.
     *
     * @return possible object is
     * {@link SiteFacilitySetsInFrame_RelStructure }
     */
    public SiteFacilitySetsInFrame_RelStructure getSiteFacilitySets() {
        return siteFacilitySets;
    }

    /**
     * Sets the value of the siteFacilitySets property.
     *
     * @param value allowed object is
     *              {@link SiteFacilitySetsInFrame_RelStructure }
     */
    public void setSiteFacilitySets(SiteFacilitySetsInFrame_RelStructure value) {
        this.siteFacilitySets = value;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("id", id)
                .add("name", name)
                .add("topoGraphicPlaces", getTopographicPlaces() != null && getTopographicPlaces().getTopographicPlace() != null ? getTopographicPlaces().getTopographicPlace().size() : 0)
                .add("stops", getStopPlaces() != null && getStopPlaces().getStopPlace() != null ? getStopPlaces().getStopPlace().size() : 0)
                .add("keyValues", getKeyValues())
                .toString();
    }
}
