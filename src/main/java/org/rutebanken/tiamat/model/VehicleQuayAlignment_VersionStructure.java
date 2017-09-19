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

public class VehicleQuayAlignment_VersionStructure
        extends VersionedChildStructure {

    protected VehicleStoppingPlaceRefStructure vehicleStoppingPlaceRef;
    protected QuayReference quayRef;

    public VehicleStoppingPlaceRefStructure getVehicleStoppingPlaceRef() {
        return vehicleStoppingPlaceRef;
    }

    public void setVehicleStoppingPlaceRef(VehicleStoppingPlaceRefStructure value) {
        this.vehicleStoppingPlaceRef = value;
    }

    public QuayReference getQuayRef() {
        return quayRef;
    }

    public void setQuayRef(QuayReference value) {
        this.quayRef = value;
    }

}
