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
import org.rutebanken.netex.model.InstalledEquipment_VersionStructure;
import org.rutebanken.netex.model.ShelterEquipment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstalledEquipmentMapper extends CustomMapper<InstalledEquipment_VersionStructure, org.rutebanken.tiamat.model.InstalledEquipment_VersionStructure> {

    private static final Logger logger = LoggerFactory.getLogger(InstalledEquipmentMapper.class);

    @Override
    public void mapAtoB(InstalledEquipment_VersionStructure netexInstalledEquipment, org.rutebanken.tiamat.model.InstalledEquipment_VersionStructure tiamatInstalledEquipment, MappingContext context) {
        try{
            super.mapAtoB(netexInstalledEquipment, tiamatInstalledEquipment, context);
            logger.info("INstalled - mapAtoB, new id:" + netexInstalledEquipment.getId());
            logger.info("INstalled - mapAtoB, netex id:" + tiamatInstalledEquipment.getNetexId());
        }catch(Exception e){
            logger.error("Can't map to tiamat stopPlace for object:" + netexInstalledEquipment.getId());
        }
    }

    @Override
    public void mapBtoA(org.rutebanken.tiamat.model.InstalledEquipment_VersionStructure tiamatInstalledEquipment, InstalledEquipment_VersionStructure netexInstalledEquipment, MappingContext context) {
        try {
            super.mapBtoA(tiamatInstalledEquipment, netexInstalledEquipment, context);
            logger.info("INstalled - mapBtoA, new id:" + netexInstalledEquipment.getId());
            logger.info("INstalled - mapBtoA, netex id:" + tiamatInstalledEquipment.getNetexId());

        }catch(Exception e){
            logger.error("Can't map to netex InstalledEquipment for object:" + tiamatInstalledEquipment.getNetexId());
            logger.error(e.getStackTrace().toString());
        }
    }
}
