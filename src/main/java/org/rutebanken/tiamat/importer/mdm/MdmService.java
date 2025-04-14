package org.rutebanken.tiamat.importer.mdm;

import org.apache.commons.collections4.CollectionUtils;
import org.rutebanken.tiamat.config.TiamatProperties;
import org.rutebanken.tiamat.feign.mdm.MdmFeignClient;
import org.rutebanken.tiamat.feign.mdm.OkinaIdentifier;
import org.rutebanken.tiamat.model.DataManagedObjectStructure;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    public void replaceTiamatIdByMdmId(List<StopPlace> stopPlaces){
        List<Long> stopPlaceMdmIds = new ArrayList<>();
        List<Long> quayMdmIds = new ArrayList<>();
        for (org.rutebanken.tiamat.model.StopPlace recoveredStopPlace : stopPlaces) {
            stopPlaceMdmIds.add(recoveredStopPlace.getMdmId());

            for (Quay quay : recoveredStopPlace.getQuays()) {
                quayMdmIds.add(quay.getMdmId());
            }
        }

        List<OkinaIdentifier> stopPlacesMdmData = mdmFeignClient.getStopPlaceIdentifiers(stopPlaceMdmIds);
        stopPlaces.forEach(stopPlace -> replaceTiamatIdByMdmId(stopPlace, stopPlacesMdmData));

        List<OkinaIdentifier> quaysMdmData = mdmFeignClient.getQuayIdentifiers(quayMdmIds);
        for (StopPlace stopPlace : stopPlaces) {
            for (Quay quay : stopPlace.getQuays()) {
                replaceTiamatIdByMdmId(quay, quaysMdmData);
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

    private void replaceTiamatIdByMdmId(DataManagedObjectStructure quayOrStopPlace, List<OkinaIdentifier> quaysMdmData) {

        String objectType = quayOrStopPlace instanceof StopPlace ? ":StopPlace:" : ":Quay:";

        for (String originalId : quayOrStopPlace.getOriginalIds()) {
            for (OkinaIdentifier currentMdmData : quaysMdmData) {
                if (originalId.equals(currentMdmData.getDataset() +  objectType  + currentMdmData.getOriginalId())){
                    quayOrStopPlace.setNetexId(validNetexPrefix + objectType + currentMdmData.getSuperId().toString());
                    return;
                }
            }
        }
    }

    public void generateIdentifier(StopPlace incomingStopPlace) {
        if (tiamatProperties.isMdmEnabled()) {
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
            } catch (Exception e) {
                logger.error("Failed to create stop identifiers in mdm: {}", e.getMessage());
            }
        }
    }

    private void fillMdmId(Set<Quay> quays, List<OkinaIdentifier> mdmIdentifiers) {
        for (Quay quay : quays) {
            Optional<Long> mdmOpt = getMdmIdFromResponse(quay.getOriginalIds().iterator().next(), mdmIdentifiers);
            mdmOpt.ifPresent(quay::setMdmId);
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
                incomingStopPlace.setMdmId(mdmIdentifier.getSuperId());
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

    public List<OkinaIdentifier> getExistingQuaysMdmIds(String datasetId, Set<Quay> quays) {

        List<OkinaIdentifier> quayIdentifiers = new ArrayList<>();
        for (Quay quay : quays) {
            quayIdentifiers.add(buildInputQuayIdentifier(datasetId, quay));
        }

        return mdmFeignClient.getQuayIdentifiersByOriginalId(quayIdentifiers);
    }
}
