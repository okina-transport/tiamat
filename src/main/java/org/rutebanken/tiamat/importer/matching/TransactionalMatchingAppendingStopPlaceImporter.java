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

package org.rutebanken.tiamat.importer.matching;

import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.tiamat.geo.StopPlaceCentroidComputer;
import org.rutebanken.tiamat.importer.AlternativeStopTypes;
import org.rutebanken.tiamat.importer.KeyValueListAppender;
import org.rutebanken.tiamat.importer.finder.StopPlaceByIdFinder;
import org.rutebanken.tiamat.importer.merging.MergingStopPlaceImporter;
import org.rutebanken.tiamat.importer.merging.QuayMerger;
import org.rutebanken.tiamat.model.DataManagedObjectStructure;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopTypeEnumeration;
import org.rutebanken.tiamat.netex.mapping.NetexMapper;
import org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.rutebanken.tiamat.versioning.VersionCreator;
import org.rutebanken.tiamat.versioning.save.StopPlaceVersionedSaverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.toList;

@Component
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class TransactionalMatchingAppendingStopPlaceImporter {

    private static final Logger logger = LoggerFactory.getLogger(TransactionalMatchingAppendingStopPlaceImporter.class);

    private static final boolean CREATE_NEW_QUAYS = true;

    @Autowired
    private KeyValueListAppender keyValueListAppender;

    @Autowired
    private StopPlaceRepository stopPlaceRepository;

    @Autowired
    private QuayMerger quayMerger;

    @Autowired
    private NetexMapper netexMapper;

    @Autowired
    private StopPlaceByIdFinder stopPlaceByIdFinder;

    @Autowired
    private AlternativeStopTypes alternativeStopTypes;

    @Autowired
    private MergingStopPlaceImporter mergingStopPlaceImporter;

    @Value("${onMoveOnly.modeMatch.noMergeSeveralProducers:false}")
    boolean noMergeOnMoveOnly;

    @Autowired
    protected VersionCreator versionCreator;

    @Autowired
    private StopPlaceVersionedSaverService stopPlaceVersionedSaverService;

    @Autowired
    private StopPlaceCentroidComputer stopPlaceCentroidComputer;

    @Autowired
    private QuayMover quayMover;

    public void findAppendAndAdd(final org.rutebanken.tiamat.model.StopPlace incomingStopPlace,
                                 List<StopPlace> matchedStopPlaces,
                                 AtomicInteger stopPlacesCreatedOrUpdated, boolean onMoveOnlyImport) {


        stopPlaceCentroidComputer.computeCentroidForStopPlace(incomingStopPlace);
        List<org.rutebanken.tiamat.model.StopPlace> foundStopPlaces = stopPlaceByIdFinder.findStopPlace(incomingStopPlace);
        final int foundStopPlacesCount = foundStopPlaces.size();

            if (!foundStopPlaces.isEmpty()) {

            if (isDuplicateImportedIds(foundStopPlaces)){
                String errorMsg = "Duplicate imported-id found. Process stopped. Please clean Stop database before importing again";
                logger.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }

            List<org.rutebanken.tiamat.model.StopPlace> filteredStopPlaces = foundStopPlaces
                    .stream()
                    .filter(foundStopPlace -> {

                        if (foundStopPlacesCount == 1) {
                            logger.info("There are only one found stop places. Filtering in stop place regardless of type {}", foundStopPlace);
                            return true;
                        }

                        if (incomingStopPlace.getStopPlaceType() == null) {
                            logger.info("Incoming stop place type is null. Filter in. {}", incomingStopPlace);
                            return true;
                        }

                        if (incomingStopPlace.getStopPlaceType().equals(StopTypeEnumeration.OTHER)) {
                            logger.info("Incoming stop place type is OTHER. Filter in. {}", incomingStopPlace);
                            return true;
                        }

                        if (foundStopPlace.getStopPlaceType().equals(incomingStopPlace.getStopPlaceType())
                                || alternativeStopTypes.matchesAlternativeType(foundStopPlace.getStopPlaceType(), incomingStopPlace.getStopPlaceType())) {
                            return true;
                        }

                        logger.warn("Found match for incoming stop place {}, but the type does not match: {} != {}. Filter out. Incoming stop: {}", foundStopPlace.getNetexId(), incomingStopPlace.getStopPlaceType(), foundStopPlace.getStopPlaceType(), incomingStopPlace);

                        return false;
                    })
                    // Collect distinct on ID
                    .collect(toList());

            foundStopPlaces = filteredStopPlaces;
        }

        if (foundStopPlaces.isEmpty()) {
            logger.warn("Cannot find stop place from IDs or location: {}. StopPlace toString: {}. Will add it as a new one",
                    incomingStopPlace.importedIdAndNameToString(),
                    incomingStopPlace);

            StopPlace newStopPlace = null;
            try {
                newStopPlace = mergingStopPlaceImporter.importStopPlace(incomingStopPlace);
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Problem while adding new stop place", e);
            }
            matchedStopPlaces.add(newStopPlace);
            stopPlacesCreatedOrUpdated.incrementAndGet();

        } else {

            if (foundStopPlaces.size() > 1) {
                logger.warn("Found {} matches for incoming stop place {}. Matches: {}", foundStopPlaces.size(), incomingStopPlace, foundStopPlaces);

                try {
                    foundStopPlaces = checkIfQuaysAlreadyPresentInOtherStopPlace(foundStopPlaces, incomingStopPlace);
                } catch (ExecutionException e) {
                    logger.error("Problem while moving/creating/deleting existing/new stop place", e);
                }
            }

            for (org.rutebanken.tiamat.model.StopPlace existingStopPlace : foundStopPlaces) {

                org.rutebanken.tiamat.model.StopPlace copy = versionCreator.createCopy(existingStopPlace, org.rutebanken.tiamat.model.StopPlace.class);

                logger.debug("Found matching stop place {}", existingStopPlace);

                boolean keyValuesChanged = (
                        keyValueListAppender.appendToOriginalId(NetexIdMapper.ORIGINAL_ID_KEY, incomingStopPlace, copy)
                                && keyValueListAppender.appendToOriginalId(NetexIdMapper.ORIGINAL_NAME_KEY, incomingStopPlace, copy)
                                && keyValueListAppender.appendToOriginalId(NetexIdMapper.ORIGINAL_STOPCODE_KEY, incomingStopPlace, copy)
                );

                if (incomingStopPlace.getTariffZones() != null) {
                    if (copy.getTariffZones() == null) {
                        copy.setTariffZones(new HashSet<>());
                    }
                    copy.getTariffZones().addAll(incomingStopPlace.getTariffZones());
                }

                boolean quayChanged = quayMerger.mergeQuays(incomingStopPlace, copy, CREATE_NEW_QUAYS, onMoveOnlyImport);

                boolean centroidChanged = stopPlaceCentroidComputer.computeCentroidForStopPlace(copy);


                if (quayChanged || keyValuesChanged || centroidChanged  ) {
                    if (existingStopPlace.getParentSiteRef() != null && !existingStopPlace.isParentStopPlace()) {
                        org.rutebanken.tiamat.model.StopPlace existingParentStopPlace = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(existingStopPlace.getParentSiteRef().getRef());
                        org.rutebanken.tiamat.model.StopPlace copyParentStopPlace = versionCreator.createCopy(existingParentStopPlace, org.rutebanken.tiamat.model.StopPlace.class);
                        org.rutebanken.tiamat.model.StopPlace stopPlaceWithParentToCopy = copy;
                        copyParentStopPlace.getChildren().removeIf(stopPlace -> stopPlace.getNetexId().equals(stopPlaceWithParentToCopy.getNetexId()));
                        copyParentStopPlace.getChildren().add(copy);
                        copyParentStopPlace = stopPlaceVersionedSaverService.saveNewVersion(existingParentStopPlace, copyParentStopPlace);
                        copy = copyParentStopPlace.getChildren().stream().filter(stopPlace -> stopPlace.getNetexId().equals(stopPlaceWithParentToCopy.getNetexId())).findFirst().get();
                    }
                    else{
                        copy = stopPlaceVersionedSaverService.saveNewVersion(existingStopPlace, copy);
                    }
                }

                String netexId = copy.getNetexId();

                matchedStopPlaces.removeIf(stopPlace -> stopPlace.getId().equals(netexId));

                matchedStopPlaces.add(netexMapper.mapToNetexModel(copy));

                stopPlacesCreatedOrUpdated.incrementAndGet();

            }
        }
    }


    /**
     * Read a list of stopPlaces and verify that each imported-id is only attached to a single netex id.
     * If not, error is logged
     * @param stopPlaceList
     * @return
     *  true : an imported-id is attached to 2 or more netex id
     *  false : no duplicates detected
     */
    private boolean isDuplicateImportedIds(List<org.rutebanken.tiamat.model.StopPlace>  stopPlaceList){
        Map<String,String> importedIdMap = new HashMap<>();
        boolean isDuplicateImportedIds = false;
        for (org.rutebanken.tiamat.model.StopPlace stopPlace : stopPlaceList) {

            isDuplicateImportedIds = isDuplicateImportedIds || isDuplicateImportIdInObject(stopPlace,importedIdMap);

            for (Quay quay : stopPlace.getQuays()) {
                isDuplicateImportedIds = isDuplicateImportedIds || isDuplicateImportIdInObject(quay,importedIdMap);
            }

        }
        return isDuplicateImportedIds;
    }

    private boolean isDuplicateImportIdInObject(DataManagedObjectStructure dataObj, Map<String,String> importedIdMap ){
        String netexId = dataObj.getNetexId();
        boolean isDuplicateImportedIds = false;

        Set<String> importedIds = dataObj.getOrCreateValues(NetexIdMapper.ORIGINAL_ID_KEY);
        for (String importedId : importedIds) {
            if (importedIdMap.containsKey(importedId) && importedIdMap.get(importedId) != netexId){
                logger.error("DUPLICATE USE OF IMPORTED-ID:" + importedId + " (" + netexId + "," + importedIdMap.get(importedId) + ")");
                isDuplicateImportedIds = true;
            }else{
                importedIdMap.put(importedId,netexId);
            }
        }
        return isDuplicateImportedIds;
    }


    private List<org.rutebanken.tiamat.model.StopPlace> checkIfQuaysAlreadyPresentInOtherStopPlace(List<org.rutebanken.tiamat.model.StopPlace> foundStopPlaces, org.rutebanken.tiamat.model.StopPlace incomingStopPlace) throws ExecutionException {
        quayMover.doMove(foundStopPlaces, incomingStopPlace);
        return stopPlaceByIdFinder.findStopPlace(incomingStopPlace);
    }
}
