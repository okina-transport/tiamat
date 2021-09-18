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
import net.logstash.logback.encoder.org.apache.commons.lang.StringUtils;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.TopographicPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.List;

public class TopographicPlaceMapper extends CustomMapper<TopographicPlace, org.rutebanken.tiamat.model.TopographicPlace> {

    private static final Logger logger = LoggerFactory.getLogger(TopographicPlaceMapper.class);

    @Override
    public void mapAtoB(TopographicPlace netexTopographicPlace, org.rutebanken.tiamat.model.TopographicPlace tiamatTopographicPlace, MappingContext context) {
        try{
            super.mapAtoB(netexTopographicPlace, tiamatTopographicPlace, context);
        }catch(Exception e){
            logger.error("Can't map to tiamat stopPlace for object:" + netexTopographicPlace.getId());
        }
    }

    @Override
    public void mapBtoA(org.rutebanken.tiamat.model.TopographicPlace tiamatTopographicPlace, TopographicPlace netexTopographicPlace, MappingContext context) {
        try {
            super.mapBtoA(tiamatTopographicPlace, netexTopographicPlace, context);

            MultilingualString topographicName = new MultilingualString();
            mapperFacade.map(tiamatTopographicPlace.getName(), topographicName);

            if (topographicName == null || StringUtils.isEmpty(topographicName.getValue())) {
                logger.warn("Empty name for topographic place:" + tiamatTopographicPlace.getNetexId());
            }

            netexTopographicPlace.setName(topographicName);
        }catch(Exception e){
            logger.error("Can't map to netex stopPlace for object:" + tiamatTopographicPlace.getNetexId());
        }
    }
}
