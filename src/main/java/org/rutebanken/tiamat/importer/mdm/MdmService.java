package org.rutebanken.tiamat.importer.mdm;

import org.apache.commons.collections4.CollectionUtils;
import org.rutebanken.tiamat.config.TiamatProperties;
import org.rutebanken.tiamat.feign.mdm.MdmFeignClient;
import org.rutebanken.tiamat.feign.mdm.OkinaIdentifier;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MdmService {

    private static final Logger logger = LoggerFactory.getLogger(MdmService.class);

    private final MdmFeignClient mdmFeignClient;

    private final TiamatProperties tiamatProperties;

    public MdmService(MdmFeignClient mdmFeignClient, TiamatProperties tiamatProperties) {
        this.mdmFeignClient = mdmFeignClient;
        this.tiamatProperties = tiamatProperties;
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
                mdmFeignClient.generateStopIdentifiers(stopIdentifiers);
                mdmFeignClient.generateQuayIdentifiers(quayIdentifiers);
            } catch (Exception e) {
                logger.error("Failed to create stop identifiers in mdm: {}", e.getMessage());
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
        stopIdentifier.setDataset(incomingStopPlace.getProvider());
        stopIdentifier.setOriginalId(new ArrayList<>(incomingStopPlace.getOriginalIds()).get(0));
        if (incomingStopPlace.getNetexId() != null) {
            stopIdentifier.setSuperId(incomingStopPlace.getNetexId());
        }
        return stopIdentifier;
    }

    private static OkinaIdentifier buildInputQuayIdentifier(String dataset, Quay quay) {
        OkinaIdentifier quayIdentifier = new OkinaIdentifier();
        quayIdentifier.setDataset(dataset);
        quayIdentifier.setOriginalId(new ArrayList<>(quay.getOriginalIds()).get(0));
        if (quay.getNetexId() != null) {
            quayIdentifier.setSuperId(quay.getNetexId());
        }
        return quayIdentifier;
    }
}
