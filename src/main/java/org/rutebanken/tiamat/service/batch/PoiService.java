package org.rutebanken.tiamat.service.batch;

import org.apache.commons.lang3.StringUtils;
import org.rutebanken.tiamat.externalapis.DtoGeocode;
import org.rutebanken.tiamat.importer.ImporterUtils;
import org.rutebanken.tiamat.model.PointOfInterest;
import org.rutebanken.tiamat.repository.PointOfInterestRepository;
import org.rutebanken.tiamat.rest.postcode.PostcodeResource;
import org.rutebanken.tiamat.versioning.save.PointOfInterestVersionedSaverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper.ORIGINAL_ID_KEY;

@Service
@EnableScheduling
public class PoiService {
    private static final Logger logger = LoggerFactory.getLogger(PostcodeResource.class);

    @Autowired
    private PointOfInterestRepository pointOfInterestRepository;

    @Autowired
    private PointOfInterestVersionedSaverService pointOfInterestVersionedSaverService;
    @Scheduled(cron = "${cron.poi.get.missing.post.code}")
    public void getMissingPostCodePoi() {
        logger.info("Démarrage de la récupération des codes postaux manquants pour les POI.");

        List<PointOfInterest> pointOfInterests = pointOfInterestRepository.getAllPOIWithoutPostcode();

        logger.info("Nombre de codes postaux à récupérer : " + pointOfInterests.size());
        int nbpostalcode = 0;
        for(PointOfInterest pointOfInterest : pointOfInterests){
            updatePostCodePOI(pointOfInterest);
            nbpostalcode++;
            logger.info("Nombre de codes postaux récupérés : " + nbpostalcode + "/" + pointOfInterests.size());
        }

        logger.info("Récupération des codes postaux manquants pour les POI terminée.");
    }

    public void updatePostCodePOI(PointOfInterest pointOfInterest) {
        Optional<String> poiId = pointOfInterest.getKeyValues().get(ORIGINAL_ID_KEY).getItems().stream().findFirst();

        String nameIdPoi = poiId.map(s -> pointOfInterest.getName().getValue() + "-" + s).orElseGet(() -> pointOfInterest.getName().getValue());

        logger.info("Récupération du code postal du POI : " + nameIdPoi);

        DtoGeocode geocodeData = ImporterUtils.getGeocodeDataByReverseGeocoding(pointOfInterest.getCentroid().getX(), pointOfInterest.getCentroid().getY());
        if(StringUtils.isEmpty(geocodeData.getPostCode())){
            logger.info("Code postal manquant pour le POI : " + nameIdPoi);
        }
        else{
            pointOfInterest.setPostalCode(geocodeData.getPostCode());
            pointOfInterestVersionedSaverService.saveNewVersion(pointOfInterest);
        }
    }
}
