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

import jakarta.persistence.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;


@Entity
public class ParkingArea
        extends ParkingComponent_VersionStructure {

    protected BigInteger totalCapacity;

    @Column(name = "nb_bays_with_recharging")
    protected BigInteger numberOfBaysWithRecharging;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    protected ParkingProperties parkingProperties;

    protected SpecificParkingAreaUsageEnumeration specificParkingAreaUsage = SpecificParkingAreaUsageEnumeration.NONE;

    @OneToMany(mappedBy = "parkingArea", cascade = CascadeType.ALL, orphanRemoval = true)
    protected List<ParkingBay> bays = new ArrayList<>();

    @Transient
    protected EntranceRefs_RelStructure entrances;

    public BigInteger getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(BigInteger value) {
        this.totalCapacity = value;
    }

    public ParkingProperties getParkingProperties() {
        return parkingProperties;
    }

    public void setParkingProperties(ParkingProperties value) {
        this.parkingProperties = value;
    }

    public EntranceRefs_RelStructure getEntrances() {
        return entrances;
    }

    public void setEntrances(EntranceRefs_RelStructure value) {
        this.entrances = value;
    }

    public SpecificParkingAreaUsageEnumeration getSpecificParkingAreaUsage() {
        return specificParkingAreaUsage;
    }

    public void setSpecificParkingAreaUsage(SpecificParkingAreaUsageEnumeration specificParkingAreaUsage) {
        this.specificParkingAreaUsage = specificParkingAreaUsage;
    }

    public List<ParkingBay> getBays() {
        return bays;
    }

    public void setBays(List<ParkingBay> bays) {
        this.bays = bays;
    }

    public BigInteger getNumberOfBaysWithRecharging() {
        return numberOfBaysWithRecharging;
    }

    public void setNumberOfBaysWithRecharging(BigInteger numberOfBaysWithRecharging) {
        this.numberOfBaysWithRecharging = numberOfBaysWithRecharging;
    }
}
