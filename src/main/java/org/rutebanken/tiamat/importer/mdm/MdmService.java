package org.rutebanken.tiamat.importer.mdm;

import org.apache.commons.collections4.CollectionUtils;
import org.rutebanken.tiamat.config.TiamatProperties;
import org.rutebanken.tiamat.feign.mdm.MdmFeignClient;
import org.rutebanken.tiamat.feign.mdm.OkinaIdentifier;
import org.rutebanken.tiamat.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MdmService {

    private static final Logger logger = LoggerFactory.getLogger(MdmService.class);

    private final MdmFeignClient mdmFeignClient;

    private final TiamatProperties tiamatProperties;

    @Value("${netex.validPrefix:MOBIITI}")
    String validNetexPrefix;


    public MdmService(MdmFeignClient mdmFeignClient, TiamatProperties tiamatProperties) {
        this.mdmFeignClient = mdmFeignClient;
        this.tiamatProperties = tiamatProperties;
    }

    public void fillImportedIds(List<StopPlace> stopPlaces){
        if (stopPlaces.isEmpty()){
            return;
        }

        Set<Long> stopPlaceMdmIds = new HashSet<>();
        Set<Long> quayMdmIds = new HashSet<>();



        for (org.rutebanken.tiamat.model.StopPlace recoveredStopPlace : stopPlaces) {
            stopPlaceMdmIds.add(Long.valueOf(recoveredStopPlace.getNetexId().split(":")[2]));

            for (Quay quay : recoveredStopPlace.getQuays()) {
                quayMdmIds.add(Long.valueOf(quay.getNetexId().split(":")[2]));
            }
        }

        List<OkinaIdentifier> stopPlacesMdmData = mdmFeignClient.getStopPlaceIdentifiers(stopPlaceMdmIds.stream().toList());
        stopPlaces.forEach(stopPlace -> feedImportedIds(stopPlace, stopPlacesMdmData));

        List<OkinaIdentifier> quaysMdmData = mdmFeignClient.getQuayIdentifiers(quayMdmIds.stream().toList());
        for (StopPlace stopPlace : stopPlaces) {
            for (Quay quay : stopPlace.getQuays()) {
                feedImportedIds(quay, quaysMdmData);
            }
        }
    }

    /**
     * Request MDM to recover existing MDM ids
     * @param incomingStopPlace
     * @return
     */
    public Optional<Long> getExistingStopPlaceMdmIds(StopPlace incomingStopPlace) {
        OkinaIdentifier stopIdentifier = buildInputStopIdentifier(incomingStopPlace);
        OkinaIdentifier stopPlaceMdmData = mdmFeignClient.getStopPlaceIdentifiersByOriginalId(stopIdentifier);
        return stopPlaceMdmData != null ? Optional.of(stopPlaceMdmData.getSuperId()) : Optional.empty();
    }



    private void feedImportedIds(DataManagedObjectStructure quayOrStopPlace, List<OkinaIdentifier> mdmData){
        String objectType = quayOrStopPlace instanceof StopPlace ? ":StopPlace:" : ":Quay:";
        Long mdmId = Long.valueOf(quayOrStopPlace.getNetexId().split(":")[2]);

        if (mdmId == null){
            return;
        }

        final Long finalMdmId = mdmId;
        List<OkinaIdentifier> mdmDataRelatedToObject = mdmData.stream()
                .filter(currentMdmData -> currentMdmData.getSuperId().equals(finalMdmId))
                .collect(Collectors.toList());

        if (!mdmDataRelatedToObject.isEmpty()){
            for (OkinaIdentifier okinaIdentifier : mdmDataRelatedToObject) {
                quayOrStopPlace.getOriginalIds().add(okinaIdentifier.getDataset() + objectType + okinaIdentifier.getOriginalId());
            }
        }

    }

    public void generateIdentifier(PointOfInterest incomingPointOfInterest) {
        if (!tiamatProperties.isMdmEnabled()){
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
        if (!tiamatProperties.isMdmEnabled()){
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
        if (!tiamatProperties.isMdmEnabled()){
            return;
        }

        if (!incomingStopPlace.getNetexId().contains(validNetexPrefix + ":StopPlace:")) {
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


                List<OkinaIdentifier> mdmQuayIdentifiers = mdmFeignClient.generateQuayIdentifiers(quayIdentifiers);
                fillMdmId(incomingStopPlace.getQuays(), mdmQuayIdentifiers);
                removeImportedIds(incomingStopPlace);
            } catch (Exception e) {
                logger.error("Failed to create stop identifiers in mdm: {}", e.getMessage());
            }
        }
    }

    public void removeImportedIds(StopPlace incomingStopPlace) {
        incomingStopPlace.getOriginalIds().clear();
        incomingStopPlace.getQuays().forEach(quay -> quay.getOriginalIds().clear());

    }

    private void fillMdmId(Set<Quay> quays, List<OkinaIdentifier> mdmIdentifiers) {
        for (Quay quay : quays) {
            Optional<Long> mdmOpt = getMdmIdFromResponse(quay.getOriginalIds().iterator().next(), mdmIdentifiers);
            mdmOpt.ifPresent(superId -> {
                quay.setNetexId(validNetexPrefix + ":Quay:" + superId);
            });
        }
    }


    /**
     * Read a response coming from MDM to recover MDM id.
     *
     * @param originalId
     *  original id from the producer .e.g: PROV1:Quay:stop1
     * @param mdmIdentifiers
     *  list of identifiers from MDM. e.g: PROV1/quay1, PROV2/quayA, PROV3/quayX
     * @return
     *  MDM id
     */
    private Optional<Long> getMdmIdFromResponse(String originalId, List<OkinaIdentifier> mdmIdentifiers){
        for (OkinaIdentifier mdmIdentifier : mdmIdentifiers) {
            if (mdmIdentifier.getOriginalId().equals(originalId.split(":")[2])) {
                return Optional.of(mdmIdentifier.getSuperId());
            }
        }
        return Optional.empty();
    }

    private void fillMdmId(StopPlace incomingStopPlace, List<OkinaIdentifier> mdmIdentifiers) {
        for (OkinaIdentifier mdmIdentifier : mdmIdentifiers) {
            if (incomingStopPlace.getOriginalIds().contains(mdmIdentifier.getDataset() + ":StopPlace:" + mdmIdentifier.getOriginalId())) {
                incomingStopPlace.setNetexId(validNetexPrefix + ":StopPlace:" + mdmIdentifier.getSuperId());
            }
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
            if (originalIdTab.length == 3){
                stopIdentifier.setOriginalId(originalIdTab[2]);
            }else{
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
            if (originalIdTab.length == 3){
                quayIdentifier.setOriginalId(originalIdTab[2]);
            }else{
                quayIdentifier.setOriginalId(originalId);
            }
        }

        if (quay.getNetexId() != null) {
            quayIdentifier.setSuperId(Long.valueOf(quay.getNetexId().split(":")[2]));
        }
        return quayIdentifier;
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

    public OkinaIdentifier getExistingPoiMdmIds(PointOfInterest poi){
        return getExistingPoiMdmIdsFromImportedId(poi.getOriginalIds().iterator().next());
    }

    public OkinaIdentifier getExistingPoiMdmIdsFromImportedId(String importedId){
        OkinaIdentifier okinaId = new OkinaIdentifier();
        okinaId.setOriginalId(importedId);
        List<OkinaIdentifier> results = mdmFeignClient.getPoiIdentifiersByOriginalId(List.of(okinaId));
        return results != null && !results.isEmpty() ? results.get(0) : null;
    }

    public OkinaIdentifier getExistingParkingMdmIdsFromImportedId(String importedId){
        OkinaIdentifier okinaId = new OkinaIdentifier();
        okinaId.setOriginalId(importedId);
        List<OkinaIdentifier> results = mdmFeignClient.getParkingsIdentifiersByOriginalId(List.of(okinaId));
        return results != null && !results.isEmpty() ? results.get(0) : null;
    }


    /**
     * Send identifiers to mdm to update imported ids
     * (used when imported)
     * @param incomingStopPlace
     *  StopPlace with MOBIITI ids
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
     * @param incomingPoi
     *  incomingPoi with MOBIITI id
     */
    public void updateImportedIds(PointOfInterest incomingPoi) {
        List<OkinaIdentifier> poiImportedIds = new ArrayList<>();
        for (String originalId : incomingPoi.getOriginalIds()) {
            OkinaIdentifier okinaIdentifier = new OkinaIdentifier();
            okinaIdentifier.setOriginalId(originalId);
            okinaIdentifier.setSuperId(Long.valueOf(incomingPoi.getNetexId().split(":")[2]));
            poiImportedIds.add(okinaIdentifier);
        }

        mdmFeignClient.updatePoiImportedIds(poiImportedIds);
    }

    /**
     * Send importedIds that need to be updated in MDM
     * (launched when modification is made in ABZU)
     * @param incomingParking
     *  incomingParking with MOBIITI id
     */
    public void updateImportedIds(Parking incomingParking) {
        List<OkinaIdentifier> parkingImportedIds = new ArrayList<>();
        for (String originalId : incomingParking.getOriginalIds()) {
            OkinaIdentifier okinaIdentifier = new OkinaIdentifier();
            okinaIdentifier.setOriginalId(originalId);
            okinaIdentifier.setSuperId(Long.valueOf(incomingParking.getNetexId().split(":")[2]));
            parkingImportedIds.add(okinaIdentifier);
        }

        mdmFeignClient.updateParkingsImportedIds(parkingImportedIds);
    }


    /**
     * Send identifiers that need to be created in MDM WITHOUT generating new ones
     * (used when re-importing MOBIITI data)
     * @param incomingPois
     *  incoming Pois with MOBIITI ids
     */
    public void createOrUpdateExistingIdentifiers(List<PointOfInterest> incomingPois) {
        List<OkinaIdentifier> idToSend = new ArrayList<>();


        for (PointOfInterest poi : incomingPois) {
                OkinaIdentifier id = new OkinaIdentifier();
                id.setOriginalId(poi.getOriginalIds().iterator().next());
                id.setSuperId(Long.valueOf(poi.getNetexId().split(":")[2]));
                idToSend.add(id);
        }


        mdmFeignClient.createOrUpdatePoiIdentifiers(idToSend);
    }



    /**
     * Force mdm to create data into table WITHOUT generating a new super id
     * (used when importing Netex stop from abzu that contains MOBIITI points)
     * @param incomingStopPlace
     *  StopPlace with MOBIITI ids
     */
    public void createOrUpdateExistingIdentifiers(StopPlace incomingStopPlace) {
        List<OkinaIdentifier> stopPlaceMdmData = getStopPlaceIdentifiers(incomingStopPlace);
        mdmFeignClient.createOrUpdateStopIdentifiers(stopPlaceMdmData);

        List<OkinaIdentifier> quayMdmData = getQuaysIdentifiers(incomingStopPlace);
        mdmFeignClient.createOrUpdateQuayIdentifiers(quayMdmData);
    }

    private List<OkinaIdentifier> getQuaysIdentifiers(StopPlace stopPlace) {
        List<OkinaIdentifier> quayMdmData = new ArrayList<>();
        for (Quay quay : stopPlace.getQuays()) {
            String quayMdmId = quay.getNetexId().split(":")[2];
            for (String originalId : quay.getOriginalIds()) {
                OkinaIdentifier mdmData = new OkinaIdentifier();
                mdmData.setDataset(originalId.split(":")[0]);
                mdmData.setOriginalId(originalId.split(":")[2]);
                mdmData.setSuperId(Long.valueOf(quayMdmId));
                quayMdmData.add(mdmData);
            }
        }
        return quayMdmData;
    }

    private List<OkinaIdentifier> getStopPlaceIdentifiers(StopPlace stopPlace) {
        List<OkinaIdentifier> stopPlaceMdmData = new ArrayList<>();
        String mdmId = stopPlace.getNetexId().split(":")[2];

        for (String originalId : stopPlace.getOriginalIds()) {
            OkinaIdentifier mdmData = new OkinaIdentifier();
            mdmData.setDataset(originalId.split(":")[0]);
            mdmData.setOriginalId(originalId.split(":")[2]);
            mdmData.setSuperId(Long.valueOf(mdmId));
            stopPlaceMdmData.add(mdmData);
        }
        return stopPlaceMdmData;
    }

    /**
     * Request MDM to get originalId and fill it in object
     * @param parking
     * object that needs to be filled with originalId
     *
     */
    public void fillOriginalId(Parking parking) {
        Long superId = Long.valueOf(parking.getNetexId().split(":")[2]);
        List<OkinaIdentifier> mdmData = mdmFeignClient.getParkingIdentifiers(Arrays.asList(superId));
        parking.getOriginalIds().add(mdmData.get(0).getOriginalId());
    }
}
