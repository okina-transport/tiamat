package org.rutebanken.tiamat.importer.mdm;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.rutebanken.tiamat.config.TiamatProperties;
import org.rutebanken.tiamat.feign.mdm.IdentifierToCheck;
import org.rutebanken.tiamat.feign.mdm.MdmFeignClient;
import org.rutebanken.tiamat.feign.mdm.MergeIdentifier;
import org.rutebanken.tiamat.feign.mdm.OkinaIdentifier;
import org.rutebanken.tiamat.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MdmService {

    private static final Logger logger = LoggerFactory.getLogger(MdmService.class);
    private static final String STOP_PLACE_QUALIFIER = ":StopPlace:";
    private static final String QUAY_QUALIFIER = ":Quay:";

    private final MdmFeignClient mdmFeignClient;

    private final TiamatProperties tiamatProperties;

    @Value("${netex.validPrefix:MOBIITI}")
    private String validNetexPrefix;

    public MdmService(MdmFeignClient mdmFeignClient, TiamatProperties tiamatProperties) {
        this.mdmFeignClient = mdmFeignClient;
        this.tiamatProperties = tiamatProperties;
    }

    public static String getOriginalIdFromTridentImportedId(String id) {
        String result = id;
        int identifierIndex = StringUtils.lastIndexOf(id, ":");
        if (identifierIndex != -1) {
            result = StringUtils.substring(id, identifierIndex + 1);
        }
        return result;
    }

    public static Long getIdentifierFromNetexId(String netexId) {
        Long result = null;
        int identifierIndex = StringUtils.lastIndexOf(netexId, ":");
        if (identifierIndex != -1) {
            result = Long.valueOf(StringUtils.substring(netexId, identifierIndex + 1));
        }
        return result;
    }

    private static OkinaIdentifier buildMdmIdentifierWithDataset(String rawNetexId, String rawOriginalId) {
        OkinaIdentifier mdmData = new OkinaIdentifier();
        String[] originalIdParts = rawOriginalId.split(":");
        mdmData.setDataset(originalIdParts[0]);
        mdmData.setOriginalId(originalIdParts[2]);
        mdmData.setSuperId(getIdentifierFromNetexId(rawNetexId));
        return mdmData;
    }

    private static OkinaIdentifier buildMdmIdentifier(String rawNetexId, String rawOriginalId) {
        OkinaIdentifier mdmData = new OkinaIdentifier();
        mdmData.setOriginalId(rawOriginalId);
        mdmData.setSuperId(getIdentifierFromNetexId(rawNetexId));
        return mdmData;
    }

    private static void getIdentifierToCheckInMdm(StopPlace recoveredStopPlace, IdentifierToCheck identifierToCheck) {
        Long stopIdentifier = getIdentifierFromNetexId(recoveredStopPlace.getNetexId());
        identifierToCheck.stopPlaceMap().put(stopIdentifier, recoveredStopPlace);
        identifierToCheck.stopPlaceMdmIds().add(stopIdentifier);

        for (Quay quay : recoveredStopPlace.getQuays()) {
            Long quayIdentifier = getIdentifierFromNetexId(quay.getNetexId());
            identifierToCheck.quayMap().put(quayIdentifier, quay);
            identifierToCheck.quayMdmIds().add(quayIdentifier);
        }
    }

    private static List<OkinaIdentifier> buildInputQuayIdentifiers(String dataset, StopPlace incomingStopPlace) {
        List<OkinaIdentifier> quayIdentifiers = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(incomingStopPlace.getQuays())) {
            for (Quay quay : incomingStopPlace.getQuays()) {
                quayIdentifiers.add(buildInputQuayIdentifier(dataset, quay));
            }
        }
        return quayIdentifiers;
    }

    private static OkinaIdentifier buildInputStopIdentifier(StopPlace incomingStopPlace) {
        OkinaIdentifier stopIdentifier = new OkinaIdentifier();
        stopIdentifier.setDataset(incomingStopPlace.getProvider().toUpperCase());

        for (String originalId : incomingStopPlace.getOriginalIds()) {
            String[] originalIdTab = originalId.split(":");
            if (originalIdTab.length == 3) {
                stopIdentifier.setOriginalId(originalIdTab[2]);
            } else {
                stopIdentifier.setOriginalId(originalId);
            }
        }

        if (incomingStopPlace.getNetexId() != null) {
            stopIdentifier.setSuperId(Long.valueOf(incomingStopPlace.getNetexId().split(":")[2]));
        }
        return stopIdentifier;
    }

    private static OkinaIdentifier buildInputQuayIdentifier(String dataset, Quay quay) {
        OkinaIdentifier quayIdentifier = new OkinaIdentifier();
        quayIdentifier.setDataset(dataset);

        for (String originalId : quay.getOriginalIds()) {
            String[] originalIdTab = originalId.split(":");
            if (originalIdTab.length == 3) {
                quayIdentifier.setOriginalId(originalIdTab[2]);
            } else {
                quayIdentifier.setOriginalId(originalId);
            }
        }

        if (quay.getNetexId() != null) {
            quayIdentifier.setSuperId(getIdentifierFromNetexId(quay.getNetexId()));
        }
        return quayIdentifier;
    }

    public boolean isMdmEnabled() {
        return tiamatProperties.isMdmEnabled();
    }

    public void fillImportedIds(List<StopPlace> stopPlaces) {
        if (stopPlaces.isEmpty() || !tiamatProperties.isMdmEnabled()) {
            return;
        }
        IdentifierToCheck identifierToCheck = new IdentifierToCheck(new HashSet<>(), new HashSet<>(), new HashMap<>(), new HashMap<>());

        for (org.rutebanken.tiamat.model.StopPlace recoveredStopPlace : stopPlaces) {
            getIdentifierToCheckInMdm(recoveredStopPlace, identifierToCheck);
            for (StopPlace child : recoveredStopPlace.getChildren()) {
                getIdentifierToCheckInMdm(child, identifierToCheck);
            }
        }

        List<OkinaIdentifier> stopPlacesMdmData = mdmFeignClient.getStopPlaceIdentifiers(identifierToCheck.stopPlaceMdmIds().stream().toList());
        StopPlace stopPlaceToComplete;
        for (OkinaIdentifier okinaIdentifier : stopPlacesMdmData) {
            stopPlaceToComplete = identifierToCheck.stopPlaceMap().get(okinaIdentifier.getSuperId());
            if (stopPlaceToComplete != null) {
                String completeOriginalId = okinaIdentifier.getDataset() + STOP_PLACE_QUALIFIER + okinaIdentifier.getOriginalId();
                stopPlaceToComplete.getOriginalIds().add(completeOriginalId);
            }
        }
        if (CollectionUtils.isNotEmpty(identifierToCheck.quayMdmIds())) {
            List<OkinaIdentifier> quaysMdmData = mdmFeignClient.getQuayIdentifiers(identifierToCheck.quayMdmIds().stream().toList());
            Quay quayToComplete;
            for (OkinaIdentifier okinaIdentifier : quaysMdmData) {
                quayToComplete = identifierToCheck.quayMap().get(okinaIdentifier.getSuperId());
                if (quayToComplete != null) {
                    quayToComplete.getOriginalIds().add(okinaIdentifier.getDataset() + QUAY_QUALIFIER + okinaIdentifier.getOriginalId());
                }
            }
        }
    }

    /**
     * Request MDM to recover existing MDM ids
     *
     * @param incomingStopPlace
     * @return
     */
    public Optional<Long> getExistingStopPlaceMdmIds(StopPlace incomingStopPlace) {
        OkinaIdentifier stopIdentifier = buildInputStopIdentifier(incomingStopPlace);
        OkinaIdentifier stopPlaceMdmData = mdmFeignClient.getStopPlaceIdentifiersByOriginalId(stopIdentifier);
        return stopPlaceMdmData != null ? Optional.of(stopPlaceMdmData.getSuperId()) : Optional.empty();
    }

    public Set<Long> getStopPlaceIdsByProvider(String provider){
        return mdmFeignClient.getStopPlaceIdentifiersByDataset(provider);
    }

    public void generateIdentifier(PointOfInterest incomingPointOfInterest) {
        if (!tiamatProperties.isMdmEnabled()) {
            return;
        }

        OkinaIdentifier okinaIdentifier = new OkinaIdentifier();
        okinaIdentifier.setOriginalId(incomingPointOfInterest.getOriginalIds().iterator().next());
        List<OkinaIdentifier> mdmData = mdmFeignClient.generatePoiIdentifiers(List.of(okinaIdentifier));
        Long superId = mdmData.get(0).getSuperId();
        incomingPointOfInterest.setNetexId(validNetexPrefix + ":PointOfInterest:" + superId);
        incomingPointOfInterest.getOriginalIds().clear();
    }

    public void generateIdentifier(Parking parking) {
        if (!tiamatProperties.isMdmEnabled()) {
            return;
        }

        OkinaIdentifier okinaIdentifier = new OkinaIdentifier();
        okinaIdentifier.setOriginalId(parking.getOriginalIds().iterator().next());
        List<OkinaIdentifier> mdmData = mdmFeignClient.generateParkingIdentifiers(List.of(okinaIdentifier));
        Long superId = mdmData.get(0).getSuperId();
        parking.setNetexId(validNetexPrefix + ":Parking:" + superId);
        parking.getOriginalIds().clear();
    }

    public void generateIdentifier(StopPlace incomingStopPlace) {
        if (!tiamatProperties.isMdmEnabled()) {
            return;
        }

        if (StringUtils.isEmpty(incomingStopPlace.getNetexId()) || !incomingStopPlace.getNetexId().contains(validNetexPrefix + STOP_PLACE_QUALIFIER)) {
            // first case : stopPlace coming from chouette. New super ids need to be generated by MDM
            try {
                List<OkinaIdentifier> stopIdentifiers = new ArrayList<>();
                OkinaIdentifier stopIdentifier = buildInputStopIdentifier(incomingStopPlace);
                List<OkinaIdentifier> quayIdentifiers = new ArrayList<>(buildInputQuayIdentifiers(stopIdentifier.getDataset(), incomingStopPlace));
                if (CollectionUtils.isNotEmpty(incomingStopPlace.getChildren())) {
                    for (StopPlace stopPlace : incomingStopPlace.getChildren()) {
                        stopIdentifiers.add(buildInputStopIdentifier(stopPlace));
                        quayIdentifiers.addAll(buildInputQuayIdentifiers(stopIdentifier.getDataset(), stopPlace));
                    }
                }
                stopIdentifiers.add(stopIdentifier);
                logger.debug("trying to save id : dataset {} - original id {} - super id {} in mdm",
                        stopIdentifier.getDataset(),
                        stopIdentifier.getOriginalId(),
                        stopIdentifier.getSuperId()
                );
                List<OkinaIdentifier> mdmStopIdentifiers = mdmFeignClient.generateStopIdentifiers(stopIdentifiers);
                fillMdmId(incomingStopPlace, mdmStopIdentifiers);
                logger.debug("Super id generated by MDM : {}", incomingStopPlace.getNetexId());

                List<OkinaIdentifier> mdmQuayIdentifiers = mdmFeignClient.generateQuayIdentifiers(quayIdentifiers);
                fillMdmId(incomingStopPlace.getQuays(), mdmQuayIdentifiers);
                removeImportedIds(incomingStopPlace);
            } catch (Exception e) {
                logger.error("Failed to create stop identifiers in mdm: {}", e.getMessage());
            }
        }
    }

    public void mergeStopIdentifier(String fromIdentifier, String targetIdentifier) {
        if (tiamatProperties.isMdmEnabled()) {
            Long originIdentifier = getIdentifierFromNetexId(fromIdentifier);
            Long target = getIdentifierFromNetexId(targetIdentifier);
            MergeIdentifier mergeIdentifier = new MergeIdentifier();
            mergeIdentifier.setOriginIdentifier(originIdentifier);
            mergeIdentifier.setTargetIdentifier(target);
            mdmFeignClient.mergeStopIdentifiers(mergeIdentifier);
        }
    }

    public void removeImportedIds(StopPlace incomingStopPlace) {
        incomingStopPlace.getOriginalIds().clear();
        incomingStopPlace.getQuays().forEach(quay -> quay.getOriginalIds().clear());

    }

    public List<OkinaIdentifier> getAllQuaysFromSuperId(Set<Long> superIds) {
        return mdmFeignClient.getQuayIdentifiers(superIds.stream().toList());
    }

    public List<OkinaIdentifier> getAllStopPlacesFromSuperId(Set<Long> superIds) {
        return mdmFeignClient.getStopPlaceIdentifiers(superIds.stream().toList());
    }

    public List<OkinaIdentifier> getAllPoisFromSuperId(Set<Long> superIds) {
        return mdmFeignClient.getPoisIdentifiers(superIds.stream().toList());
    }

    public List<OkinaIdentifier> getAllParkingsFromSuperId(Set<Long> superIds) {
        return mdmFeignClient.getParkingIdentifiers(superIds.stream().toList());
    }

    public List<OkinaIdentifier> getExistingQuaysMdmIds(String datasetId, Set<Quay> quays) {

        List<OkinaIdentifier> quayIdentifiers = new ArrayList<>();
        for (Quay quay : quays) {
            quayIdentifiers.add(buildInputQuayIdentifier(datasetId, quay));
        }

        return mdmFeignClient.getQuayIdentifiersByOriginalId(quayIdentifiers);
    }

    public OkinaIdentifier getExistingPoiMdmIds(PointOfInterest poi) {
        return getExistingPoiMdmIdsFromImportedId(poi.getOriginalIds().iterator().next());
    }

    public OkinaIdentifier getExistingPoiMdmIdsFromImportedId(String importedId) {
        OkinaIdentifier okinaId = new OkinaIdentifier();
        okinaId.setOriginalId(importedId);
        List<OkinaIdentifier> results = mdmFeignClient.getPoiIdentifiersByOriginalId(List.of(okinaId));
        return results != null && !results.isEmpty() ? results.get(0) : null;
    }

    public OkinaIdentifier getExistingParkingMdmIdsFromImportedId(String importedId) {
        OkinaIdentifier okinaId = new OkinaIdentifier();
        okinaId.setOriginalId(importedId);
        List<OkinaIdentifier> results = mdmFeignClient.getParkingsIdentifiersByOriginalId(List.of(okinaId));
        return results != null && !results.isEmpty() ? results.get(0) : null;
    }


    /**
     * Send identifiers to mdm to update imported ids
     * (used when imported)
     *
     * @param incomingStopPlace StopPlace with MOBIITI ids
     */
    public void updateImportedIds(StopPlace incomingStopPlace) {
        List<OkinaIdentifier> stopPlaceMdmData = getStopPlaceIdentifiers(incomingStopPlace);
        mdmFeignClient.updateStopImportedIds(stopPlaceMdmData);

        List<OkinaIdentifier> quayMdmData = getQuaysIdentifiers(incomingStopPlace);
        mdmFeignClient.updateQuaysImportedIds(quayMdmData);
    }


    /**
     * Send importedIds that need to be updated in MDM
     * (launched when modification is made in ABZU)
     *
     * @param incomingPoi incomingPoi with MOBIITI id
     */
    public void updateImportedIds(PointOfInterest incomingPoi) {
        List<OkinaIdentifier> poiImportedIds = new ArrayList<>(incomingPoi.getOriginalIds().size());

        for (String originalId : incomingPoi.getOriginalIds()) {
            poiImportedIds.add(buildMdmIdentifier(incomingPoi.getNetexId(), originalId));
        }

        mdmFeignClient.updatePoiImportedIds(poiImportedIds);
    }

    /**
     * Send importedIds that need to be updated in MDM
     * (launched when modification is made in ABZU)
     *
     * @param incomingParking incomingParking with MOBIITI id
     */
    public void updateImportedIds(Parking incomingParking) {
        List<OkinaIdentifier> parkingImportedIds = new ArrayList<>(incomingParking.getOriginalIds().size());

        for (String originalId : incomingParking.getOriginalIds()) {
            parkingImportedIds.add(buildMdmIdentifier(incomingParking.getNetexId(), originalId));
        }

        mdmFeignClient.updateParkingsImportedIds(parkingImportedIds);
    }


    /**
     * Send identifiers that need to be created in MDM WITHOUT generating new ones
     * (used when re-importing MOBIITI data)
     *
     * @param incomingPois incoming Pois with MOBIITI ids
     */
    public void createOrUpdateExistingIdentifiers(List<PointOfInterest> incomingPois) {
        List<OkinaIdentifier> idToSend = new ArrayList<>(incomingPois.size());

        for (PointOfInterest poi : incomingPois) {
            idToSend.add(buildMdmIdentifier(poi.getNetexId(), poi.getOriginalIds().iterator().next()));
        }

        mdmFeignClient.createOrUpdatePoiIdentifiers(idToSend);
    }

    /**
     * Force mdm to create data into table WITHOUT generating a new super id
     * (used when importing Netex stop from abzu that contains MOBIITI points)
     *
     * @param incomingStopPlace StopPlace with MOBIITI ids
     */
    public void createOrUpdateExistingIdentifiers(StopPlace incomingStopPlace) {
        List<OkinaIdentifier> stopPlaceMdmData = getStopPlaceIdentifiers(incomingStopPlace);
        mdmFeignClient.createOrUpdateStopIdentifiers(stopPlaceMdmData);

        List<OkinaIdentifier> quayMdmData = getQuaysIdentifiers(incomingStopPlace);
        mdmFeignClient.createOrUpdateQuayIdentifiers(quayMdmData);
    }

    /**
     * Request MDM to get originalId and fill it in object
     *
     * @param parking object that needs to be filled with originalId
     */
    public void fillOriginalId(Parking parking) {
        Long superId = getIdentifierFromNetexId(parking.getNetexId());
        List<OkinaIdentifier> mdmData = mdmFeignClient.getParkingIdentifiers(List.of(superId));
        parking.getOriginalIds().add(mdmData.get(0).getOriginalId());
    }

    public void fillPoiImportedIds(List<PointOfInterest> initializedPoi) {
        if (CollectionUtils.isNotEmpty(initializedPoi)) {
            Map<Long, PointOfInterest> databasePoi = new HashMap<>(initializedPoi.size());
            List<Long> identifiers = new ArrayList<>(initializedPoi.size());
            for (PointOfInterest poi : initializedPoi) {
                Long identifierFromNetexId = getIdentifierFromNetexId(poi.getNetexId());
                identifiers.add(identifierFromNetexId);
                databasePoi.put(identifierFromNetexId, poi);
            }

            List<OkinaIdentifier> poiCompletedIdentifiers = mdmFeignClient.getPoisIdentifiers(identifiers);

            PointOfInterest pointOfInterest;
            for (OkinaIdentifier okinaIdentifier : poiCompletedIdentifiers) {
                pointOfInterest = databasePoi.get(okinaIdentifier.getSuperId());
                if (pointOfInterest != null) {
                    pointOfInterest.getOriginalIds().add(okinaIdentifier.getOriginalId());
                }
            }
        }
    }


    public void fillParkingImportedIds(List<Parking> initializedParking) {
        if (CollectionUtils.isNotEmpty(initializedParking)) {
            Map<Long, Parking> databaseParking = new HashMap<>(initializedParking.size());
            List<Long> identifiers = new ArrayList<>(initializedParking.size());
            for (Parking parking : initializedParking) {
                Long identifierFromNetexId = getIdentifierFromNetexId(parking.getNetexId());
                identifiers.add(identifierFromNetexId);
                databaseParking.put(identifierFromNetexId, parking);
            }

            List<OkinaIdentifier> parkingCompletedIdentifiers = mdmFeignClient.getParkingIdentifiers(identifiers);

            Parking parking;
            for (OkinaIdentifier okinaIdentifier : parkingCompletedIdentifiers) {
                parking = databaseParking.get(okinaIdentifier.getSuperId());
                if (parking != null) {
                    parking.getOriginalIds().add(okinaIdentifier.getOriginalId());
                }
            }
        }
    }

    private void fillMdmId(Set<Quay> quays, List<OkinaIdentifier> mdmIdentifiers) {
        for (Quay quay : quays) {
            Optional<Long> mdmOpt = getMdmIdFromResponse(quay.getOriginalIds().iterator().next(), mdmIdentifiers);
            mdmOpt.ifPresent(superId -> quay.setNetexId(validNetexPrefix + QUAY_QUALIFIER + superId));
        }
    }

    /**
     * Read a response coming from MDM to recover MDM id.
     *
     * @param originalId     original id from the producer .e.g: PROV1:Quay:stop1
     * @param mdmIdentifiers list of identifiers from MDM. e.g: PROV1/quay1, PROV2/quayA, PROV3/quayX
     * @return MDM id
     */
    private Optional<Long> getMdmIdFromResponse(String originalId, List<OkinaIdentifier> mdmIdentifiers) {
        for (OkinaIdentifier mdmIdentifier : mdmIdentifiers) {
            if (mdmIdentifier.getOriginalId().equals(originalId.split(":")[2])) {
                return Optional.of(mdmIdentifier.getSuperId());
            }
        }
        return Optional.empty();
    }

    private void fillMdmId(StopPlace incomingStopPlace, List<OkinaIdentifier> mdmIdentifiers) {
        for (OkinaIdentifier mdmIdentifier : mdmIdentifiers) {
            if (incomingStopPlace.getOriginalIds().contains(mdmIdentifier.getDataset() + STOP_PLACE_QUALIFIER + mdmIdentifier.getOriginalId())) {
                incomingStopPlace.setNetexId(validNetexPrefix + STOP_PLACE_QUALIFIER + mdmIdentifier.getSuperId());
            }
        }
    }

    private List<OkinaIdentifier> getQuaysIdentifiers(StopPlace stopPlace) {
        List<OkinaIdentifier> quayMdmData = new ArrayList<>(stopPlace.getQuays().size());

        for (Quay quay : stopPlace.getQuays()) {
            for (String originalId : quay.getOriginalIds()) {
                quayMdmData.add(buildMdmIdentifierWithDataset(quay.getNetexId(), originalId));
            }
        }
        return quayMdmData;
    }

    private List<OkinaIdentifier> getStopPlaceIdentifiers(StopPlace stopPlace) {
        List<OkinaIdentifier> stopPlaceMdmData = new ArrayList<>(stopPlace.getOriginalIds().size());

        for (String originalId : stopPlace.getOriginalIds()) {
            stopPlaceMdmData.add(buildMdmIdentifierWithDataset(stopPlace.getNetexId(), originalId));
        }
        return stopPlaceMdmData;
    }

}
