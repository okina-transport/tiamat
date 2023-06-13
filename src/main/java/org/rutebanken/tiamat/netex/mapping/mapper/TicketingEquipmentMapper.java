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
import org.rutebanken.netex.model.TicketingEquipment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TicketingEquipmentMapper extends CustomMapper<TicketingEquipment, org.rutebanken.tiamat.model.TicketingEquipment> {

    private static final Logger logger = LoggerFactory.getLogger(TicketingEquipmentMapper.class);

    @Override
    public void mapAtoB(TicketingEquipment netexTicketingEquipment, org.rutebanken.tiamat.model.TicketingEquipment tiamatTicketingEquipment, MappingContext context) {
        try{
            super.mapAtoB(netexTicketingEquipment, tiamatTicketingEquipment, context);
        }catch(Exception e){
            logger.error("Can't map to tiamat ticketing for object:" + netexTicketingEquipment.getId());
        }
    }

    @Override
    public void mapBtoA(org.rutebanken.tiamat.model.TicketingEquipment tiamatTicketingEquipment, TicketingEquipment netexTicketingEquipment, MappingContext context) {
        try {
            super.mapBtoA(tiamatTicketingEquipment, netexTicketingEquipment, context);
            netexTicketingEquipment.setVersion(String.valueOf(tiamatTicketingEquipment.getVersion()));
            netexTicketingEquipment.setId(tiamatTicketingEquipment.getNetexId());

        }catch(Exception e){
            logger.error("Can't map to netex ticketing for object:" + tiamatTicketingEquipment.getNetexId());
            logger.error(e.getStackTrace().toString());
        }
    }
}
