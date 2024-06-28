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

import org.rutebanken.tiamat.exporter.params.TiamatVehicleModeStopPlacetypeMapping;
import org.rutebanken.tiamat.geo.StopPlaceCentroidComputer;
import org.rutebanken.tiamat.geo.ZoneDistanceChecker;
import org.rutebanken.tiamat.importer.KeyValueListAppender;
import org.rutebanken.tiamat.importer.finder.NearbyStopPlaceFinder;
import org.rutebanken.tiamat.importer.finder.NearbyStopsWithSameTypeFinder;
import org.rutebanken.tiamat.importer.finder.StopPlaceFromOriginalIdFinder;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.model.StopTypeEnumeration;
import org.rutebanken.tiamat.model.VehicleModeEnumeration;
import org.rutebanken.tiamat.netex.mapping.NetexMapper;
import org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.rutebanken.tiamat.versioning.VersionCreator;
import org.rutebanken.tiamat.versioning.save.StopPlaceVersionedSaverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@Component
@Qualifier("mergingStopPlaceImporter")
@Transactional
public class MergingStopPlaceImporter {

    private static final Logger logger = LoggerFactory.getLogger(MergingStopPlaceImporter.class);

    /**
     * Enable short distance check for quay merging when merging existing stop places
     */
    public static final boolean EXISTING_STOP_QUAY_MERGE_SHORT_DISTANCE_CHECK_BEFORE_ID_MATCH = false;

    /**
     * Allow the quay merger to add new quays if no match found
     */
    public static final boolean ADD_NEW_QUAYS = true;

    private final StopPlaceFromOriginalIdFinder stopPlaceFromOriginalIdFinder;

    private final NearbyStopsWithSameTypeFinder nearbyStopsWithSameTypeFinder;

    private final NearbyStopPlaceFinder nearbyStopPlaceFinder;

    private final StopPlaceCentroidComputer stopPlaceCentroidComputer;

    private final KeyValueListAppender keyValueListAppender;

    private final QuayMerger quayMerger;

    private final NetexMapper netexMapper;

    private final StopPlaceVersionedSaverService stopPlaceVersionedSaverService;

    private final ZoneDistanceChecker zoneDistanceChecker;

    private final VersionCreator versionCreator;

    private final StopPlaceRepository stopPlaceRepository;

    @Autowired
    public MergingStopPlaceImporter(StopPlaceFromOriginalIdFinder stopPlaceFromOriginalIdFinder,
                                    NearbyStopsWithSameTypeFinder nearbyStopsWithSameTypeFinder, NearbyStopPlaceFinder nearbyStopPlaceFinder,
                                    StopPlaceCentroidComputer stopPlaceCentroidComputer,
                                    KeyValueListAppender keyValueListAppender, QuayMerger quayMerger, NetexMapper netexMapper,
                                    StopPlaceVersionedSaverService stopPlaceVersionedSaverService, ZoneDistanceChecker zoneDistanceChecker, VersionCreator versionCreator, StopPlaceRepository stopPlaceRepository) {
        this.stopPlaceFromOriginalIdFinder = stopPlaceFromOriginalIdFinder;
        this.nearbyStopsWithSameTypeFinder = nearbyStopsWithSameTypeFinder;
        this.nearbyStopPlaceFinder = nearbyStopPlaceFinder;
        this.stopPlaceCentroidComputer = stopPlaceCentroidComputer;
        this.keyValueListAppender = keyValueListAppender;
        this.quayMerger = quayMerger;
        this.netexMapper = netexMapper;
        this.stopPlaceVersionedSaverService = stopPlaceVersionedSaverService;
        this.zoneDistanceChecker = zoneDistanceChecker;
        this.versionCreator = versionCreator;
        this.stopPlaceRepository = stopPlaceRepository;
    }

    /**
     * When importing site frames in multiple threads, and those site frames might contain different stop places that will be merged,
     * we run into the risk of having multiple threads trying to save the same stop place.
     * <p>
     * That's why we use a striped semaphore to not work on the same stop place concurrently. (SiteFrameImporter)
     * it is important to flush the session between each stop place, *before* the semaphore has been released.
     * <p>
     * Attempts to use saveAndFlush or hibernate flush mode always have not been successful.
     */
    public org.rutebanken.netex.model.StopPlace importStopPlace(StopPlace newStopPlace) throws InterruptedException, ExecutionException {

        logger.debug("Transaction active: {}. Isolation level: {}", TransactionSynchronizationManager.isActualTransactionActive(), TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());

        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new RuntimeException("Transaction with required "
                    + "TransactionSynchronizationManager.isActualTransactionActive(): " + TransactionSynchronizationManager.isActualTransactionActive());
        }

        return netexMapper.mapToNetexModel(importStopPlaceWithoutNetexMapping(newStopPlace));
    }

    public StopPlace importStopPlaceWithoutNetexMapping(StopPlace incomingStopPlace) throws InterruptedException, ExecutionException {
        return handleCompletelyNewStopPlace(incomingStopPlace);
    }


    public StopPlace handleCompletelyNewStopPlace(StopPlace incomingStopPlace) throws ExecutionException {

        if (incomingStopPlace.getNetexId() != null) {
            // This should not be necesarry.
            // Because this is a completely new stop.
            // And original netex ID should have been moved to key values.
            incomingStopPlace.setNetexId(null);
            if (incomingStopPlace.getQuays() != null) {
                incomingStopPlace.getQuays().forEach(q -> q.setNetexId(null));
            }
        }

        // TODO OKINA : to check
//        if (incomingStopPlace.getQuays() != null) {
//            Set<Quay> quays = quayMerger.mergeQuays(incomingStopPlace.getQuays(), null, new AtomicInteger(), new AtomicInteger(), ADD_NEW_QUAYS);
//            incomingStopPlace.setQuays(quays);
//            logger.trace("Importing quays for new stop place {}", incomingStopPlace);
//        }

        stopPlaceCentroidComputer.computeCentroidForStopPlace(incomingStopPlace);
        // Ignore incoming version. Always set version to 1 for new stop places.
        logger.debug("New stop place: {}. Setting version to \"1\"", incomingStopPlace.getName());
        versionCreator.createCopy(incomingStopPlace, StopPlace.class);
        StopTypeEnumeration incomingStopPlaceType = incomingStopPlace.getStopPlaceType();
        VehicleModeEnumeration incomingTransportMode = TiamatVehicleModeStopPlacetypeMapping.getVehicleModeEnumeration(incomingStopPlaceType);
        incomingStopPlace.setTransportMode(incomingTransportMode);

        incomingStopPlace = stopPlaceVersionedSaverService.saveNewVersion(incomingStopPlace);
        return updateCache(incomingStopPlace);
    }


    private StopPlace updateCache(StopPlace stopPlace) {
        // Keep the attached stop place reference in case it is merged.

        stopPlaceFromOriginalIdFinder.update(stopPlace);
        nearbyStopPlaceFinder.update(stopPlace);
        logger.info("Saved stop place {}", stopPlace);
        return stopPlace;
    }


}
