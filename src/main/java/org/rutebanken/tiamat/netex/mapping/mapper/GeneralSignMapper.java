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
import org.rutebanken.netex.model.GeneralSign;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeneralSignMapper extends CustomMapper<GeneralSign, org.rutebanken.tiamat.model.GeneralSign> {

    private static final Logger logger = LoggerFactory.getLogger(GeneralSignMapper.class);

    @Override
    public void mapAtoB(GeneralSign netexGeneralSign, org.rutebanken.tiamat.model.GeneralSign tiamatGeneralSign, MappingContext context) {
        try{
            super.mapAtoB(netexGeneralSign, tiamatGeneralSign, context);
        }catch(Exception e){
            logger.error("Can't map to tiamat GeneralSign for object:" + netexGeneralSign.getId());
        }
    }

    @Override
    public void mapBtoA(org.rutebanken.tiamat.model.GeneralSign tiamatGeneralSign, GeneralSign netexGeneralSign, MappingContext context) {
        try {
            super.mapBtoA(tiamatGeneralSign, netexGeneralSign, context);
            netexGeneralSign.setVersion(String.valueOf(tiamatGeneralSign.getVersion()));
            netexGeneralSign.setId(tiamatGeneralSign.getNetexId());

        }catch(Exception e){
            logger.error("Can't map to netex GeneralSign for object:" + tiamatGeneralSign.getNetexId());
            logger.error(e.getStackTrace().toString());
        }
    }
}
