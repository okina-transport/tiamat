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

package org.rutebanken.tiamat.importer.modifier;

import org.rutebanken.tiamat.importer.PublicationDeliveryImporter;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.service.TopographicPlaceLookupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Change stop places and quays before stop place import.
 */
@Service
public class StopPlacePostFilterSteps {

    private static final Logger logger = LoggerFactory.getLogger(StopPlacePostFilterSteps.class);

    private final StopPlaceNameCleaner stopPlaceNameCleaner;
    private final NameToDescriptionMover nameToDescriptionMover;
    private final QuayNameRemover quayNameRemover;
    private final StopPlaceNameNumberToQuayMover stopPlaceNameNumberToQuayMover;
    private final QuayDescriptionPlatformCodeExtractor quayDescriptionPlatformCodeExtractor;
    private final CompassBearingRemover compassBearingRemover;
    private final TopographicPlaceLookupService topographicPlaceLookupService;
    private final TopographicPlaceNameRemover topographicPlaceNameRemover;


    @Autowired
    public StopPlacePostFilterSteps(StopPlaceNameCleaner stopPlaceNameCleaner,
                                    NameToDescriptionMover nameToDescriptionMover,
                                    QuayNameRemover quayNameRemover,
                                    StopPlaceNameNumberToQuayMover stopPlaceNameNumberToQuayMover,
                                    QuayDescriptionPlatformCodeExtractor quayDescriptionPlatformCodeExtractor,
                                    CompassBearingRemover compassBearingRemover,
                                    TopographicPlaceNameRemover topographicPlaceNameRemover,
                                    TopographicPlaceLookupService topographicPlaceLookupService) {
        this.stopPlaceNameCleaner = stopPlaceNameCleaner;
        this.nameToDescriptionMover = nameToDescriptionMover;
        this.quayNameRemover = quayNameRemover;
        this.stopPlaceNameNumberToQuayMover = stopPlaceNameNumberToQuayMover;
        this.quayDescriptionPlatformCodeExtractor = quayDescriptionPlatformCodeExtractor;
        this.compassBearingRemover = compassBearingRemover;
        this.topographicPlaceLookupService = topographicPlaceLookupService;
        this.topographicPlaceNameRemover = topographicPlaceNameRemover;
    }

    public List<StopPlace> run(List<StopPlace> stops) {
        final String logCorrelationId = MDC.get(PublicationDeliveryImporter.IMPORT_CORRELATION_ID);
        stops.parallelStream()
                .peek(stopPlace -> MDC.put(PublicationDeliveryImporter.IMPORT_CORRELATION_ID, logCorrelationId))
                .map(stopPlace -> compassBearingRemover.remove(stopPlace))
                .map(stopPlace -> stopPlaceNameCleaner.cleanNames(stopPlace))
                .map(stopPlace -> nameToDescriptionMover.updateDescriptionFromName(stopPlace))
                .map(stopPlace -> quayNameRemover.removeQuayNameIfEqualToStopPlaceName(stopPlace))
                .map(stopPlace -> stopPlaceNameNumberToQuayMover.moveNumberEndingToQuay(stopPlace))
                .map(stopPlace -> quayDescriptionPlatformCodeExtractor.extractPlatformCodes(stopPlace))
                .peek(stopPlace -> topographicPlaceLookupService.populateTopographicPlaceRelation(stopPlace))
                .map(stopPlace -> topographicPlaceNameRemover.removeIfmatch(stopPlace))
                .peek(stopPlace -> logger.debug("Stop place: {}", stopPlace))
                .collect(toList());
        return stops;
    }
}
