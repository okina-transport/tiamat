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

package org.rutebanken.tiamat.netex.mapping.mapper;

import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MappingContext;
import org.rutebanken.tiamat.model.ParkingCapacity;
import org.rutebanken.tiamat.model.ParkingProperties;

import java.util.ArrayList;
import java.util.List;

public class ParkingPropertyMapper extends CustomMapper<org.rutebanken.netex.model.ParkingProperties, ParkingProperties> {

    @Override
    public void mapAtoB(org.rutebanken.netex.model.ParkingProperties netexParkingProperties, ParkingProperties tiamatParkingProperties, MappingContext context) {
        super.mapAtoB(netexParkingProperties, tiamatParkingProperties, context);

        List<ParkingCapacity> capacities = new ArrayList<>();

        for (Object capacity : netexParkingProperties.getSpaces().getParkingCapacityRefOrParkingCapacity()) {
            ParkingCapacity currentCapacity = (ParkingCapacity) capacity;
            //construire un parking capacity tiamat
            ParkingCapacity newCapacity = new ParkingCapacity();
            newCapacity.setParentRef(currentCapacity.getParentRef());
            newCapacity.setParkingUserType(currentCapacity.getParkingUserType());
            newCapacity.setParkingVehicleType(currentCapacity.getParkingVehicleType());
            newCapacity.setParkingStayType(currentCapacity.getParkingStayType());
            newCapacity.setNumberOfSpaces(currentCapacity.getNumberOfSpaces());
            newCapacity.setNumberOfSpacesWithRechargePoint(currentCapacity.getNumberOfSpacesWithRechargePoint());
            newCapacity.setNumberOfCarsharingSpaces(currentCapacity.getNumberOfCarsharingSpaces());
            newCapacity.setVersion(currentCapacity.getVersion()+1);
            capacities.add(newCapacity);
        }
        tiamatParkingProperties.setSpaces(capacities);
        tiamatParkingProperties.setNetexId(netexParkingProperties.getId());
        tiamatParkingProperties.setVersion(Long.parseLong(netexParkingProperties.getVersion())+1);
    }

    @Override
    public void mapBtoA(ParkingProperties tiamatParkingProperties, org.rutebanken.netex.model.ParkingProperties netexParkingProperties, MappingContext context) {
        super.mapBtoA(tiamatParkingProperties, netexParkingProperties, context);


    }
}
