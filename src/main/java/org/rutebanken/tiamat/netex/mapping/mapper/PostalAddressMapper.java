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
import org.apache.commons.lang3.StringUtils;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.PostalAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostalAddressMapper extends CustomMapper<PostalAddress, org.rutebanken.tiamat.model.PostalAddress> {

    private static final Logger logger = LoggerFactory.getLogger(PostalAddressMapper.class);

    @Override
    public void mapAtoB(PostalAddress netexPostalAddress, org.rutebanken.tiamat.model.PostalAddress tiamatPostalAddress, MappingContext context) {
        try{
            super.mapAtoB(netexPostalAddress, tiamatPostalAddress, context);
            if (netexPostalAddress.getTown() != null){
                tiamatPostalAddress.setTown(netexPostalAddress.getTown().getValue());
            }

            if (netexPostalAddress.getStreet() != null){
                tiamatPostalAddress.setStreet(netexPostalAddress.getStreet().getValue());
            }
        }catch(Exception e){
            logger.error("Can't map to tiamat postalAddress for object:" + netexPostalAddress.getId());
        }
    }

    @Override
    public void mapBtoA(org.rutebanken.tiamat.model.PostalAddress tiamatPostalAddress, PostalAddress netexPostalAddress, MappingContext context) {
        try {
            super.mapBtoA(tiamatPostalAddress, netexPostalAddress, context);

            if (StringUtils.isNotEmpty(tiamatPostalAddress.getStreet())){
                MultilingualString stretMultiLing = new MultilingualString();
                stretMultiLing.setValue(tiamatPostalAddress.getStreet());
                netexPostalAddress.setStreet(stretMultiLing);
            }

            if (StringUtils.isNotEmpty(tiamatPostalAddress.getTown())){
                MultilingualString townMultiLing = new MultilingualString();
                townMultiLing.setValue(tiamatPostalAddress.getTown());
                netexPostalAddress.setTown(townMultiLing);
            }

        }catch(Exception e){
            logger.error("Can't map to netex postalAddress for object:" + tiamatPostalAddress.getId());
            logger.error(e.getMessage());
        }
    }
}
