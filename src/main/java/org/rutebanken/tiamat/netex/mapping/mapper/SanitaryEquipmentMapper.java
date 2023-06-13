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
import org.rutebanken.netex.model.SanitaryEquipment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SanitaryEquipmentMapper extends CustomMapper<SanitaryEquipment, org.rutebanken.tiamat.model.SanitaryEquipment> {

    private static final Logger logger = LoggerFactory.getLogger(SanitaryEquipmentMapper.class);

    @Override
    public void mapAtoB(SanitaryEquipment netexSanitaryEquipment, org.rutebanken.tiamat.model.SanitaryEquipment tiamatSanitaryEquipment, MappingContext context) {
        try{
            super.mapAtoB(netexSanitaryEquipment, tiamatSanitaryEquipment, context);
        }catch(Exception e){
            logger.error("Can't map to tiamat sanitary for object:" + netexSanitaryEquipment.getId());
        }
    }

    @Override
    public void mapBtoA(org.rutebanken.tiamat.model.SanitaryEquipment tiamatSanitaryEquipment, SanitaryEquipment netexSanitaryEquipment, MappingContext context) {
        try {
            super.mapBtoA(tiamatSanitaryEquipment, netexSanitaryEquipment, context);
            netexSanitaryEquipment.setVersion(String.valueOf(tiamatSanitaryEquipment.getVersion()));
            netexSanitaryEquipment.setId(tiamatSanitaryEquipment.getNetexId());

        }catch(Exception e){
            logger.error("Can't map to netex sanitary for object:" + tiamatSanitaryEquipment.getNetexId());
            logger.error(e.getStackTrace().toString());
        }
    }
}
