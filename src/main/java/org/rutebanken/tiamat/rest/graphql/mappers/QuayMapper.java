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

package org.rutebanken.tiamat.rest.graphql.mappers;

import org.apache.commons.lang.StringUtils;
import org.rutebanken.tiamat.externalapis.ApiProxyService;
import org.rutebanken.tiamat.model.PrivateCodeStructure;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.service.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.COMPASS_BEARING;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.ID;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.PRIVATE_CODE;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.PUBLIC_CODE;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.TYPE;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.VALUE;

@Component
public class QuayMapper {

    public static final Logger logger = LoggerFactory.getLogger(QuayMapper.class);

    @Autowired
    private GroupOfEntitiesMapper groupOfEntitiesMapper;

    private ApiProxyService apiProxyService = new ApiProxyService();

    public boolean populateQuayFromInput(StopPlace stopPlace, Map quayInputMap) {
        Quay quay;
        if (quayInputMap.get(ID) != null) {
            Optional<Quay> existingQuay = stopPlace.getQuays().stream()
                    .filter(q -> q.getNetexId() != null)
                    .filter(q -> q.getNetexId().equals(quayInputMap.get(ID))).findFirst();

            Preconditions.checkArgument(existingQuay.isPresent(),
                    "Attempting to update Quay [id = %s] on StopPlace [id = %s] , but Quay does not exist on StopPlace",
                    quayInputMap.get(ID),
                    stopPlace.getNetexId());

            quay = existingQuay.get();
            logger.info("Updating Quay {} for StopPlace {}", quay.getNetexId(), stopPlace.getNetexId());
        } else {
            quay = new Quay();
            logger.info("Creating new Quay");
        }
        boolean isQuayUpdated = groupOfEntitiesMapper.populate(quayInputMap, quay);

        if (quayInputMap.get(COMPASS_BEARING) != null) {
            quay.setCompassBearing(((BigDecimal) quayInputMap.get(COMPASS_BEARING)).floatValue());
            isQuayUpdated = true;
        }
        if (quayInputMap.get(PUBLIC_CODE) != null) {
            quay.setPublicCode((String) quayInputMap.get(PUBLIC_CODE));
            isQuayUpdated = true;
        }

        if (quayInputMap.get(PRIVATE_CODE) != null) {
            Map privateCodeInputMap = (Map) quayInputMap.get(PRIVATE_CODE);
            if (quay.getPrivateCode() == null) {
                quay.setPrivateCode(new PrivateCodeStructure());
            }
            quay.getPrivateCode().setType((String) privateCodeInputMap.get(TYPE));
            quay.getPrivateCode().setValue((String) privateCodeInputMap.get(VALUE));
            isQuayUpdated = true;
        }

        // On mets à jour le zip code
        String citycodeReverseGeocoding = null;
        try {
           citycodeReverseGeocoding = apiProxyService.getCitycodeByReverseGeocoding(new BigDecimal(quay.getCentroid().getCoordinate().y, MathContext.DECIMAL64), new BigDecimal(quay.getCentroid().getCoordinate().x, MathContext.DECIMAL64));
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération du code postal du quay = " + quay.getId(), e);
        }
        if (!StringUtils.equals(quay.getZipCode(), citycodeReverseGeocoding) && citycodeReverseGeocoding != null) {
            quay.setZipCode(citycodeReverseGeocoding);
            isQuayUpdated = true;
        }

        if (isQuayUpdated) {
            quay.setChanged(Instant.now());

            if (quay.getNetexId() == null) {
                stopPlace.getQuays().add(quay);
            }
        }
        return isQuayUpdated;
    }

}
