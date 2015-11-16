//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.06 at 10:37:32 AM CET 
//


package uk.org.netex.netex;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.NormalizedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * Type for a Version of a STOP PLACE.
 * 
 * <p>Java class for StopPlace_VersionStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="StopPlace_VersionStructure">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.netex.org.uk/netex}Site_VersionStructure">
 *       &lt;sequence>
 *         &lt;group ref="{http://www.netex.org.uk/netex}StopPlaceGroup"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "StopPlace_VersionStructure", propOrder = {
    "publicCode",
    "transportMode",
    "airSubmode",
    "busSubmode",
    "coachSubmode",
    "funicularSubmode",
    "metroSubmode",
    "tramSubmode",
    "telecabinSubmode",
    "railSubmode",
    "waterSubmode",
    "otherTransportModes",
    "tariffZones",
    "stopPlaceType",
    "borderCrossing",
    "unlocalisedEquipments",
    "servedPlaces",
    "mainTerminusForPlaces",
    "limitedUse",
    "weighting",
    "quays",
    "accessSpaces",
    "pathLinks",
    "pathJunctions",
    "accesses",
    "navigationPaths",
    "vehicleStoppingPlaces"
})
@XmlSeeAlso({
    StopPlace.class
})
@MappedSuperclass
public class StopPlace_VersionStructure
    extends Site_VersionStructure
{

    @XmlElement(name = "PublicCode")
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    @XmlSchemaType(name = "normalizedString")
    protected String publicCode;

    @XmlElement(name = "TransportMode")
    @XmlSchemaType(name = "NMTOKEN")
    protected VehicleModeEnumeration transportMode;

    @XmlElement(name = "AirSubmode", defaultValue = "unknown")
    @XmlSchemaType(name = "NMTOKEN")
    @Enumerated(EnumType.STRING)
    protected AirSubmodeEnumeration airSubmode = AirSubmodeEnumeration.UNKNOWN;

    @XmlElement(name = "BusSubmode", defaultValue = "unknown")
    @XmlSchemaType(name = "NMTOKEN")
    @Enumerated(EnumType.STRING)
    protected BusSubmodeEnumeration busSubmode;

    @XmlElement(name = "CoachSubmode", defaultValue = "unknown")
    @XmlSchemaType(name = "NMTOKEN")
    @Enumerated(EnumType.STRING)
    protected CoachSubmodeEnumeration coachSubmode;

    @XmlElement(name = "FunicularSubmode", defaultValue = "unknown")
    @XmlSchemaType(name = "NMTOKEN")
    @Enumerated(EnumType.STRING)
    protected FunicularSubmodeEnumeration funicularSubmode;

    @XmlElement(name = "MetroSubmode", defaultValue = "unknown")
    @XmlSchemaType(name = "NMTOKEN")
    @Enumerated(EnumType.STRING)
    protected MetroSubmodeEnumeration metroSubmode;

    @XmlElement(name = "TramSubmode", defaultValue = "unknown")
    @XmlSchemaType(name = "NMTOKEN")
    @Enumerated(EnumType.STRING)
    protected TramSubmodeEnumeration tramSubmode;

    @XmlElement(name = "TelecabinSubmode", defaultValue = "unknown")
    @XmlSchemaType(name = "NMTOKEN")
    @Enumerated(EnumType.STRING)
    @Transient
    protected TelecabinSubmodeEnumeration telecabinSubmode;

    @XmlElement(name = "RailSubmode", defaultValue = "unknown")
    @XmlSchemaType(name = "NMTOKEN")
    @Enumerated(EnumType.STRING)
    protected RailSubmodeEnumeration railSubmode;

    @XmlElement(name = "WaterSubmode", defaultValue = "unknown")
    @XmlSchemaType(name = "NMTOKEN")
    @Enumerated(EnumType.STRING)
    protected WaterSubmodeEnumeration waterSubmode;

    @XmlList
    @XmlElement(name = "OtherTransportModes")
    @XmlSchemaType(name = "anySimpleType")
    @ElementCollection(targetClass = VehicleModeEnumeration.class, fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    protected List<VehicleModeEnumeration> otherTransportModes;

    //@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @ElementCollection(fetch = FetchType.EAGER)
    private List<TariffZoneRef> tariffZones;

    @XmlElement(name = "StopPlaceType")
    @XmlSchemaType(name = "string")
    @Enumerated(EnumType.STRING)
    protected StopTypeEnumeration stopPlaceType;

    @XmlElement(name = "BorderCrossing", defaultValue = "false")
    protected Boolean borderCrossing;

//    @Embedded
//    protected ExplicitEquipments_RelStructure unlocalisedEquipments;

    @Embedded
    private StopPlaceReference parentStopPlaceReference;

    @OneToOne
    @Transient
    protected TopographicPlaceRefs_RelStructure servedPlaces;

    @OneToOne
    @Transient
    protected TopographicPlaceRefs_RelStructure mainTerminusForPlaces;

    @XmlElement(name = "LimitedUse")
    @XmlSchemaType(name = "string")
    @Enumerated(EnumType.STRING)
    protected LimitedUseTypeEnumeration limitedUse;

    @XmlElement(name = "Weighting")
    @XmlSchemaType(name = "string")
    @Enumerated(value = EnumType.STRING)
    protected InterchangeWeightingEnumeration weighting;

//    @OneToOne
//    protected Quays_RelStructure quays;

//    @OneToOne
//    protected AccessSpaces_RelStructure accessSpaces;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<AccessSpaceRefStructure> accessSpaces;

    @OneToOne
    protected SitePathLinks_RelStructure pathLinks;

    @OneToOne
    protected PathJunctions_RelStructure pathJunctions;

    @OneToOne
    protected Accesses_RelStructure accesses;

    @OneToOne
    protected NavigationPaths_RelStructure navigationPaths;

    @OneToOne
    protected VehicleStoppingPlaces_RelStructure vehicleStoppingPlaces;


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

    /**
     * Gets the value of the airSubmode property.
     * 
     * @return
     *     possible object is
     *     {@link AirSubmodeEnumeration }
     *     
     */
    public AirSubmodeEnumeration getAirSubmode() {
        return airSubmode;
    }

    /**
     * Sets the value of the airSubmode property.
     * 
     * @param value
     *     allowed object is
     *     {@link AirSubmodeEnumeration }
     *     
     */
    public void setAirSubmode(AirSubmodeEnumeration value) {
        this.airSubmode = value;
    }

    /**
     * Gets the value of the busSubmode property.
     * 
     * @return
     *     possible object is
     *     {@link BusSubmodeEnumeration }
     *     
     */
    public BusSubmodeEnumeration getBusSubmode() {
        return busSubmode;
    }

    /**
     * Sets the value of the busSubmode property.
     * 
     * @param value
     *     allowed object is
     *     {@link BusSubmodeEnumeration }
     *     
     */
    public void setBusSubmode(BusSubmodeEnumeration value) {
        this.busSubmode = value;
    }

    /**
     * Gets the value of the coachSubmode property.
     * 
     * @return
     *     possible object is
     *     {@link CoachSubmodeEnumeration }
     *     
     */
    public CoachSubmodeEnumeration getCoachSubmode() {
        return coachSubmode;
    }

    /**
     * Sets the value of the coachSubmode property.
     * 
     * @param value
     *     allowed object is
     *     {@link CoachSubmodeEnumeration }
     *     
     */
    public void setCoachSubmode(CoachSubmodeEnumeration value) {
        this.coachSubmode = value;
    }

    /**
     * Gets the value of the funicularSubmode property.
     * 
     * @return
     *     possible object is
     *     {@link FunicularSubmodeEnumeration }
     *     
     */
    public FunicularSubmodeEnumeration getFunicularSubmode() {
        return funicularSubmode;
    }

    /**
     * Sets the value of the funicularSubmode property.
     * 
     * @param value
     *     allowed object is
     *     {@link FunicularSubmodeEnumeration }
     *     
     */
    public void setFunicularSubmode(FunicularSubmodeEnumeration value) {
        this.funicularSubmode = value;
    }

    /**
     * Gets the value of the metroSubmode property.
     * 
     * @return
     *     possible object is
     *     {@link MetroSubmodeEnumeration }
     *     
     */
    public MetroSubmodeEnumeration getMetroSubmode() {
        return metroSubmode;
    }

    /**
     * Sets the value of the metroSubmode property.
     * 
     * @param value
     *     allowed object is
     *     {@link MetroSubmodeEnumeration }
     *     
     */
    public void setMetroSubmode(MetroSubmodeEnumeration value) {
        this.metroSubmode = value;
    }

    /**
     * Gets the value of the tramSubmode property.
     * 
     * @return
     *     possible object is
     *     {@link TramSubmodeEnumeration }
     *     
     */
    public TramSubmodeEnumeration getTramSubmode() {
        return tramSubmode;
    }

    /**
     * Sets the value of the tramSubmode property.
     * 
     * @param value
     *     allowed object is
     *     {@link TramSubmodeEnumeration }
     *     
     */
    public void setTramSubmode(TramSubmodeEnumeration value) {
        this.tramSubmode = value;
    }

    /**
     * Gets the value of the telecabinSubmode property.
     * 
     * @return
     *     possible object is
     *     {@link TelecabinSubmodeEnumeration }
     *     
     */
    public TelecabinSubmodeEnumeration getTelecabinSubmode() {
        return telecabinSubmode;
    }

    /**
     * Sets the value of the telecabinSubmode property.
     * 
     * @param value
     *     allowed object is
     *     {@link TelecabinSubmodeEnumeration }
     *     
     */
    public void setTelecabinSubmode(TelecabinSubmodeEnumeration value) {
        this.telecabinSubmode = value;
    }

    /**
     * Gets the value of the railSubmode property.
     * 
     * @return
     *     possible object is
     *     {@link RailSubmodeEnumeration }
     *     
     */
    public RailSubmodeEnumeration getRailSubmode() {
        return railSubmode;
    }

    /**
     * Sets the value of the railSubmode property.
     * 
     * @param value
     *     allowed object is
     *     {@link RailSubmodeEnumeration }
     *     
     */
    public void setRailSubmode(RailSubmodeEnumeration value) {
        this.railSubmode = value;
    }

    /**
     * Gets the value of the waterSubmode property.
     * 
     * @return
     *     possible object is
     *     {@link WaterSubmodeEnumeration }
     *     
     */
    public WaterSubmodeEnumeration getWaterSubmode() {
        return waterSubmode;
    }

    /**
     * Sets the value of the waterSubmode property.
     * 
     * @param value
     *     allowed object is
     *     {@link WaterSubmodeEnumeration }
     *     
     */
    public void setWaterSubmode(WaterSubmodeEnumeration value) {
        this.waterSubmode = value;
    }

    /**
     * Gets the value of the otherTransportModes property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the otherTransportModes property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getOtherTransportModes().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link VehicleModeEnumeration }
     * 
     * 
     */
    @JsonIgnore
    public List<VehicleModeEnumeration> getOtherTransportModes() {
        if (otherTransportModes == null) {
            otherTransportModes = new ArrayList<VehicleModeEnumeration>();
        }
        return this.otherTransportModes;
    }

    /**
     * Gets the value of the tariffZones property.
     * 
     * @return
     *     possible object is
     *     {@link TariffZoneRefs_RelStructure }
     *     
     */
   /* public TariffZoneRefs_RelStructure getTariffZones() {
        return tariffZones;
    }*/

    /**
     * Sets the value of the tariffZones property.
     * 
     * @param value
     *     allowed object is
     *     {@link TariffZoneRefs_RelStructure }
     *     
     */
  /*  public void setTariffZones(TariffZoneRefs_RelStructure value) {
        this.tariffZones = value;
    }*/

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
     * Gets the value of the borderCrossing property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isBorderCrossing() {
        return borderCrossing;
    }

    /**
     * Sets the value of the borderCrossing property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setBorderCrossing(Boolean value) {
        this.borderCrossing = value;
    }

    /**
     * Gets the value of the unlocalisedEquipments property.
     * 
     * @return
     *     possible object is
     *     {@link ExplicitEquipments_RelStructure }
     *     
     */
/*    public ExplicitEquipments_RelStructure getUnlocalisedEquipments() {
        return unlocalisedEquipments;
    }
*/
    /**
     * Sets the value of the unlocalisedEquipments property.
     * 
     * @param value
     *     allowed object is
     *     {@link ExplicitEquipments_RelStructure }
     *     
     */
 /*   public void setUnlocalisedEquipments(ExplicitEquipments_RelStructure value) {
        this.unlocalisedEquipments = value;
    }
*/
    /**
     * Gets the value of the servedPlaces property.
     * 
     * @return
     *     possible object is
     *     {@link TopographicPlaceRefs_RelStructure }
     *     
     */
    public TopographicPlaceRefs_RelStructure getServedPlaces() {
        return servedPlaces;
    }

    /**
     * Sets the value of the servedPlaces property.
     * 
     * @param value
     *     allowed object is
     *     {@link TopographicPlaceRefs_RelStructure }
     *     
     */
    public void setServedPlaces(TopographicPlaceRefs_RelStructure value) {
        this.servedPlaces = value;
    }

    /**
     * Gets the value of the mainTerminusForPlaces property.
     * 
     * @return
     *     possible object is
     *     {@link TopographicPlaceRefs_RelStructure }
     *     
     */
    public TopographicPlaceRefs_RelStructure getMainTerminusForPlaces() {
        return mainTerminusForPlaces;
    }

    /**
     * Sets the value of the mainTerminusForPlaces property.
     * 
     * @param value
     *     allowed object is
     *     {@link TopographicPlaceRefs_RelStructure }
     *     
     */
    public void setMainTerminusForPlaces(TopographicPlaceRefs_RelStructure value) {
        this.mainTerminusForPlaces = value;
    }

    /**
     * Gets the value of the limitedUse property.
     * 
     * @return
     *     possible object is
     *     {@link LimitedUseTypeEnumeration }
     *     
     */
    public LimitedUseTypeEnumeration getLimitedUse() {
        return limitedUse;
    }

    /**
     * Sets the value of the limitedUse property.
     * 
     * @param value
     *     allowed object is
     *     {@link LimitedUseTypeEnumeration }
     *     
     */
    public void setLimitedUse(LimitedUseTypeEnumeration value) {
        this.limitedUse = value;
    }

    /**
     * Gets the value of the weighting property.
     * 
     * @return
     *     possible object is
     *     {@link InterchangeWeightingEnumeration }
     *     
     */
    public InterchangeWeightingEnumeration getWeighting() {
        return weighting;
    }

    /**
     * Sets the value of the weighting property.
     * 
     * @param value
     *     allowed object is
     *     {@link InterchangeWeightingEnumeration }
     *     
     */
    public void setWeighting(InterchangeWeightingEnumeration value) {
        this.weighting = value;
    }

    /**
     * Gets the value of the quays property.
     * 
     * @return
     *     possible object is
     *     {@link Quays_RelStructure }
     *     
     */
/*    public Quays_RelStructure getQuays() {
        return quays;
    }
*/
    /**
     * Sets the value of the quays property.
     * 
     * @param value
     *     allowed object is
     *     {@link Quays_RelStructure }
     *     
     */
  /*  public void setQuays(Quays_RelStructure value) {
        this.quays = value;
    }*/

    /**
     * Gets the value of the accessSpaces property.
     * 
     * @return
     *     possible object is
     *     {@link AccessSpaces_RelStructure }
     *     
//    public AccessSpaces_RelStructure getAccessSpaces() {
//        return accessSpaces;
//    }

    /**
     * Sets the value of the accessSpaces property.
     * 
     * @param value
     *     allowed object is
     *     {@link AccessSpaces_RelStructure }
     *     
     */
 //   public void setAccessSpaces(AccessSpaces_RelStructure value) {
 //       this.accessSpaces = value;
 //   }

    /**
     * Gets the value of the pathLinks property.
     * 
     * @return
     *     possible object is
     *     {@link SitePathLinks_RelStructure }
     *     
     */
    public SitePathLinks_RelStructure getPathLinks() {
        return pathLinks;
    }

    /**
     * Sets the value of the pathLinks property.
     * 
     * @param value
     *     allowed object is
     *     {@link SitePathLinks_RelStructure }
     *     
     */
    public void setPathLinks(SitePathLinks_RelStructure value) {
        this.pathLinks = value;
    }

    /**
     * Gets the value of the pathJunctions property.
     * 
     * @return
     *     possible object is
     *     {@link PathJunctions_RelStructure }
     *     
     */
    public PathJunctions_RelStructure getPathJunctions() {
        return pathJunctions;
    }

    /**
     * Sets the value of the pathJunctions property.
     * 
     * @param value
     *     allowed object is
     *     {@link PathJunctions_RelStructure }
     *     
     */
    public void setPathJunctions(PathJunctions_RelStructure value) {
        this.pathJunctions = value;
    }

    /**
     * Gets the value of the accesses property.
     * 
     * @return
     *     possible object is
     *     {@link Accesses_RelStructure }
     *     
     */
    public Accesses_RelStructure getAccesses() {
        return accesses;
    }

    /**
     * Sets the value of the accesses property.
     * 
     * @param value
     *     allowed object is
     *     {@link Accesses_RelStructure }
     *     
     */
    public void setAccesses(Accesses_RelStructure value) {
        this.accesses = value;
    }

    /**
     * Gets the value of the navigationPaths property.
     * 
     * @return
     *     possible object is
     *     {@link NavigationPaths_RelStructure }
     *     
     */
    public NavigationPaths_RelStructure getNavigationPaths() {
        return navigationPaths;
    }

    /**
     * Sets the value of the navigationPaths property.
     * 
     * @param value
     *     allowed object is
     *     {@link NavigationPaths_RelStructure }
     *     
     */
    public void setNavigationPaths(NavigationPaths_RelStructure value) {
        this.navigationPaths = value;
    }

    /**
     * Gets the value of the vehicleStoppingPlaces property.
     * 
     * @return
     *     possible object is
     *     {@link VehicleStoppingPlaces_RelStructure }
     *     
     */
    public VehicleStoppingPlaces_RelStructure getVehicleStoppingPlaces() {
        return vehicleStoppingPlaces;
    }

    /**
     * Sets the value of the vehicleStoppingPlaces property.
     * 
     * @param value
     *     allowed object is
     *     {@link VehicleStoppingPlaces_RelStructure }
     *     
     */
    public void setVehicleStoppingPlaces(VehicleStoppingPlaces_RelStructure value) {
        this.vehicleStoppingPlaces = value;
    }

    public List<TariffZoneRef> getTariffZones() {
        return tariffZones;
    }

    public void setTariffZones(List<TariffZoneRef> tariffZones) {
        this.tariffZones = tariffZones;
    }

    public StopPlaceReference getParentStopPlaceReference() {
        return parentStopPlaceReference;
    }

    public void setParentStopPlaceReference(StopPlaceReference parentStopPlaceReference) {
        this.parentStopPlaceReference = parentStopPlaceReference;
    }

    public List<AccessSpaceRefStructure> getAccessSpaces() {
        return accessSpaces;
    }

    public void setAccessSpaces(List<AccessSpaceRefStructure> accessSpaces) {
        this.accessSpaces = accessSpaces;
    }
}
