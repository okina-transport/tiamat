package org.rutebanken.tiamat.model;

import javax.persistence.*;
import java.math.BigDecimal;

@MappedSuperclass
public class TransportType_VersionStructure extends DataManagedObjectStructure {

    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "name_value")),
            @AttributeOverride(name = "lang", column = @Column(name = "name_lang", length = 5))
    })
    @Embedded
    protected EmbeddableMultilingualString name;


    @Transient
    protected EmbeddableMultilingualString shortName;

    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "description_value", length = 4000)),
            @AttributeOverride(name = "lang", column = @Column(name = "description_lang", length = 5))
    })
    @Embedded
    protected EmbeddableMultilingualString description;
    private String euroClass;
    private Boolean reversingDirection;
    private Boolean selfPropelled;
    @Enumerated(EnumType.STRING)
    private PropulsionTypeEnumeration propulsionType;
    @Enumerated(EnumType.STRING)
    private FuelTypeEnumeration fuelType;
    @Enumerated(EnumType.STRING)
    private FuelTypeEnumeration typeOfFuel;
    private BigDecimal maximumRange;
    @Enumerated(EnumType.STRING)
    private AllVehicleModesOfTransportEnumeration transportMode;
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "passenger_capacity_id")
    private PassengerCapacity passengerCapacity;

    public PassengerCapacity getPassengerCapacity() {
        return passengerCapacity;
    }

    public void setPassengerCapacity(PassengerCapacity passengerCapacity) {
        this.passengerCapacity = passengerCapacity;
    }

    public AllVehicleModesOfTransportEnumeration getTransportMode() {
        return transportMode;
    }

    public void setTransportMode(AllVehicleModesOfTransportEnumeration transportMode) {
        this.transportMode = transportMode;
    }

    public BigDecimal getMaximumRange() {
        return maximumRange;
    }

    public void setMaximumRange(BigDecimal maximumRange) {
        this.maximumRange = maximumRange;
    }

    public FuelTypeEnumeration getTypeOfFuel() {
        return typeOfFuel;
    }

    public void setTypeOfFuel(FuelTypeEnumeration typeOfFuel) {
        this.typeOfFuel = typeOfFuel;
    }

    public FuelTypeEnumeration getFuelType() {
        return fuelType;
    }

    public void setFuelType(FuelTypeEnumeration fuelType) {
        this.fuelType = fuelType;
    }

    public PropulsionTypeEnumeration getPropulsionType() {
        return propulsionType;
    }

    public void setPropulsionType(PropulsionTypeEnumeration propulsionType) {
        this.propulsionType = propulsionType;
    }

    public Boolean getSelfPropelled() {
        return selfPropelled;
    }

    public void setSelfPropelled(Boolean selfPropelled) {
        this.selfPropelled = selfPropelled;
    }

    public Boolean getReversingDirection() {
        return reversingDirection;
    }

    public void setReversingDirection(Boolean reversingDirection) {
        this.reversingDirection = reversingDirection;
    }

    public String getEuroClass() {
        return euroClass;
    }

    public void setEuroClass(String euroClass) {
        this.euroClass = euroClass;
    }

    public EmbeddableMultilingualString getName() {
        return name;
    }

    public void setName(EmbeddableMultilingualString name) {
        this.name = name;
    }

    public EmbeddableMultilingualString getShortName() {
        return shortName;
    }

    public void setShortName(EmbeddableMultilingualString shortName) {
        this.shortName = shortName;
    }

    public EmbeddableMultilingualString getDescription() {
        return description;
    }

    public void setDescription(EmbeddableMultilingualString description) {
        this.description = description;
    }
}
