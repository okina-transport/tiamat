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

import java.math.BigDecimal;


public class VehicleType_VersionStructure
        extends DataManagedObjectStructure {

    protected MultilingualStringEntity name;
    protected MultilingualStringEntity shortName;
    protected MultilingualStringEntity description;
    protected PrivateCodeStructure privateCode;
    protected Boolean reversingDirection;
    protected Boolean selfPropelled;
    protected TypeOfFuelEnumeration typeOfFuel;
    protected String euroClass;
    protected PassengerCapacityStructure passengerCapacity;
    protected PassengerCapacities_RelStructure capacities;
    protected Boolean lowFloor;
    protected Boolean hasLiftOrRamp;
    protected Boolean hasHoist;
    protected BigDecimal length;
    protected VehicleTypeRefStructure includedIn;
    protected VehicleModelRefStructure classifiedAsRef;
    protected ServiceFacilitySets_RelStructure facilities;
    protected PassengerCarryingRequirements_RelStructure canCarry;
    protected VehicleManoeuvringRequirements_RelStructure canManoeuvre;
    protected FacilityRequirements_RelStructure satisfiesFacilityRequirements;

    public MultilingualStringEntity getName() {
        return name;
    }

    public void setName(MultilingualStringEntity value) {
        this.name = value;
    }

    public MultilingualStringEntity getShortName() {
        return shortName;
    }

    public void setShortName(MultilingualStringEntity value) {
        this.shortName = value;
    }

    public MultilingualStringEntity getDescription() {
        return description;
    }

    public void setDescription(MultilingualStringEntity value) {
        this.description = value;
    }

    public PrivateCodeStructure getPrivateCode() {
        return privateCode;
    }

    public void setPrivateCode(PrivateCodeStructure value) {
        this.privateCode = value;
    }

    public Boolean isReversingDirection() {
        return reversingDirection;
    }

    public void setReversingDirection(Boolean value) {
        this.reversingDirection = value;
    }

    public Boolean isSelfPropelled() {
        return selfPropelled;
    }

    public void setSelfPropelled(Boolean value) {
        this.selfPropelled = value;
    }

    public TypeOfFuelEnumeration getTypeOfFuel() {
        return typeOfFuel;
    }

    public void setTypeOfFuel(TypeOfFuelEnumeration value) {
        this.typeOfFuel = value;
    }

    public String getEuroClass() {
        return euroClass;
    }

    public void setEuroClass(String value) {
        this.euroClass = value;
    }

    public PassengerCapacityStructure getPassengerCapacity() {
        return passengerCapacity;
    }

    public void setPassengerCapacity(PassengerCapacityStructure value) {
        this.passengerCapacity = value;
    }

    public PassengerCapacities_RelStructure getCapacities() {
        return capacities;
    }

    public void setCapacities(PassengerCapacities_RelStructure value) {
        this.capacities = value;
    }

    public Boolean isLowFloor() {
        return lowFloor;
    }

    public void setLowFloor(Boolean value) {
        this.lowFloor = value;
    }

    public Boolean isHasLiftOrRamp() {
        return hasLiftOrRamp;
    }

    public void setHasLiftOrRamp(Boolean value) {
        this.hasLiftOrRamp = value;
    }

    public Boolean isHasHoist() {
        return hasHoist;
    }

    public void setHasHoist(Boolean value) {
        this.hasHoist = value;
    }

    public BigDecimal getLength() {
        return length;
    }

    public void setLength(BigDecimal value) {
        this.length = value;
    }

    public VehicleTypeRefStructure getIncludedIn() {
        return includedIn;
    }

    public void setIncludedIn(VehicleTypeRefStructure value) {
        this.includedIn = value;
    }

    public VehicleModelRefStructure getClassifiedAsRef() {
        return classifiedAsRef;
    }

    public void setClassifiedAsRef(VehicleModelRefStructure value) {
    }

    public ServiceFacilitySets_RelStructure getFacilities() {
        return facilities;
    }

    public void setFacilities(ServiceFacilitySets_RelStructure value) {
        this.facilities = value;
    }

    public PassengerCarryingRequirements_RelStructure getCanCarry() {
        return canCarry;
    }

    public void setCanCarry(PassengerCarryingRequirements_RelStructure value) {
        this.canCarry = value;
    }

    public VehicleManoeuvringRequirements_RelStructure getCanManoeuvre() {
        return canManoeuvre;
    }

    public void setCanManoeuvre(VehicleManoeuvringRequirements_RelStructure value) {
        this.canManoeuvre = value;
    }

    public FacilityRequirements_RelStructure getSatisfiesFacilityRequirements() {
        return satisfiesFacilityRequirements;
    }

    public void setSatisfiesFacilityRequirements(FacilityRequirements_RelStructure value) {
        this.satisfiesFacilityRequirements = value;
    }

}
