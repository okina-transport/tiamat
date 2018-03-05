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

public class SiteConnectionEndStructure {

    protected AllVehicleModesOfTransportEnumeration transportMode;
    protected StopPlaceReference stopPlaceRef;
    protected AccessSpaceRefStructure accessSpaceRef;
    protected BoardingPositionRefStructure boardingPositionRef;
    protected QuayReference quayRef;
    protected StopPlaceEntranceRefStructure stopPlaceEntranceRef;
    protected PointOfInterestRefStructure pointOfInterestRef;
    protected PointOfInterestSpaceRefStructure pointOfInterestSpaceRef;
    protected PointOfInterestEntranceRefStructure pointOfInterestEntranceRef;
    protected ParkingRefStructure parkingRef;
    protected ParkingAreaRefStructure parkingAreaRef;
    protected ParkingEntranceRefStructure parkingEntranceRef;
    protected MultilingualStringEntity label;

    public AllVehicleModesOfTransportEnumeration getTransportMode() {
        return transportMode;
    }

    public void setTransportMode(AllVehicleModesOfTransportEnumeration value) {
        this.transportMode = value;
    }

    public StopPlaceReference getStopPlaceRef() {
        return stopPlaceRef;
    }

    public void setStopPlaceRef(StopPlaceReference value) {
        this.stopPlaceRef = value;
    }

    public AccessSpaceRefStructure getAccessSpaceRef() {
        return accessSpaceRef;
    }

    public void setAccessSpaceRef(AccessSpaceRefStructure value) {
        this.accessSpaceRef = value;
    }

    public BoardingPositionRefStructure getBoardingPositionRef() {
        return boardingPositionRef;
    }

    public void setBoardingPositionRef(BoardingPositionRefStructure value) {
        this.boardingPositionRef = value;
    }

    public QuayReference getQuayRef() {
        return quayRef;
    }

    public void setQuayRef(QuayReference value) {
        this.quayRef = value;
    }

    public StopPlaceEntranceRefStructure getStopPlaceEntranceRef() {
        return stopPlaceEntranceRef;
    }

    public void setStopPlaceEntranceRef(StopPlaceEntranceRefStructure value) {
        this.stopPlaceEntranceRef = value;
    }

    public PointOfInterestRefStructure getPointOfInterestRef() {
        return pointOfInterestRef;
    }

    public void setPointOfInterestRef(PointOfInterestRefStructure value) {
        this.pointOfInterestRef = value;
    }

    public PointOfInterestSpaceRefStructure getPointOfInterestSpaceRef() {
        return pointOfInterestSpaceRef;
    }

    public void setPointOfInterestSpaceRef(PointOfInterestSpaceRefStructure value) {
        this.pointOfInterestSpaceRef = value;
    }

    public PointOfInterestEntranceRefStructure getPointOfInterestEntranceRef() {
        return pointOfInterestEntranceRef;
    }

    public void setPointOfInterestEntranceRef(PointOfInterestEntranceRefStructure value) {
        this.pointOfInterestEntranceRef = value;
    }

    public ParkingRefStructure getParkingRef() {
        return parkingRef;
    }

    public void setParkingRef(ParkingRefStructure value) {
        this.parkingRef = value;
    }

    public ParkingAreaRefStructure getParkingAreaRef() {
        return parkingAreaRef;
    }

    public void setParkingAreaRef(ParkingAreaRefStructure value) {
        this.parkingAreaRef = value;
    }

    public ParkingEntranceRefStructure getParkingEntranceRef() {
        return parkingEntranceRef;
    }

    public void setParkingEntranceRef(ParkingEntranceRefStructure value) {
        this.parkingEntranceRef = value;
    }

    public MultilingualStringEntity getLabel() {
        return label;
    }

    public void setLabel(MultilingualStringEntity value) {
        this.label = value;
    }

}
