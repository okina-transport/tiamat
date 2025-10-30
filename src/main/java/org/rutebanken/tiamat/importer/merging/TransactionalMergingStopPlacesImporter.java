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

package org.rutebanken.tiamat.importer.merging;

import org.rutebanken.tiamat.importer.StopPlaceTopographicPlaceReferenceUpdater;
import org.rutebanken.tiamat.model.StopPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * When importing site frames with the matching stops concurrently, not thread safe.
 */
@Component
@Transactional
public class TransactionalMergingStopPlacesImporter {

    private static final Logger logger = LoggerFactory.getLogger(TransactionalMergingStopPlacesImporter.class);

    private final MergingStopPlaceImporter mergingStopPlaceImporter;

    private final StopPlaceTopographicPlaceReferenceUpdater topographicPlaceReferenceUpdater;

    @Autowired
    public TransactionalMergingStopPlacesImporter(MergingStopPlaceImporter mergingStopPlaceImporter,
                                                  StopPlaceTopographicPlaceReferenceUpdater topographicPlaceReferenceUpdater) {
        this.mergingStopPlaceImporter = mergingStopPlaceImporter;
        this.topographicPlaceReferenceUpdater = topographicPlaceReferenceUpdater;
    }

    public Collection<org.rutebanken.netex.model.StopPlace> importStopPlaces(
            List<StopPlace> stopPlaces,
            AtomicInteger stopPlacesCreated,
            Boolean containsMobiitiIds,
            boolean recomputeStopPlacesLocation) {

        List<org.rutebanken.netex.model.StopPlace> createdStopPlaces = new ArrayList<>();

        stopPlaces.stream()
                .filter(Objects::nonNull)
                .forEach(stopPlace -> {
                    try {
                        topographicPlaceReferenceUpdater.updateTopographicReference(stopPlace);
                        org.rutebanken.netex.model.StopPlace imported = mergingStopPlaceImporter.importStopPlace(stopPlace, containsMobiitiIds, recomputeStopPlacesLocation, true);

                        if (imported != null) {
                            createdStopPlaces.add(imported);
                            stopPlacesCreated.incrementAndGet();
                        }
                    } catch (Exception e) {
                        logger.error("Erreur lors de l'import de l'arrêt {}", stopPlace, e);
                    }
                });

        return distinctByIdAndHighestVersion(createdStopPlaces);
    }



    /**
     * In order to get a distinct list over stop places, and the newest version if duplicates.
     *
     * @param stopPlaces
     * @return unique list with stop places based on ID
     */
    public Collection<org.rutebanken.netex.model.StopPlace> distinctByIdAndHighestVersion(List<org.rutebanken.netex.model.StopPlace> stopPlaces) {
        Map<String, org.rutebanken.netex.model.StopPlace> uniqueStopPlaces = new HashMap<>();
        for (org.rutebanken.netex.model.StopPlace stopPlace : stopPlaces) {
            if (uniqueStopPlaces.containsKey(stopPlace.getId())) {
                org.rutebanken.netex.model.StopPlace existingStopPlace = uniqueStopPlaces.get(stopPlace.getId());
                long existingStopVersion = tryParseLong(existingStopPlace.getVersion());
                long stopPlaceVersion = tryParseLong(stopPlace.getVersion());
                if (existingStopVersion < stopPlaceVersion) {
                    logger.info("Returning newest version of stop place with ID {}: {}", stopPlace.getId(), stopPlace.getVersion());
                    uniqueStopPlaces.put(stopPlace.getId(), stopPlace);
                }
            } else {
                uniqueStopPlaces.put(stopPlace.getId(), stopPlace);
            }
        }
        return uniqueStopPlaces.values();
    }

    private long tryParseLong(String version) {
        try {
            return Long.parseLong(version);
        } catch (NumberFormatException | NullPointerException e) {
            return 0L;
        }
    }


}
