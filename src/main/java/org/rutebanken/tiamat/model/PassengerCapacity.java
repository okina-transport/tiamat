package org.rutebanken.tiamat.model;

import javax.persistence.*;
import java.math.BigInteger;

@Entity
public class PassengerCapacity extends DataManagedObjectStructure {

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

    @Enumerated(EnumType.STRING)
    private FareClassEnumeration fareClass;
    private BigInteger totalCapacity;
    private BigInteger seatingCapacity;
    private BigInteger standingCapacity;
    private BigInteger specialPlaceCapacity;
    private BigInteger pushchairCapacity;
    private BigInteger wheelchairPlaceCapacity;

    public FareClassEnumeration getFareClass() {
        return fareClass;
    }

    public void setFareClass(FareClassEnumeration fareClass) {
        this.fareClass = fareClass;
    }

    public BigInteger getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(BigInteger totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public BigInteger getSeatingCapacity() {
        return seatingCapacity;
    }

    public void setSeatingCapacity(BigInteger seatingCapacity) {
        this.seatingCapacity = seatingCapacity;
    }

    public BigInteger getStandingCapacity() {
        return standingCapacity;
    }

    public void setStandingCapacity(BigInteger standingCapacity) {
        this.standingCapacity = standingCapacity;
    }

    public BigInteger getSpecialPlaceCapacity() {
        return specialPlaceCapacity;
    }

    public void setSpecialPlaceCapacity(BigInteger specialPlaceCapacity) {
        this.specialPlaceCapacity = specialPlaceCapacity;
    }

    public BigInteger getPushchairCapacity() {
        return pushchairCapacity;
    }

    public void setPushchairCapacity(BigInteger pushchairCapacity) {
        this.pushchairCapacity = pushchairCapacity;
    }

    public BigInteger getWheelchairPlaceCapacity() {
        return wheelchairPlaceCapacity;
    }

    public void setWheelchairPlaceCapacity(BigInteger wheelchairPlaceCapacity) {
        this.wheelchairPlaceCapacity = wheelchairPlaceCapacity;
    }
}
