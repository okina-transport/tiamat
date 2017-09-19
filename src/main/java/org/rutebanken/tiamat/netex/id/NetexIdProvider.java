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

package org.rutebanken.tiamat.netex.id;

import org.rutebanken.tiamat.model.identification.IdentifiedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NetexIdProvider {

    private static final Logger logger = LoggerFactory.getLogger(NetexIdProvider.class);

    private final GaplessIdGeneratorService gaplessIdGenerator;

    private final ValidPrefixList validPrefixList;

    @Autowired
    public NetexIdProvider(GaplessIdGeneratorService gaplessIdGenerator, ValidPrefixList validPrefixList) {
        this.gaplessIdGenerator = gaplessIdGenerator;
        this.validPrefixList = validPrefixList;


    }

    public String getGeneratedId(IdentifiedEntity identifiedEntity) {
        String entityTypeName = key(identifiedEntity);

        long longId = gaplessIdGenerator.getNextIdForEntity(entityTypeName);

        return NetexIdHelper.getNetexId(entityTypeName, longId);
    }

    public void claimId(IdentifiedEntity identifiedEntity) {

        String prefix = NetexIdHelper.extractIdPrefix(identifiedEntity.getNetexId());

        if(validPrefixList.isValidPrefixForType(prefix, identifiedEntity.getClass())) {
            logger.debug("Claimed ID {} contains valid prefix for claiming: {}", identifiedEntity.getNetexId(), prefix);

            if(NetexIdHelper.isNsrId(identifiedEntity.getNetexId())) {
                Long claimedId = NetexIdHelper.extractIdPostfixNumeric(identifiedEntity.getNetexId());

                String entityTypeName = key(identifiedEntity);

                gaplessIdGenerator.getNextIdForEntity(entityTypeName, claimedId);
            }
            // Because IDs might end with non-numbers we cannot support claiming for any ID other than NSR.
        } else {
            logger.warn("Detected non NSR ID: {} with prefix {}", identifiedEntity.getNetexId(), prefix);
        }
    }

    private String key(IdentifiedEntity identifiedEntity) {
        return identifiedEntity.getClass().getSimpleName();
    }

}
