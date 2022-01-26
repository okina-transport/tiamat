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
import org.rutebanken.netex.model.ShelterEquipment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShelterEquipmentMapper extends CustomMapper<ShelterEquipment, org.rutebanken.tiamat.model.ShelterEquipment> {

    private static final Logger logger = LoggerFactory.getLogger(ShelterEquipmentMapper.class);

    @Override
    public void mapAtoB(ShelterEquipment netexShelterEquipment, org.rutebanken.tiamat.model.ShelterEquipment tiamatShelterEquipment, MappingContext context) {
        try{
            super.mapAtoB(netexShelterEquipment, tiamatShelterEquipment, context);
        }catch(Exception e){
            logger.error("Can't map to tiamat shelter for object:" + netexShelterEquipment.getId());
        }
    }

    @Override
    public void mapBtoA(org.rutebanken.tiamat.model.ShelterEquipment tiamatShelterEquipment, ShelterEquipment netexShelterEquipment, MappingContext context) {
        try {
            super.mapBtoA(tiamatShelterEquipment, netexShelterEquipment, context);
            netexShelterEquipment.setVersion(String.valueOf(tiamatShelterEquipment.getVersion()));
            netexShelterEquipment.setId(tiamatShelterEquipment.getNetexId());

        }catch(Exception e){
            logger.error("Can't map to netex shelter for object:" + tiamatShelterEquipment.getNetexId());
            logger.error(e.getStackTrace().toString());
        }
    }
}
