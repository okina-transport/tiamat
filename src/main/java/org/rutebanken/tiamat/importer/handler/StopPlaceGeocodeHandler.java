package org.rutebanken.tiamat.importer.handler;

import org.apache.commons.lang3.StringUtils;
import org.rutebanken.tiamat.externalapis.DtoGeocode;
import org.rutebanken.tiamat.importer.ImporterUtils;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.repository.QuayRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StopPlaceGeocodeHandler {

    private static final Logger logger = LoggerFactory.getLogger(StopPlaceGeocodeHandler.class);


    private final QuayRepository quayRepository;

    public StopPlaceGeocodeHandler(QuayRepository quayRepository) {
        this.quayRepository = quayRepository;
    }

    @Async
    public void setPostalCode(List<String> netexIdentifier) {
        List<Quay> quays = quayRepository.findAllLatestVersionByNetexId(netexIdentifier);
        quays.forEach(quay -> {
            DtoGeocode dtoGeocode = null;
            try {
                if (StringUtils.isBlank(quay.getZipCode())) {
                    dtoGeocode = ImporterUtils.getGeocodeDataByReverseGeocoding(quay.getCentroid().getCoordinate().x, quay.getCentroid().getCoordinate().y);
                }
            } catch (Exception e) {
                logger.error("Erreur lors de la récupération du code postal du quay : {}", quay.getId(), e);
            }
            if (dtoGeocode != null && StringUtils.isNotBlank(dtoGeocode.getCityCode())) {
                quay.setZipCode(dtoGeocode.getCityCode());
            } else {
                logger.error("Code postal non trouvé pour le quay {} ", quay.getId());
            }
        });

        quayRepository.saveAll(quays);
    }
}
