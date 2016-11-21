package org.rutebanken.tiamat.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;

@Entity
public class StopPlace
        extends Site_VersionStructure implements Serializable {

    protected String publicCode;

    @Enumerated(EnumType.STRING)
    protected VehicleModeEnumeration transportMode;

    @Enumerated(EnumType.STRING)
    protected AirSubmodeEnumeration airSubmode = AirSubmodeEnumeration.UNKNOWN;

    @Enumerated(EnumType.STRING)
    protected BusSubmodeEnumeration busSubmode;

    @Enumerated(EnumType.STRING)
    protected CoachSubmodeEnumeration coachSubmode;

    @Enumerated(EnumType.STRING)
    protected FunicularSubmodeEnumeration funicularSubmode;

    @Enumerated(EnumType.STRING)
    protected MetroSubmodeEnumeration metroSubmode;

    @Enumerated(EnumType.STRING)
    protected TramSubmodeEnumeration tramSubmode;

    @Enumerated(EnumType.STRING)
    protected TelecabinSubmodeEnumeration telecabinSubmode;

    @Enumerated(EnumType.STRING)
    protected RailSubmodeEnumeration railSubmode;

    @Enumerated(EnumType.STRING)
    protected WaterSubmodeEnumeration waterSubmode;

    @ElementCollection(targetClass = VehicleModeEnumeration.class)
    @Enumerated(EnumType.STRING)
    @Transient
    protected List<VehicleModeEnumeration> otherTransportModes;

    @Enumerated(EnumType.STRING)
    protected StopTypeEnumeration stopPlaceType;

    protected Boolean borderCrossing;

//    @Embedded
//    protected ExplicitEquipments_RelStructure unlocalisedEquipments;

    @OneToOne
    @Transient
    protected TopographicPlaceRefs_RelStructure servedPlaces;

    @OneToOne
    @Transient
    protected TopographicPlaceRefs_RelStructure mainTerminusForPlaces;

    @Enumerated(EnumType.STRING)
    protected LimitedUseTypeEnumeration limitedUse;

    @Enumerated(value = EnumType.STRING)
    protected InterchangeWeightingEnumeration weighting;

    @OneToMany(cascade = CascadeType.MERGE)
    private Set<Quay> quays = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<AccessSpace> accessSpaces = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY)
    @Transient
    protected SitePathLinks_RelStructure pathLinks;

    @OneToOne(fetch = FetchType.LAZY)
    @Transient
    protected PathJunctions_RelStructure pathJunctions;

    @OneToOne(fetch = FetchType.LAZY)
    @Transient
    protected Accesses_RelStructure accesses;

    @OneToOne(fetch = FetchType.LAZY)
    @Transient
    protected NavigationPaths_RelStructure navigationPaths;

    @OneToOne(fetch = FetchType.LAZY)
    @Transient
    protected VehicleStoppingPlaces_RelStructure vehicleStoppingPlaces;

    public StopPlace(EmbeddableMultilingualString name) {
        super(name);
    }

    public StopPlace() {
    }

    public String getPublicCode() {
        return publicCode;
    }

    public void setPublicCode(String value) {
        this.publicCode = value;
    }

    public VehicleModeEnumeration getTransportMode() {
        return transportMode;
    }

    public void setTransportMode(VehicleModeEnumeration value) {
        this.transportMode = value;
    }

    public AirSubmodeEnumeration getAirSubmode() {
        return airSubmode;
    }

    public void setAirSubmode(AirSubmodeEnumeration value) {
        this.airSubmode = value;
    }

    public BusSubmodeEnumeration getBusSubmode() {
        return busSubmode;
    }

    public void setBusSubmode(BusSubmodeEnumeration value) {
        this.busSubmode = value;
    }

    public CoachSubmodeEnumeration getCoachSubmode() {
        return coachSubmode;
    }

    public void setCoachSubmode(CoachSubmodeEnumeration value) {
        this.coachSubmode = value;
    }

    public FunicularSubmodeEnumeration getFunicularSubmode() {
        return funicularSubmode;
    }

    public void setFunicularSubmode(FunicularSubmodeEnumeration value) {
        this.funicularSubmode = value;
    }

    public MetroSubmodeEnumeration getMetroSubmode() {
        return metroSubmode;
    }

    public void setMetroSubmode(MetroSubmodeEnumeration value) {
        this.metroSubmode = value;
    }

    public TramSubmodeEnumeration getTramSubmode() {
        return tramSubmode;
    }

    public void setTramSubmode(TramSubmodeEnumeration value) {
        this.tramSubmode = value;
    }

    public TelecabinSubmodeEnumeration getTelecabinSubmode() {
        return telecabinSubmode;
    }

    public void setTelecabinSubmode(TelecabinSubmodeEnumeration value) {
        this.telecabinSubmode = value;
    }

    public RailSubmodeEnumeration getRailSubmode() {
        return railSubmode;
    }

    public void setRailSubmode(RailSubmodeEnumeration value) {
        this.railSubmode = value;
    }

    public WaterSubmodeEnumeration getWaterSubmode() {
        return waterSubmode;
    }

    public void setWaterSubmode(WaterSubmodeEnumeration value) {
        this.waterSubmode = value;
    }

    @JsonIgnore
    public List<VehicleModeEnumeration> getOtherTransportModes() {
        if (otherTransportModes == null) {
            otherTransportModes = new ArrayList<VehicleModeEnumeration>();
        }
        return this.otherTransportModes;
    }

    public StopTypeEnumeration getStopPlaceType() {
        return stopPlaceType;
    }

    public void setStopPlaceType(StopTypeEnumeration value) {
        this.stopPlaceType = value;
    }

    public Boolean isBorderCrossing() {
        return borderCrossing;
    }

    public void setBorderCrossing(Boolean value) {
        this.borderCrossing = value;
    }

    public TopographicPlaceRefs_RelStructure getServedPlaces() {
        return servedPlaces;
    }

    public void setServedPlaces(TopographicPlaceRefs_RelStructure value) {
        this.servedPlaces = value;
    }

    public TopographicPlaceRefs_RelStructure getMainTerminusForPlaces() {
        return mainTerminusForPlaces;
    }

    public void setMainTerminusForPlaces(TopographicPlaceRefs_RelStructure value) {
        this.mainTerminusForPlaces = value;
    }

    public LimitedUseTypeEnumeration getLimitedUse() {
        return limitedUse;
    }


    public void setLimitedUse(LimitedUseTypeEnumeration value) {
        this.limitedUse = value;
    }

    public InterchangeWeightingEnumeration getWeighting() {
        return weighting;
    }

    public void setWeighting(InterchangeWeightingEnumeration value) {
        this.weighting = value;
    }

    public SitePathLinks_RelStructure getPathLinks() {
        return pathLinks;
    }

    public void setPathLinks(SitePathLinks_RelStructure value) {
        this.pathLinks = value;
    }

    public PathJunctions_RelStructure getPathJunctions() {
        return pathJunctions;
    }

    public void setPathJunctions(PathJunctions_RelStructure value) {
        this.pathJunctions = value;
    }

    public Accesses_RelStructure getAccesses() {
        return accesses;
    }

    public void setAccesses(Accesses_RelStructure value) {
        this.accesses = value;
    }

    public NavigationPaths_RelStructure getNavigationPaths() {
        return navigationPaths;
    }

    public void setNavigationPaths(NavigationPaths_RelStructure value) {
        this.navigationPaths = value;
    }

    public VehicleStoppingPlaces_RelStructure getVehicleStoppingPlaces() {
        return vehicleStoppingPlaces;
    }

    public void setVehicleStoppingPlaces(VehicleStoppingPlaces_RelStructure value) {
        this.vehicleStoppingPlaces = value;
    }

    public List<AccessSpace> getAccessSpaces() {
        return accessSpaces;
    }


    public Set<Quay> getQuays() {
        return quays;
    }

    public void setQuays(Set<Quay> quays) {
        this.quays = quays;
    }

    @Override
    public boolean equals(Object object) {
        if(this == object) {
            return true;
        } else if (!(object instanceof StopPlace)) {
            return false;
        }

        StopPlace other = (StopPlace) object;

        // Could have used super
        return Objects.equals(this.name, other.name)
                && Objects.equals(this.centroid, other.centroid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, centroid);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("id", id)
                .add("name", name)
                .add("quays", quays)
                .add("centroid", centroid)
                .add("keyValues", getKeyValues())
                .toString();
    }
}
