package org.rutebanken.tiamat.importer.mdm;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.rutebanken.tiamat.client.mdm.IdentifierToCheck;
import org.rutebanken.tiamat.client.mdm.MdmClient;
import org.rutebanken.tiamat.client.mdm.MergeIdentifier;
import org.rutebanken.tiamat.client.mdm.OkinaIdentifier;
import org.rutebanken.tiamat.client.mdm.ParkingIdentifier;
import org.rutebanken.tiamat.config.TiamatProperties;
import org.rutebanken.tiamat.model.Organisation;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.model.PointOfInterest;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.netex.NetexUtils;
import org.rutebanken.tiamat.rest.exception.TiamatBusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class MdmService {

    private static final Logger logger = LoggerFactory.getLogger(MdmService.class);
    private static final String STOP_PLACE_QUALIFIER = ":StopPlace:";
    private static final String QUAY_QUALIFIER = ":Quay:";

    private final MdmClient mdmClient;

    private final TiamatProperties tiamatProperties;

    @Value("${netex.validPrefix:MOBIITI}")
    private String validNetexPrefix;

    public MdmService(MdmClient mdmClient, TiamatProperties tiamatProperties) {
        this.mdmClient = mdmClient;
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

    private static ParkingIdentifier buildParkingIdentifier(Parking parking) {
        ParkingIdentifier mdmData = new ParkingIdentifier();
        mdmData.setOperator(parking.getOperator());
        mdmData.setOriginalId(CollectionUtils.isNotEmpty(parking.getOriginalIds()) ? parking.getOriginalIds().iterator().next() : "");
        mdmData.setCountryCode("FR");
        mdmData.setInsee(parking.getInsee());
        if (StringUtils.isNotBlank(parking.getNetexId())) {
            mdmData.setSuperId(parking.getNetexId());
        }
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

    private static OkinaIdentifier buildInputStopIdentifier(StopPlace incomingStopPlace) throws TiamatBusinessException {
        OkinaIdentifier stopIdentifier = new OkinaIdentifier();


        stopIdentifier.setDataset(StringUtils.upperCase(getProvider(incomingStopPlace)));

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

    private static String getProvider(StopPlace incomingStopPlace) throws TiamatBusinessException {
        if (StringUtils.isNotEmpty(incomingStopPlace.getProvider())) {
            return incomingStopPlace.getProvider().toUpperCase();
        }

        if (MapUtils.isNotEmpty(incomingStopPlace.getKeyValues())) {
            List<String> spImportedIds = incomingStopPlace.getKeyValues().entrySet()
                    .stream().filter(entry -> entry.getKey().equals("imported-id"))
                    .map(Map.Entry::getValue)
                    .flatMap(value -> value.getItems().stream())
                    .toList();

            if (CollectionUtils.isNotEmpty(spImportedIds)) {
                String[] importedIdParts = spImportedIds.getFirst().split(":");
                if (importedIdParts.length > 0) {
                    incomingStopPlace.setProvider(StringUtils.lowerCase(importedIdParts[0]));
                    return importedIdParts[0];
                }
            }
        }
        throw new TiamatBusinessException(400, "Missing provider");
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

    public void deleteStopPlaceAndQuayIdsByDataset(String datasetId) {
        mdmClient.deleteStopPlacesByDataset(datasetId);
        mdmClient.deleteQuaysByDataset(datasetId);
    }

    public void deleteStopPlaceBySuperId(String superId) {
        mdmClient.deleteStopPlaceBySuperId(superId);
    }

    public void deleteQuaysBySuperId(String superId) {
        mdmClient.deleteQuaysBySuperId(superId);
    }

    public void deletePoisBySuperId(String superId) {
        mdmClient.deletePoisBySuperId(superId);
    }

    public void deleteParkingsBySuperId(String superId) {
        mdmClient.deleteParkingsBySuperId(superId);
    }


    public void deleteAllPoiIds() {
        mdmClient.deleteAllPoisIds();
    }

    public void deleteAllParkingIds() {
        mdmClient.deleteAllParkingIds();
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

        List<OkinaIdentifier> stopPlacesMdmData = mdmClient.getStopPlaceIdentifiers(identifierToCheck.stopPlaceMdmIds().stream().toList());
        StopPlace stopPlaceToComplete;
        for (OkinaIdentifier okinaIdentifier : stopPlacesMdmData) {
            stopPlaceToComplete = identifierToCheck.stopPlaceMap().get(okinaIdentifier.getSuperId());
            if (stopPlaceToComplete != null) {
                String completeOriginalId = okinaIdentifier.getDataset() + STOP_PLACE_QUALIFIER + okinaIdentifier.getOriginalId();
                stopPlaceToComplete.getOriginalIds().add(completeOriginalId);
            }
        }
        if (CollectionUtils.isNotEmpty(identifierToCheck.quayMdmIds())) {
            List<OkinaIdentifier> quaysMdmData = mdmClient.getQuayIdentifiers(identifierToCheck.quayMdmIds().stream().toList());
            Quay quayToComplete;
            for (OkinaIdentifier okinaIdentifier : quaysMdmData) {
                quayToComplete = identifierToCheck.quayMap().get(okinaIdentifier.getSuperId());
                if (quayToComplete != null) {
                    quayToComplete.getOriginalIds().add(okinaIdentifier.getDataset() + QUAY_QUALIFIER + okinaIdentifier.getOriginalId());
                }
            }
        }
    }


    public void fillImportedIdsInNetexStopPlace(org.rutebanken.netex.model.StopPlace netexStopPlace, String datasetId) {
        if (netexStopPlace == null || !tiamatProperties.isMdmEnabled() || isImportedIdAlreadyDefinedForDataset(netexStopPlace, datasetId)) {
            return;
        }

        Long stopIdentifier = getIdentifierFromNetexId(netexStopPlace.getId());

        List<OkinaIdentifier> stopPlacesMdmData = mdmClient.getStopPlaceIdentifiers(List.of(stopIdentifier));

        for (OkinaIdentifier okinaIdentifier : stopPlacesMdmData) {
            if (datasetId.equals(okinaIdentifier.getDataset())) {
                String completeOriginalId = okinaIdentifier.getDataset() + STOP_PLACE_QUALIFIER + okinaIdentifier.getOriginalId();
                NetexUtils.addImportedId(netexStopPlace, completeOriginalId);
            }
        }

        List<Long> searchList = new ArrayList<>();
        for (org.rutebanken.netex.model.Quay quay : NetexUtils.getQuaysFromStopPlace(netexStopPlace)) {
            Long quayIdentifier = getIdentifierFromNetexId(quay.getId());
            searchList.add(quayIdentifier);
        }

        List<OkinaIdentifier> quaysMdmData = mdmClient.getQuayIdentifiers(searchList);

        for (org.rutebanken.netex.model.Quay quay : NetexUtils.getQuaysFromStopPlace(netexStopPlace)) {
            Long quayNetexId = getIdentifierFromNetexId(quay.getId());

            for (OkinaIdentifier quayMdm : quaysMdmData) {
                if (quayMdm.getSuperId().equals(quayNetexId) && datasetId.equals(quayMdm.getDataset())) {
                    String importedId = quayMdm.getDataset() + QUAY_QUALIFIER + quayMdm.getOriginalId();
                    NetexUtils.addImportedId(quay, importedId);
                }
            }
        }
    }

    private boolean isImportedIdAlreadyDefinedForDataset(org.rutebanken.netex.model.StopPlace netexStopPlace, String datasetId) {

        if (netexStopPlace.getKeyList() == null || CollectionUtils.isEmpty(netexStopPlace.getKeyList().getKeyValue())) {
            return false;
        }

        return netexStopPlace.getKeyList().getKeyValue().stream()
                .anyMatch(keyValueStructure -> keyValueStructure.getKey().equals("imported-id") && keyValueStructure.getValue().toLowerCase().startsWith(datasetId.toLowerCase()));

    }

    /**
     * Request MDM to recover existing MDM ids
     *
     * @param incomingStopPlace incoming stop place
     * @return stop place superId if stop place id is found, null otherwise (wrapped in Optional)
     */
    public Optional<Long> getExistingStopPlaceMdmIds(StopPlace incomingStopPlace) throws TiamatBusinessException {
        OkinaIdentifier stopIdentifier = buildInputStopIdentifier(incomingStopPlace);
        OkinaIdentifier stopPlaceMdmData = mdmClient.getStopPlaceIdentifiersByOriginalId(stopIdentifier);
        return stopPlaceMdmData != null ? Optional.of(stopPlaceMdmData.getSuperId()) : Optional.empty();
    }

    public Set<Long> getStopPlaceIdsByProvider(String provider) {
        return mdmClient.getStopPlaceIdentifiersByDataset(provider);
    }

    public void generateIdentifier(PointOfInterest incomingPointOfInterest) {
        if (!tiamatProperties.isMdmEnabled()) {
            return;
        }

        OkinaIdentifier okinaIdentifier = new OkinaIdentifier();
        okinaIdentifier.setOriginalId(incomingPointOfInterest.getOriginalIds().iterator().next());
        List<OkinaIdentifier> mdmData = mdmClient.generatePoiIdentifiers(List.of(okinaIdentifier));
        Long superId = mdmData.getFirst().getSuperId();
        incomingPointOfInterest.setNetexId(validNetexPrefix + ":PointOfInterest:" + superId);
        incomingPointOfInterest.getOriginalIds().clear();
    }

    public void generateIdentifier(Parking parking) {
        if (!tiamatProperties.isMdmEnabled()) {
            return;
        }

        ParkingIdentifier parkingIdentifier = buildParkingIdentifier(parking);
        List<ParkingIdentifier> mdmData = mdmClient.generateParkingIdentifiers(List.of(parkingIdentifier));
        String superId = mdmData.getFirst().getSuperId();
        parking.setNetexId(superId);
    }

    public void generateIdentifier(Organisation organisation) {
        if (!tiamatProperties.isMdmEnabled()) {
            return;
        }

        OkinaIdentifier okinaIdentifier = new OkinaIdentifier();
        okinaIdentifier.setDataset("unused");
        okinaIdentifier.setOriginalId(organisation.getOriginalId());
        OkinaIdentifier mdmData = mdmClient.generateOrganisationIdentifier(okinaIdentifier);
        Long superId = mdmData.getSuperId();
        organisation.setNetexId(validNetexPrefix + ":Organisation:" + superId);
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
                List<OkinaIdentifier> mdmStopIdentifiers = mdmClient.generateStopIdentifiers(stopIdentifiers);
                fillMdmId(incomingStopPlace, mdmStopIdentifiers);
                logger.debug("Super id generated by MDM : {}", incomingStopPlace.getNetexId());

                List<OkinaIdentifier> mdmQuayIdentifiers = mdmClient.generateQuayIdentifiers(quayIdentifiers);
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
            mdmClient.mergeStopIdentifiers(mergeIdentifier);
        }
    }

    public void removeImportedIds(StopPlace incomingStopPlace) {
        incomingStopPlace.getOriginalIds().clear();
        incomingStopPlace.getQuays().forEach(quay -> quay.getOriginalIds().clear());
    }

    public List<OkinaIdentifier> getAllQuaysFromSuperId(Set<Long> superIds) {
        return mdmClient.getQuayIdentifiers(superIds.stream().toList());
    }

    public List<OkinaIdentifier> getAllStopPlacesFromSuperId(Set<Long> superIds) {
        return mdmClient.getStopPlaceIdentifiers(superIds.stream().toList());
    }

    public List<OkinaIdentifier> getAllPoisFromSuperId(Set<Long> superIds) {
        return mdmClient.getPoisIdentifiers(superIds.stream().toList());
    }

    public List<ParkingIdentifier> getAllParkingsFromSuperId(Set<String> superIds) {
        return mdmClient.getParkingIdentifiers(superIds.stream().toList());
    }

    public List<OkinaIdentifier> getExistingQuaysMdmIds(String datasetId, Set<Quay> quays) {

        List<OkinaIdentifier> quayIdentifiers = new ArrayList<>();
        for (Quay quay : quays) {
            quayIdentifiers.add(buildInputQuayIdentifier(datasetId, quay));
        }

        return mdmClient.getQuayIdentifiersByOriginalId(quayIdentifiers);
    }

    public OkinaIdentifier getExistingPoiMdmIds(PointOfInterest poi) {
        return getExistingPoiMdmIdsFromImportedId(poi.getOriginalIds().iterator().next());
    }

    public OkinaIdentifier getExistingPoiMdmIdsFromImportedId(String importedId) {
        OkinaIdentifier okinaId = new OkinaIdentifier();
        okinaId.setOriginalId(importedId);
        List<OkinaIdentifier> results = mdmClient.getPoiIdentifiersByOriginalId(List.of(okinaId));
        return results != null && !results.isEmpty() ? results.getFirst() : null;
    }

    public Optional<ParkingIdentifier> getExistingParkingMdmIdsFromImportedId(String operator, String importedId) {
        try {
            return Optional.of(mdmClient.getParkingIdentifierbyOperatorAndOriginalId(operator,
                    importedId));
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw e;
        }
    }

    public Optional<OkinaIdentifier> getExistingOrganisationMdmIdsFromImportedId(String importedId) {
        try {
            OkinaIdentifier result = mdmClient.getOrganisationsIdentifierByOriginalId(importedId);
            return Optional.of(result);
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw e;
        }
    }

    /**
     * Send identifiers to mdm to update imported ids
     * (used when imported)
     *
     * @param incomingStopPlace StopPlace with MOBIITI ids
     */
    public void updateImportedIds(StopPlace incomingStopPlace) {
        List<OkinaIdentifier> stopPlaceMdmData = getStopPlaceIdentifiers(incomingStopPlace);
        mdmClient.updateStopImportedIds(stopPlaceMdmData);

        List<OkinaIdentifier> quayMdmData = getQuaysIdentifiers(incomingStopPlace);
        if (CollectionUtils.isNotEmpty(quayMdmData)) {
            mdmClient.updateQuaysImportedIds(quayMdmData);
            List<OkinaIdentifier> upToDateQuayIdentifiers = mdmClient.getQuayIdentifiersByOriginalId(quayMdmData);
            fillMdmId(incomingStopPlace.getQuays(), upToDateQuayIdentifiers);
        }
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

        mdmClient.updatePoiImportedIds(poiImportedIds);
    }

    /**
     * Send importedIds that need to be updated in MDM
     * (launched when modification is made in ABZU)
     *
     * @param incomingParking incomingParking with MOBIITI id
     */
    public void updateImportedIds(Parking incomingParking) {
        List<ParkingIdentifier> parkingImportedIds = new ArrayList<>(incomingParking.getOriginalIds().size());

        parkingImportedIds.add(buildParkingIdentifier(incomingParking));

        mdmClient.updateParkingsImportedIds(parkingImportedIds);
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

        mdmClient.createOrUpdatePoiIdentifiers(idToSend);
    }

    /**
     * Force mdm to create data into table WITHOUT generating a new super id
     * (used when importing Netex stop from abzu that contains MOBIITI points)
     *
     * @param incomingStopPlace StopPlace with MOBIITI ids
     */
    public void createOrUpdateExistingIdentifiers(StopPlace incomingStopPlace) {
        List<OkinaIdentifier> stopPlaceMdmData = getStopPlaceIdentifiers(incomingStopPlace);
        mdmClient.createOrUpdateStopIdentifiers(stopPlaceMdmData);

        List<OkinaIdentifier> quayMdmData = getQuaysIdentifiers(incomingStopPlace);
        mdmClient.createOrUpdateQuayIdentifiers(quayMdmData);
    }

    /**
     * Request MDM to get originalId and fill it in object
     *
     * @param parking object that needs to be filled with originalId
     */
    public void fillOriginalId(Parking parking) {
        List<ParkingIdentifier> mdmData = mdmClient.getParkingIdentifiers(List.of(parking.getNetexId()));
        if (!mdmData.isEmpty()) {
            parking.getOriginalIds().add(mdmData.getFirst().getOriginalId());
        }
    }

    public void fillPoiImportedIds(List<PointOfInterest> initializedPoi) {
        if (CollectionUtils.isNotEmpty(initializedPoi)) {
            Map<Long, PointOfInterest> databasePoi = HashMap.newHashMap(initializedPoi.size());
            List<Long> identifiers = new ArrayList<>(initializedPoi.size());
            for (PointOfInterest poi : initializedPoi) {
                Long identifierFromNetexId = getIdentifierFromNetexId(poi.getNetexId());
                identifiers.add(identifierFromNetexId);
                databasePoi.put(identifierFromNetexId, poi);
            }

            List<OkinaIdentifier> poiCompletedIdentifiers = mdmClient.getPoisIdentifiers(identifiers);

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
            Map<String, Parking> databaseParking = HashMap.newHashMap(initializedParking.size());
            List<String> identifiers = new ArrayList<>(initializedParking.size());
            for (Parking parking : initializedParking) {
                identifiers.add(parking.getNetexId());
                databaseParking.put(parking.getNetexId(), parking);
            }

            List<ParkingIdentifier> parkingCompletedIdentifiers = mdmClient.getParkingIdentifiers(identifiers);

            Parking parking;
            for (ParkingIdentifier parkingIdentifier : parkingCompletedIdentifiers) {
                parking = databaseParking.get(parkingIdentifier.getSuperId());
                if (parking != null) {
                    parking.getOriginalIds().add(parkingIdentifier.getOriginalId());
                }
            }
        }
    }

    private void fillMdmId(Set<Quay> quays, List<OkinaIdentifier> mdmIdentifiers) {
        for (Quay quay : quays) {
            if (CollectionUtils.isEmpty(quay.getOriginalIds())){
                continue;
            }

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
