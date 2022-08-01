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
import org.rutebanken.netex.model.WaitingRoomEquipment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WaitingRoomEquipmentMapper extends CustomMapper<WaitingRoomEquipment, org.rutebanken.tiamat.model.WaitingRoomEquipment> {

    private static final Logger logger = LoggerFactory.getLogger(GeneralSignMapper.class);

    @Override
    public void mapAtoB(WaitingRoomEquipment netexWaitingRoomEquipment, org.rutebanken.tiamat.model.WaitingRoomEquipment tiamatWaitingRoomEquipment, MappingContext context) {
        try {
            super.mapAtoB(netexWaitingRoomEquipment, tiamatWaitingRoomEquipment, context);
        } catch (Exception e) {
            logger.error("Can't map to tiamat WaitingRoomEquipment for object:" + netexWaitingRoomEquipment.getId());
        }
    }

    @Override
    public void mapBtoA(org.rutebanken.tiamat.model.WaitingRoomEquipment tiamatWaitingRoomEquipment, WaitingRoomEquipment netexWaitingRoomEquipment, MappingContext context) {
        try {
            super.mapBtoA(tiamatWaitingRoomEquipment, netexWaitingRoomEquipment, context);
            netexWaitingRoomEquipment.setVersion(String.valueOf(tiamatWaitingRoomEquipment.getVersion()));
            netexWaitingRoomEquipment.setId(tiamatWaitingRoomEquipment.getNetexId());

        } catch (Exception e) {
            logger.error("Can't map to netex WaitingRoomEquipment for object:" + tiamatWaitingRoomEquipment.getNetexId());
            logger.error(e.getStackTrace().toString());
        }
    }
}
