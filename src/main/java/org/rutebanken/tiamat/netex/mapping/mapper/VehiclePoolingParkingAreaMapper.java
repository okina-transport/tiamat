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
import org.rutebanken.tiamat.model.ParkingArea;
import org.rutebanken.tiamat.netex.NetexUtils;

public class VehiclePoolingParkingAreaMapper extends CustomMapper<org.rutebanken.netex.model.VehiclePoolingParkingArea, ParkingArea> {

    @Override
    public void mapAtoB(org.rutebanken.netex.model.VehiclePoolingParkingArea netexParkingArea, ParkingArea tiamatParkingArea, MappingContext context) {
        super.mapAtoB(netexParkingArea, tiamatParkingArea, context);

        tiamatParkingArea.setTotalCapacity(NetexUtils.getParkingAreaTotalCapacity(netexParkingArea.getRest()));
    }

    @Override
    public void mapBtoA(ParkingArea tiamatParkingArea, org.rutebanken.netex.model.VehiclePoolingParkingArea netexParkingArea, MappingContext context) {
        super.mapBtoA(tiamatParkingArea, netexParkingArea, context);

    }
}
