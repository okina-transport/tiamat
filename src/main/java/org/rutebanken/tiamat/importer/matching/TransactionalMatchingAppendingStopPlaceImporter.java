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
import org.rutebanken.tiamat.exporter.params.TiamatVehicleModeStopPlacetypeMapping;
import org.rutebanken.tiamat.geo.StopPlaceCentroidComputer;
import org.rutebanken.tiamat.geo.ZoneDistanceChecker;
import org.rutebanken.tiamat.importer.*;
import org.rutebanken.tiamat.importer.finder.NearbyStopPlaceFinder;
import org.rutebanken.tiamat.importer.finder.SimpleNearbyStopPlaceFinder;
import org.rutebanken.tiamat.importer.finder.StopPlaceByIdFinder;
import org.rutebanken.tiamat.importer.merging.MergingStopPlaceImporter;
import org.rutebanken.tiamat.importer.merging.QuayMerger;
import org.rutebanken.tiamat.model.*;
import org.rutebanken.tiamat.netex.mapping.NetexMapper;
import org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.rutebanken.tiamat.repository.TariffZoneRepository;
import org.rutebanken.tiamat.rest.exception.TiamatBusinessException;
import org.rutebanken.tiamat.service.stopplace.StopPlaceDeleter;
import org.rutebanken.tiamat.service.stopplace.StopPlaceQuayMover;
import org.rutebanken.tiamat.versioning.VersionCreator;
import org.rutebanken.tiamat.versioning.save.StopPlaceVersionedSaverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper.RAIL_UIC_KEY;

@Component
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class TransactionalMatchingAppendingStopPlaceImporter {

    private static final Logger logger = LoggerFactory.getLogger(TransactionalMatchingAppendingStopPlaceImporter.class);

    private static final boolean CREATE_NEW_QUAYS = true;

    private static final boolean ALLOW_OTHER_TYPE_AS_ANY_MATCH = true;

    @Autowired
    private KeyValueListAppender keyValueListAppender;

    @Autowired
    private StopPlaceRepository stopPlaceRepository;

    @Autowired
    private TariffZoneRepository tariffZoneRepository;

    @Autowired
    private QuayMerger quayMerger;

    @Autowired
    private NetexMapper netexMapper;

    @Autowired
    private NearbyStopPlaceFinder nearbyStopPlaceFinder;

    @Autowired
    private StopPlaceByIdFinder stopPlaceByIdFinder;

    @Autowired
    private SimpleNearbyStopPlaceFinder simpleNearbyStopPlaceFinder;

    @Autowired
    private ZoneDistanceChecker zoneDistanceChecker;

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

    @Value("${stopPlace.sharing.policy}")
    protected StopPlaceSharingPolicy sharingPolicy;

    public void findAppendAndAdd(final org.rutebanken.tiamat.model.StopPlace incomingStopPlace,
                                 List<StopPlace> matchedStopPlaces,
                                 AtomicInteger stopPlacesCreatedOrUpdated, ImportParams importParams) throws TiamatBusinessException {


        stopPlaceCentroidComputer.computeCentroidForStopPlace(incomingStopPlace);
        List<org.rutebanken.tiamat.model.StopPlace> foundStopPlaces = stopPlaceByIdFinder.findStopPlace(incomingStopPlace);
        foundStopPlaces = removeExpiredVersions(foundStopPlaces);


        final int foundStopPlacesCount = foundStopPlaces.size();

        if (!foundStopPlaces.isEmpty()) {


            executeQualityChecksOnRecoveredData(foundStopPlaces, incomingStopPlace);


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
                newStopPlace = mergingStopPlaceImporter.importStopPlace(incomingStopPlace, false);
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


                boolean keyValuesChanged = false;

                if (keyValueListAppender.appendToOriginalId(NetexIdMapper.ORIGINAL_ID_KEY, incomingStopPlace, copy)) {
                    keyValuesChanged = true;
                }
                if (keyValueListAppender.appendToOriginalId(NetexIdMapper.ORIGINAL_NAME_KEY, incomingStopPlace, copy)) {
                    keyValuesChanged = true;
                }
                if (keyValueListAppender.appendToOriginalId(NetexIdMapper.ORIGINAL_STOPCODE_KEY, incomingStopPlace, copy)) {
                    keyValuesChanged = true;
                }
                if (keyValueListAppender.appendKeyValue(NetexIdMapper.EXTERNAL_REF, incomingStopPlace, copy)) {
                    keyValuesChanged = true;
                }
                if (keyValueListAppender.appendKeyValue(NetexIdMapper.FARE_ZONE, incomingStopPlace, copy)) {
                    keyValuesChanged = true;
                }
                if (keyValueListAppender.appendKeyValue(RAIL_UIC_KEY, incomingStopPlace, copy)) {
                    keyValuesChanged = true;
                }

                boolean nameChanged = false;
                boolean wheelChairChanged = false;
                boolean tariffZoneChanged = false;
                boolean privateCodeChanged = false;
                boolean hasTransportModeChanged = false;


                hasTransportModeChanged = updateTransportMode(incomingStopPlace, copy);


                if (incomingStopPlace.getTariffZones() != null) {
                    if (copy.getTariffZones() == null) {
                        copy.setTariffZones(new HashSet<>());
                        tariffZoneChanged = true;
                    } else {
                        tariffZoneChanged = !incomingStopPlace.getTariffZones().equals(copy.getTariffZones());
                        for (TariffZoneRef tariffZoneRef : incomingStopPlace.getTariffZones()) {
                            String netexId = tariffZoneRepository.findFirstByKeyValue(NetexIdMapper.FARE_ZONE, tariffZoneRef.getRef());
                            tariffZoneRef.setRef(netexId);
                            copy.getTariffZones().add(tariffZoneRef);
                        }
                    }

                }

                if (!importParams.keepStopNames && !incomingStopPlace.getName().getValue().equals(copy.getName().getValue())) {
                    nameChanged = true;
                    copy.setName(incomingStopPlace.getName());
                }

                if (importParams.updateStopAccessibility) {
                    wheelChairChanged = updateWheelchairIfNeeded(copy, incomingStopPlace);
                }

                if (incomingStopPlace.getPrivateCode() != null && !incomingStopPlace.getPrivateCode().equals(existingStopPlace.getPrivateCode())) {
                    privateCodeChanged = true;
                    copy.setPrivateCode(incomingStopPlace.getPrivateCode());
                }

                boolean quayChanged = quayMerger.mergeQuays(incomingStopPlace, copy, CREATE_NEW_QUAYS, importParams);

                boolean centroidChanged = stopPlaceCentroidComputer.computeCentroidForStopPlace(copy);


                if (quayChanged || keyValuesChanged || centroidChanged || nameChanged || wheelChairChanged || tariffZoneChanged || privateCodeChanged || hasTransportModeChanged) {
                    if (existingStopPlace.getParentSiteRef() != null && !existingStopPlace.isParentStopPlace()) {
                        org.rutebanken.tiamat.model.StopPlace existingParentStopPlace = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(existingStopPlace.getParentSiteRef().getRef());
                        org.rutebanken.tiamat.model.StopPlace copyParentStopPlace = versionCreator.createCopy(existingParentStopPlace, org.rutebanken.tiamat.model.StopPlace.class);
                        org.rutebanken.tiamat.model.StopPlace stopPlaceWithParentToCopy = copy;
                        copyParentStopPlace.getChildren().removeIf(stopPlace -> stopPlace.getNetexId().equals(stopPlaceWithParentToCopy.getNetexId()));
                        copyParentStopPlace.getChildren().add(copy);
                        copyParentStopPlace = stopPlaceVersionedSaverService.saveNewVersion(existingParentStopPlace, copyParentStopPlace);
                        copy = copyParentStopPlace.getChildren().stream().filter(stopPlace -> stopPlace.getNetexId().equals(stopPlaceWithParentToCopy.getNetexId())).findFirst().get();
                    } else {
                        copy = stopPlaceVersionedSaverService.saveNewVersion(existingStopPlace, copy);
                    }
                }

                copyPropertiesToParentStopPlace(copy);

                String netexId = copy.getNetexId();

                matchedStopPlaces.removeIf(stopPlace -> stopPlace.getId().equals(netexId));

                matchedStopPlaces.add(netexMapper.mapToNetexModel(copy));

                stopPlacesCreatedOrUpdated.incrementAndGet();

            }
        }
    }

    private List<org.rutebanken.tiamat.model.StopPlace> removeExpiredVersions(List<org.rutebanken.tiamat.model.StopPlace> foundStopPlaces) {

        if (foundStopPlaces == null || foundStopPlaces.isEmpty()){
            return new ArrayList<>();
        }

        List<org.rutebanken.tiamat.model.StopPlace> filteredStopPlaces = new ArrayList<>();
        for (org.rutebanken.tiamat.model.StopPlace foundStopPlace : foundStopPlaces) {
            if(foundStopPlace.getValidBetween() != null &&
                    (foundStopPlace.getValidBetween().getToDate() == null || foundStopPlace.getValidBetween().getToDate().isAfter(Instant.now()))){
                filteredStopPlaces.add(foundStopPlace);
            }
        }
        return filteredStopPlaces;
    }

    private boolean updateTransportMode(org.rutebanken.tiamat.model.StopPlace incomingStopPlace, org.rutebanken.tiamat.model.StopPlace existingStopPlace) {

        StopTypeEnumeration incomingStopPlaceType = incomingStopPlace.getStopPlaceType();
        VehicleModeEnumeration incomingTransportMode = TiamatVehicleModeStopPlacetypeMapping.getVehicleModeEnumeration(incomingStopPlaceType);
        if (isVehicleModeAlreadyExisting(existingStopPlace, incomingTransportMode)) {
            return false;
        }

        if (existingStopPlace.getTransportMode() == null){
            // to repair old data with no transport mode defined
            fillEmptyTransportMode(existingStopPlace);
        }

        Set<VehicleModeEnumeration> completeTransportModeSet = new HashSet<>();
        completeTransportModeSet.add(existingStopPlace.getTransportMode());
        if (!existingStopPlace.getOtherTransportModes().isEmpty()){
            completeTransportModeSet.addAll(existingStopPlace.getOtherTransportModes());
        }
        completeTransportModeSet.add(incomingTransportMode);

        VehicleModeEnumeration primaryTransportMode = getPrimaryTransportMode(completeTransportModeSet);
        existingStopPlace.setTransportMode(primaryTransportMode);

        completeTransportModeSet.remove(primaryTransportMode);
        existingStopPlace.getOtherTransportModes().clear();
        existingStopPlace.getOtherTransportModes().addAll(completeTransportModeSet);

        if (!isStopPlaceTypeMatchingPrimaryVehicleMode(existingStopPlace, primaryTransportMode)) {
            existingStopPlace.setStopPlaceType(incomingStopPlace.getStopPlaceType());
        }
        return true;

    }

    private void fillEmptyTransportMode(org.rutebanken.tiamat.model.StopPlace existingStopPlace) {
        if (StopTypeEnumeration.ONSTREET_BUS.equals(existingStopPlace.getStopPlaceType())){
            existingStopPlace.setTransportMode(VehicleModeEnumeration.BUS);
        }

        if (StopTypeEnumeration.ONSTREET_TRAM.equals(existingStopPlace.getStopPlaceType()) || StopTypeEnumeration.METRO_STATION.equals(existingStopPlace.getStopPlaceType())){
            existingStopPlace.setTransportMode(VehicleModeEnumeration.TRAM);
        }

        if (StopTypeEnumeration.FERRY_STOP.equals(existingStopPlace.getStopPlaceType())){
            existingStopPlace.setTransportMode(VehicleModeEnumeration.FERRY);
        }

        if (StopTypeEnumeration.RAIL_STATION.equals(existingStopPlace.getStopPlaceType())){
            existingStopPlace.setTransportMode(VehicleModeEnumeration.RAIL);
        }

        if (StopTypeEnumeration.METRO_STATION.equals(existingStopPlace.getStopPlaceType())){
            existingStopPlace.setTransportMode(VehicleModeEnumeration.TRAM);
        }
    }

    private boolean isStopPlaceTypeMatchingPrimaryVehicleMode(org.rutebanken.tiamat.model.StopPlace stopPlace, VehicleModeEnumeration primaryTransportMode) {
        if (VehicleModeEnumeration.RAIL.equals(primaryTransportMode) &&
                StopTypeEnumeration.RAIL_STATION.equals(stopPlace.getStopPlaceType())) {
            return true;
        }

        if (VehicleModeEnumeration.BUS.equals(primaryTransportMode) &&
                (StopTypeEnumeration.ONSTREET_BUS.equals(stopPlace.getStopPlaceType()) || StopTypeEnumeration.BUS_STATION.equals(stopPlace.getStopPlaceType()))) {
            return true;
        }

        if (VehicleModeEnumeration.COACH.equals(primaryTransportMode) &&
                StopTypeEnumeration.COACH_STATION.equals(stopPlace.getStopPlaceType())) {
            return true;
        }

        if (VehicleModeEnumeration.TRAM.equals(primaryTransportMode) &&
                (StopTypeEnumeration.ONSTREET_TRAM.equals(stopPlace.getStopPlaceType()) || StopTypeEnumeration.METRO_STATION.equals(stopPlace.getStopPlaceType()) || StopTypeEnumeration.TRAM_STATION.equals(stopPlace.getStopPlaceType()))) {
            return true;
        }

        if (VehicleModeEnumeration.AIR.equals(primaryTransportMode) &&
                StopTypeEnumeration.AIRPORT.equals(stopPlace.getStopPlaceType())) {
            return true;
        }

        if (VehicleModeEnumeration.FERRY.equals(primaryTransportMode) &&
                (StopTypeEnumeration.FERRY_STOP.equals(stopPlace.getStopPlaceType()) || StopTypeEnumeration.FERRY_PORT.equals(stopPlace.getStopPlaceType()))) {
            return true;
        }

        if (VehicleModeEnumeration.WATER.equals(primaryTransportMode) &&
                StopTypeEnumeration.HARBOUR_PORT.equals(stopPlace.getStopPlaceType())) {
            return true;
        }

        if (VehicleModeEnumeration.CABLEWAY.equals(primaryTransportMode) &&
                StopTypeEnumeration.LIFT_STATION.equals(stopPlace.getStopPlaceType())) {
            return true;
        }

        if (VehicleModeEnumeration.OTHER.equals(primaryTransportMode) &&
                StopTypeEnumeration.OTHER.equals(stopPlace.getStopPlaceType())) {
            return true;
        }

        return false;
    }

    private VehicleModeEnumeration getPrimaryTransportMode(Set<VehicleModeEnumeration> completeTransportModeSet) {

        if (completeTransportModeSet.contains(VehicleModeEnumeration.RAIL)) {
            return VehicleModeEnumeration.RAIL;
        }

        if (completeTransportModeSet.contains(VehicleModeEnumeration.BUS)) {
            return VehicleModeEnumeration.BUS;
        }

        if (completeTransportModeSet.contains(VehicleModeEnumeration.COACH)) {
            return VehicleModeEnumeration.COACH;
        }

        if (completeTransportModeSet.contains(VehicleModeEnumeration.TRAM)) {
            return VehicleModeEnumeration.TRAM;
        }
        return new ArrayList<>(completeTransportModeSet).get(0);
    }


    private boolean isVehicleModeAlreadyExisting(org.rutebanken.tiamat.model.StopPlace stopPlace, VehicleModeEnumeration vehicleMode) {
        return (stopPlace.getTransportMode() != null && stopPlace.getTransportMode().equals(vehicleMode)) ||
                (stopPlace.getOtherTransportModes() != null && stopPlace.getOtherTransportModes().contains(vehicleMode));
    }

    private boolean updateWheelchairIfNeeded(org.rutebanken.tiamat.model.StopPlace existingStopPlace, org.rutebanken.tiamat.model.StopPlace incomingStopPlace) {
        Optional<LimitationStatusEnumeration> existingWheelchairLimitationOpt = ImporterUtils.getWheelchairLimitation(existingStopPlace);
        Optional<LimitationStatusEnumeration> incomingWheelchairLimitationOpt = ImporterUtils.getWheelchairLimitation(incomingStopPlace);
        boolean updated = false;

        if (!existingWheelchairLimitationOpt.equals(incomingWheelchairLimitationOpt) && incomingWheelchairLimitationOpt.isPresent()) {
            updated = true;
            ImporterUtils.updateWheelchairLimitation(existingStopPlace, incomingWheelchairLimitationOpt.get());
        }
        return updated;
    }

    /**
     * Launch quality checks on recovered data.
     * - If duplicate ids are found in db, an exception is raised
     * - if stop place type is inconsistent between incoming stop place and recovered stop place from database, an exception is raised
     *
     * @param foundStopPlaces   List of stop places recovered from DB
     * @param incomingStopPlace New incoming stop place
     */
    private void executeQualityChecksOnRecoveredData(List<org.rutebanken.tiamat.model.StopPlace> foundStopPlaces, org.rutebanken.tiamat.model.StopPlace incomingStopPlace) throws TiamatBusinessException {

        String errorMsg;
        if (isDuplicateImportedIds(foundStopPlaces)) {
            errorMsg = "Duplicate imported-id found. Process stopped. Please clean Stop database before importing again";
            logger.error(errorMsg);
            throw new TiamatBusinessException(TiamatBusinessException.DUPLICATE_IMPORTED_ID, errorMsg);
        }
    }

    private void copyPropertiesToParentStopPlace(org.rutebanken.tiamat.model.StopPlace copy) {
        if (copy.getKeyValues() == null || !copy.getKeyValues().containsKey(RAIL_UIC_KEY)) {
            return;
        }

        String netexId = copy.getNetexId();
        org.rutebanken.tiamat.model.StopPlace importedStopPlace = stopPlaceRepository.findFirstByNetexIdOrderByVersionDescAndInitialize(netexId);
        if (importedStopPlace.getParentSiteRef() != null && copy.getKeyValues().get(RAIL_UIC_KEY).getItems().stream().findFirst().isPresent()) {
            String railUIC = copy.getKeyValues().get(RAIL_UIC_KEY).getItems().stream().findFirst().get();
            String parentNetexId = importedStopPlace.getParentSiteRef().getRef();
            org.rutebanken.tiamat.model.StopPlace parentStopPlace = stopPlaceRepository.findFirstByNetexIdOrderByVersionDescAndInitialize(parentNetexId);

            Set<String> parentRailUIC = parentStopPlace.getOrCreateValues(RAIL_UIC_KEY);
            parentRailUIC.add(railUIC);

        }

    }


    /**
     * Read a list of stopPlaces and verify that each imported-id is only attached to a single netex id.
     * If not, error is logged
     *
     * @param stopPlaceList
     * @return true : an imported-id is attached to 2 or more netex id
     * false : no duplicates detected
     */
    private boolean isDuplicateImportedIds(List<org.rutebanken.tiamat.model.StopPlace> stopPlaceList) {
        Map<String, String> importedIdMap = new HashMap<>();
        boolean isDuplicateImportedIds = false;
        for (org.rutebanken.tiamat.model.StopPlace stopPlace : stopPlaceList) {

            isDuplicateImportedIds = isDuplicateImportedIds || isDuplicateImportIdInObject(stopPlace, importedIdMap);

            for (Quay quay : stopPlace.getQuays()) {
                isDuplicateImportedIds = isDuplicateImportedIds || isDuplicateImportIdInObject(quay, importedIdMap);
            }

        }
        return isDuplicateImportedIds;
    }

    private boolean isDuplicateImportIdInObject(DataManagedObjectStructure dataObj, Map<String, String> importedIdMap) {
        String netexId = dataObj.getNetexId();
        boolean isDuplicateImportedIds = false;

        Set<String> importedIds = dataObj.getOrCreateValues(NetexIdMapper.ORIGINAL_ID_KEY);
        for (String importedId : importedIds) {
            if (importedIdMap.containsKey(importedId) && importedIdMap.get(importedId) != netexId) {
                logger.error("DUPLICATE USE OF IMPORTED-ID:" + importedId + " (" + netexId + "," + importedIdMap.get(importedId) + ")");
                isDuplicateImportedIds = true;
            } else {
                importedIdMap.put(importedId, netexId);
            }
        }
        return isDuplicateImportedIds;
    }


    private List<org.rutebanken.tiamat.model.StopPlace> checkIfQuaysAlreadyPresentInOtherStopPlace(List<org.rutebanken.tiamat.model.StopPlace> foundStopPlaces, org.rutebanken.tiamat.model.StopPlace incomingStopPlace) throws ExecutionException {
        quayMover.doMove(foundStopPlaces, incomingStopPlace);
        return stopPlaceByIdFinder.findStopPlace(incomingStopPlace);
    }
}
