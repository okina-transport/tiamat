/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package org.rutebanken.tiamat.model;

import com.google.common.base.MoreObjects;
import jakarta.persistence.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SqlResultSetMapping(
        name = "ParkingMapping",
        entities = @EntityResult(
                entityClass = Parking.class
        )
)
@Entity
public class Parking
        extends Site_VersionStructure {

    @Transient
    protected String publicCode;
    @Transient
    protected MultilingualStringEntity label;
    @Transient
    protected String defaultCurrency;
    @Transient
    protected List<String> currenciesAccepted;
    @Transient
    protected List<String> cardsAccepted;
    @Transient
    protected PaymentByMobileStructure paymentByMobile;
    @Transient
    protected ParkingEntrancesForVehicles_RelStructure vehicleEntrancesNetex;

    @Transient
    protected SitePathLinks_RelStructure pathLinks;
    @Transient
    protected PathJunctions_RelStructure pathJunctions;
    @Transient
    protected NavigationPaths_RelStructure navigationPaths;

    @Enumerated(EnumType.STRING)
    protected ParkingTypeEnumeration parkingType;

    @ElementCollection(targetClass = ParkingPaymentProcessEnumeration.class)
    @LazyCollection(LazyCollectionOption.FALSE)
    @Enumerated(EnumType.STRING)
    protected List<ParkingPaymentProcessEnumeration> parkingPaymentProcess;

    @ElementCollection(targetClass = PaymentMethodEnumeration.class)
    @LazyCollection(LazyCollectionOption.FALSE)
    @Enumerated(EnumType.STRING)
    protected List<PaymentMethodEnumeration> parkingPaymentMethods;

    @ElementCollection(targetClass = ParkingVehicleEnumeration.class)
    @LazyCollection(LazyCollectionOption.FALSE)
    @Enumerated(EnumType.STRING)
    protected List<ParkingVehicleEnumeration> parkingVehicleTypes;

    protected ParkingLayoutEnumeration parkingLayout;
    protected BigInteger numberOfParkingLevels;
    protected BigInteger principalCapacity;
    protected BigInteger totalCapacity;
    protected boolean overnightParkingPermitted = false;
    protected boolean prohibitedForHazardousMaterials = false;
    protected boolean rechargingAvailable = false;
    protected boolean carpoolingAvailable = false;
    protected boolean carsharingAvailable = false;
    protected boolean secure = false;
    protected boolean realTimeOccupancyAvailable = false;
    protected ParkingReservationEnumeration parkingReservation;
    protected String bookingUrl;
    protected boolean freeParkingOutOfHours = false;
    protected String insee;
    protected String siret;
    protected String typeOfParkingRef;
    protected String operator;
    protected String address;

    @Transient
    protected String originalId;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    protected List<ParkingProperties> parkingProperties;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    protected List<ParkingArea> parkingAreas;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    protected List<TransportType> transportTypes;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    protected List<TypeOfPaymentMethod> typeOfPaymentMethods;
    @ManyToOne
    @JoinColumn(name = "organisation_id")
    private Organisation organisation;
    private String rentalUriIos;
    private String rentalUriAndroid;

    @ElementCollection(targetClass = SiteRefStructure.class, fetch = FetchType.EAGER)
    @CollectionTable(
            name = "parking_adjacent_sites",
            joinColumns = @JoinColumn(name = "parking_id")
    )
    @AttributeOverrides({
            @AttributeOverride(name = "ref", column = @Column(name = "ref")),
            @AttributeOverride(name = "version", column = @Column(name = "version"))
    })
    protected Set<SiteRefStructure> adjacentSites = new HashSet<>();


    @OneToMany(cascade = CascadeType.ALL)
    Set<AvailabilityCondition> availabilityConditions = new HashSet<>();

    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    protected PostalAddress postalAddress;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "parking_id")
    protected List<ParkingEntrance> vehicleEntrances = new ArrayList<>();


    public List<TypeOfPaymentMethod> getTypeOfPaymentMethods() {
        return typeOfPaymentMethods;
    }

    public void setTypeOfPaymentMethods(List<TypeOfPaymentMethod> typeOfPaymentMethods) {
        this.typeOfPaymentMethods = typeOfPaymentMethods;
    }

    public List<TransportType> getTransportTypes() {
        return transportTypes;
    }

    public void setTransportTypes(List<TransportType> transportTypes) {
        this.transportTypes = transportTypes;
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

    public NavigationPaths_RelStructure getNavigationPaths() {
        return navigationPaths;
    }

    public void setNavigationPaths(NavigationPaths_RelStructure value) {
        this.navigationPaths = value;
    }

    public String getPublicCode() {
        return publicCode;
    }

    public void setPublicCode(String value) {
        this.publicCode = value;
    }

    public MultilingualStringEntity getLabel() {
        return label;
    }

    public void setLabel(MultilingualStringEntity value) {
        this.label = value;
    }

    public ParkingTypeEnumeration getParkingType() {
        return parkingType;
    }

    public void setParkingType(ParkingTypeEnumeration value) {
        this.parkingType = value;
    }

    public List<ParkingVehicleEnumeration> getParkingVehicleTypes() {
        if (parkingVehicleTypes == null) {
            parkingVehicleTypes = new ArrayList<>();
        }
        return this.parkingVehicleTypes;
    }

    public ParkingLayoutEnumeration getParkingLayout() {
        return parkingLayout;
    }

    public void setParkingLayout(ParkingLayoutEnumeration value) {
        this.parkingLayout = value;
    }

    public BigInteger getNumberOfParkingLevels() {
        return numberOfParkingLevels;
    }

    public void setNumberOfParkingLevels(BigInteger value) {
        this.numberOfParkingLevels = value;
    }

    public BigInteger getPrincipalCapacity() {
        return principalCapacity;
    }

    public void setPrincipalCapacity(BigInteger value) {
        this.principalCapacity = value;
    }

    public BigInteger getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(BigInteger value) {
        this.totalCapacity = value;
    }

    public boolean isOvernightParkingPermitted() {
        return overnightParkingPermitted;
    }

    public void setOvernightParkingPermitted(boolean value) {
        this.overnightParkingPermitted = value;
    }

    public boolean isProhibitedForHazardousMaterials() {
        return prohibitedForHazardousMaterials;
    }

    public void setProhibitedForHazardousMaterials(boolean value) {
        this.prohibitedForHazardousMaterials = value;
    }

    public boolean isRechargingAvailable() {
        return rechargingAvailable;
    }

    public void setRechargingAvailable(boolean value) {
        this.rechargingAvailable = value;
    }

    public boolean isCarpoolingAvailable() {
        return carpoolingAvailable;
    }

    public void setCarpoolingAvailable(boolean value) {
        this.carpoolingAvailable = value;
    }

    public boolean isCarsharingAvailable() {
        return carsharingAvailable;
    }

    public void setCarsharingAvailable(boolean value) {
        this.carsharingAvailable = value;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean value) {
        this.secure = value;
    }

    public boolean isRealTimeOccupancyAvailable() {
        return realTimeOccupancyAvailable;
    }

    public void setRealTimeOccupancyAvailable(boolean value) {
        this.realTimeOccupancyAvailable = value;
    }

    public List<ParkingPaymentProcessEnumeration> getParkingPaymentProcess() {
        if (parkingPaymentProcess == null) {
            parkingPaymentProcess = new ArrayList<>();
        }
        return this.parkingPaymentProcess;
    }

    public void setParkingPaymentProcess(List<ParkingPaymentProcessEnumeration> parkingPaymentProcess) {
        this.parkingPaymentProcess = parkingPaymentProcess;
    }

    public List<PaymentMethodEnumeration> getParkingPaymentMethods() {
        if (parkingPaymentMethods == null) {
            parkingPaymentMethods = new ArrayList<>();
        }
        return this.parkingPaymentMethods;
    }

    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    public void setDefaultCurrency(String value) {
        this.defaultCurrency = value;
    }

    public List<String> getCurrenciesAccepted() {
        if (currenciesAccepted == null) {
            currenciesAccepted = new ArrayList<>();
        }
        return this.currenciesAccepted;
    }

    public List<String> getCardsAccepted() {
        if (cardsAccepted == null) {
            cardsAccepted = new ArrayList<>();
        }
        return this.cardsAccepted;
    }

    public ParkingReservationEnumeration getParkingReservation() {
        return parkingReservation;
    }

    public void setParkingReservation(ParkingReservationEnumeration value) {
        this.parkingReservation = value;
    }

    public String getBookingUrl() {
        return bookingUrl;
    }

    public void setBookingUrl(String value) {
        this.bookingUrl = value;
    }

    public PaymentByMobileStructure getPaymentByMobile() {
        return paymentByMobile;
    }

    public void setPaymentByMobile(PaymentByMobileStructure value) {
        this.paymentByMobile = value;
    }

    public boolean isFreeParkingOutOfHours() {
        return freeParkingOutOfHours;
    }

    public void setFreeParkingOutOfHours(boolean value) {
        this.freeParkingOutOfHours = value;
    }

    public List<ParkingProperties> getParkingProperties() {
        return parkingProperties;
    }

    public void setParkingProperties(List<ParkingProperties> value) {
        this.parkingProperties = value;
    }

    public List<ParkingArea> getParkingAreas() {
        return parkingAreas;
    }

    public void setParkingAreas(List<ParkingArea> value) {
        this.parkingAreas = value;
    }

    public List<ParkingEntrance> getVehicleEntrances() {
        if (vehicleEntrances == null) {
            vehicleEntrances = new ArrayList<>();
        }
        return vehicleEntrances;
    }

    public void setVehicleEntrances(List<ParkingEntrance> vehicleEntrances) {
        this.vehicleEntrances = vehicleEntrances;
    }

    public String getInsee() {
        return insee;
    }

    public void setInsee(String insee) {
        this.insee = insee;
    }

    public String getSiret() {
        return siret;
    }

    public void setSiret(String siret) {
        this.siret = siret;
    }

    public void setTypeOfParkingRef(String typeOfParkingRef) {
        this.typeOfParkingRef = typeOfParkingRef;
    }

    public String getParkingTypeRef() {
        return typeOfParkingRef;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getOriginalId() {
        return originalId;
    }

    public void setOriginalId(String originalId) {
        this.originalId = originalId;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Organisation getOrganisation() {
        return organisation;
    }

    public void setOrganisation(Organisation organisation) {
        this.organisation = organisation;
    }

    public String getRentalUriIos() {
        return rentalUriIos;
    }

    public void setRentalUriIos(String rentalUriIos) {
        this.rentalUriIos = rentalUriIos;
    }

    public String getRentalUriAndroid() {
        return rentalUriAndroid;
    }

    public void setRentalUriAndroid(String rentalUriAndroid) {
        this.rentalUriAndroid = rentalUriAndroid;
    }


    public Set<SiteRefStructure> getAdjacentSites() {
        return adjacentSites;
    }

    public void setAdjacentSites(Set<SiteRefStructure> adjacentSites) {
        this.adjacentSites = adjacentSites;
    }

    public Set<AvailabilityCondition> getAvailabilityConditions() {
        return availabilityConditions;
    }

    public void setAvailabilityConditions(Set<AvailabilityCondition> availabilityConditions) {
        this.availabilityConditions = availabilityConditions;
    }

    public PostalAddress getPostalAddress() {
        return postalAddress;
    }

    public void setPostalAddress(PostalAddress postalAddress) {
        this.postalAddress = postalAddress;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("netexId", netexId)
                .add("version", version)
                .add("created", created)
                .add("changed", changed)
                .add("centroid", centroid)
                .add("parentSiteRef", parentSiteRef)
                .add("publicCode", publicCode)
                .add("insee", insee)
                .add("siret", siret)
                .add("label", label)
                .add("parkingPaymentProcess", parkingPaymentProcess)
                .add("paymentMethods", parkingPaymentMethods)
                .add("defaultCurrency", defaultCurrency)
                .add("currenciesAccepted", currenciesAccepted)
                .add("cardsAccepted", cardsAccepted)
                .add("paymentByMobile", paymentByMobile)
                .add("vehicleEntrances", vehicleEntrances)
                .add("pathLinks", pathLinks)
                .add("pathJunctions", pathJunctions)
                .add("navigationPaths", navigationPaths)
                .add("parkingType", parkingType)
                .add("parkingVehicleTypes", parkingVehicleTypes)
                .add("parkingLayout", parkingLayout)
                .add("numberOfParkingLevels", numberOfParkingLevels)
                .add("principalCapacity", principalCapacity)
                .add("totalCapacity", totalCapacity)
                .add("overnightParkingPermitted", overnightParkingPermitted)
                .add("prohibitedForHazardousMaterials", prohibitedForHazardousMaterials)
                .add("rechargingAvailable", rechargingAvailable)
                .add("carpoolingAvailable", carpoolingAvailable)
                .add("carsharingAvailable", carsharingAvailable)
                .add("secure", secure)
                .add("realTimeOccupancyAvailable", realTimeOccupancyAvailable)
                .add("parkingReservation", parkingReservation)
                .add("bookingUrl", bookingUrl)
                .add("freeParkingOutOfHours", freeParkingOutOfHours)
                .add("parkingProperties", parkingProperties)
                .add("parkingAreas", parkingAreas)
                .add("originalId", originalId)
                .add("operator", operator)
                .toString();
    }
}
