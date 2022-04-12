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

package org.rutebanken.tiamat.netex.mapping.converter;

import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.converter.BidirectionalConverter;
import ma.glasnost.orika.metadata.Type;
import org.rutebanken.netex.model.ParkingAreas_RelStructure;
import org.rutebanken.netex.model.PointOfInterestClassificationRefStructure;
import org.rutebanken.netex.model.PointOfInterestClassificationsViews_RelStructure;
import org.rutebanken.tiamat.model.ParkingArea;
import org.rutebanken.tiamat.model.PointOfInterestClassification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class PointOfInterestClassificationListConverter extends BidirectionalConverter<Set<PointOfInterestClassification>, PointOfInterestClassificationsViews_RelStructure> {

    private static final Logger logger = LoggerFactory.getLogger(PointOfInterestClassificationListConverter.class);

    @Override
    public PointOfInterestClassificationsViews_RelStructure convertTo(Set<PointOfInterestClassification> pointOfInterestClassifications, Type<PointOfInterestClassificationsViews_RelStructure> destinationType, MappingContext mappingContext) {
        if(pointOfInterestClassifications == null || pointOfInterestClassifications.isEmpty()) {
            return null;
        }

        PointOfInterestClassificationsViews_RelStructure pointOfInterestClassificationsViews_relStructure = new PointOfInterestClassificationsViews_RelStructure();

        logger.debug("Mapping {} point of interest classifications to netex", pointOfInterestClassifications != null ? pointOfInterestClassifications.size() : 0);

        pointOfInterestClassifications.forEach(pointOfInterestClassification -> {
            org.rutebanken.netex.model.PointOfInterestClassificationRefStructure netexPointOfInterestClassificationRef = new PointOfInterestClassificationRefStructure().withRef(pointOfInterestClassification.getNetexId());
            pointOfInterestClassificationsViews_relStructure.getPointOfInterestClassificationRefOrPointOfInterestClassificationView().add(netexPointOfInterestClassificationRef);
        });
        return pointOfInterestClassificationsViews_relStructure;
    }

    @Override
    public Set<PointOfInterestClassification> convertFrom(PointOfInterestClassificationsViews_RelStructure pointOfInterestClassificationsViews_relStructure, Type<Set<PointOfInterestClassification>> destinationType, MappingContext mappingContext) {
        logger.debug("Mapping {} classifications to internal model", pointOfInterestClassificationsViews_relStructure != null ? pointOfInterestClassificationsViews_relStructure.getPointOfInterestClassificationRefOrPointOfInterestClassificationView().size() : 0);
        Set<PointOfInterestClassification> pointOfInterestClassifications = new HashSet<>();
        if(pointOfInterestClassificationsViews_relStructure.getPointOfInterestClassificationRefOrPointOfInterestClassificationView() != null) {
            pointOfInterestClassificationsViews_relStructure.getPointOfInterestClassificationRefOrPointOfInterestClassificationView().stream()
                    .filter(object -> object instanceof org.rutebanken.netex.model.PointOfInterestClassification)
                    .map(object -> ((org.rutebanken.netex.model.PointOfInterestClassification) object))
                    .map(netexPointOfInterestClassification -> {
                        PointOfInterestClassification tiamatPoiClassification = mapperFacade.map(netexPointOfInterestClassification, PointOfInterestClassification.class);
                        return tiamatPoiClassification;
                    })
                    .forEach(pointOfInterestClassification -> pointOfInterestClassifications.add(pointOfInterestClassification));
        }

        return pointOfInterestClassifications;
    }
}
