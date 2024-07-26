package org.rutebanken.tiamat.service.batch;

import org.apache.commons.lang3.StringUtils;
import org.rutebanken.tiamat.externalapis.DtoGeocode;
import org.rutebanken.tiamat.importer.ImporterUtils;
import org.rutebanken.tiamat.model.PointOfInterest;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.repository.PointOfInterestRepository;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.rutebanken.tiamat.versioning.VersionCreator;
import org.rutebanken.tiamat.versioning.save.PointOfInterestVersionedSaverService;
import org.rutebanken.tiamat.versioning.save.StopPlaceVersionedSaverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper.ORIGINAL_ID_KEY;

@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class MissingPostCodeService {
    private static final Logger logger = LoggerFactory.getLogger(MissingPostCodeService.class);

    @Autowired
    private PointOfInterestRepository pointOfInterestRepository;

    @Autowired
    private StopPlaceRepository stopPlaceRepository;

    @Autowired
    private PointOfInterestVersionedSaverService pointOfInterestVersionedSaverService;

    @Autowired
    private StopPlaceVersionedSaverService stopPlaceVersionedSaverService;

    @Autowired
    private VersionCreator versionCreator;

    private int nbPostCodeQuays = 0;
    private int nbPostCodePOI = 0;

    public void getMissingPostCode() {
        getMissingPostCodeQuays();
        getMissingPostCodePoi();
    }

    private void getMissingPostCodeQuays() {
        logger.info("Démarrage de la récupération des codes postaux manquants des quais.");

        List<StopPlace> stopPlaces = stopPlaceRepository.getStopPlaceWithQuaysWithoutPostCode();

        long nbMissingPostCodeQuays = stopPlaces.stream()
                .flatMap(stopPlace -> stopPlace.getQuays().stream())
                .filter(quay -> StringUtils.isEmpty(quay.getZipCode()))
                .count();

        logger.info("Nombre de codes postaux de quais à récupérer : {}", nbMissingPostCodeQuays);

        List<String> parentStopPlacesRef = getParentStopPlacesRef(stopPlaces);

        List<StopPlace> childStopPlaces = removeStopPlacesWithParentRef(stopPlaces);

        updateParentStopPlaces(parentStopPlacesRef, nbMissingPostCodeQuays);
        updateStopPlaces(childStopPlaces, nbMissingPostCodeQuays);

        logger.info("Récupération des codes postaux manquants des quais terminée.");
    }

    private List<String> getParentStopPlacesRef(List<StopPlace> stopPlaces) {
        return stopPlaces.stream()
                .filter(stopPlace -> stopPlace.getParentSiteRef() != null)
                .map(stopPlace -> stopPlace.getParentSiteRef().getRef())
                .distinct()
                .collect(Collectors.toList());
    }

    private List<StopPlace> removeStopPlacesWithParentRef(List<StopPlace> stopPlaces) {
        return stopPlaces.stream()
                .filter(stopPlace -> stopPlace.getParentSiteRef() == null)
                .collect(Collectors.toList());
    }

    private void updateParentStopPlaces(List<String> parentStopPlacesRef, long nbMissingPostCodeQuays) {
        for (String oldParentStopPlaceRef : parentStopPlacesRef) {
            org.rutebanken.tiamat.model.StopPlace existingParentStopPlace = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(oldParentStopPlaceRef);
            org.rutebanken.tiamat.model.StopPlace newParentStopPlace = versionCreator.createCopy(existingParentStopPlace, org.rutebanken.tiamat.model.StopPlace.class);
            boolean postCodeUpdated = false;

            for (StopPlace oldStopPlace : existingParentStopPlace.getChildren()) {
                org.rutebanken.tiamat.model.StopPlace newStopPlace = versionCreator.createCopy(oldStopPlace, org.rutebanken.tiamat.model.StopPlace.class);
                if (updatePostCodeQuay(newStopPlace)) {
                    postCodeUpdated = true;
                    updateChildStopPlace(newParentStopPlace, newStopPlace);
                }
            }
            if (postCodeUpdated) {
                stopPlaceVersionedSaverService.saveNewVersion(existingParentStopPlace, newParentStopPlace);
                logger.info("Nombre de codes postaux de quais récupérés : {}/{}", nbPostCodeQuays, nbMissingPostCodeQuays);
            }
        }
    }


    private void updateStopPlaces(List<StopPlace> stopPlaces, long nbMissingPostCodeQuays) {
        for (StopPlace oldStopPlace : stopPlaces) {
            org.rutebanken.tiamat.model.StopPlace newStopPlace = versionCreator.createCopy(oldStopPlace, org.rutebanken.tiamat.model.StopPlace.class);
            if (updatePostCodeQuay(newStopPlace)) {
                stopPlaceVersionedSaverService.saveNewVersion(oldStopPlace, newStopPlace);
                logger.info("Nombre de codes postaux de quais récupérés : {}/{}", nbPostCodeQuays, nbMissingPostCodeQuays);
            }
        }
    }

    private void updateChildStopPlace(org.rutebanken.tiamat.model.StopPlace newParentStopPlace, org.rutebanken.tiamat.model.StopPlace newStopPlace) {
        newParentStopPlace.getChildren().removeIf(stopPlace -> stopPlace.getNetexId().equals(newStopPlace.getNetexId()));
        newParentStopPlace.getChildren().add(newStopPlace);
    }

    private void getMissingPostCodePoi() {
        logger.info("Démarrage de la récupération des codes postaux manquants des POI.");

        List<PointOfInterest> pointOfInterests = pointOfInterestRepository.getAllPOIWithoutPostcode();
        logger.info("Nombre de codes postaux de POI à récupérer : {}", pointOfInterests.size());

        for (PointOfInterest pointOfInterest : pointOfInterests) {
            updatePostCodePOI(pointOfInterest);
            logger.info("Nombre de codes postaux de POI récupérés : {}/{}", nbPostCodePOI, pointOfInterests.size());
        }

        logger.info("Récupération des codes postaux manquants des POI terminée.");
    }

    private void updatePostCodePOI(PointOfInterest pointOfInterest) {
        Optional<String> poiId = pointOfInterest.getKeyValues().get(ORIGINAL_ID_KEY).getItems().stream().findFirst();
        String nameIdPoi = poiId.map(s -> pointOfInterest.getName().getValue() + "-" + s).orElse(pointOfInterest.getName().getValue());

        logger.info("Récupération du code postal du POI : {}", nameIdPoi);

        DtoGeocode geocodeData = ImporterUtils.getGeocodeDataByReverseGeocoding(pointOfInterest.getCentroid().getX(), pointOfInterest.getCentroid().getY());
        if (StringUtils.isEmpty(geocodeData.getPostCode())) {
            logger.info("Code postal manquant pour le POI : {}", nameIdPoi);
        } else {
            pointOfInterest.setPostalCode(geocodeData.getPostCode());
            pointOfInterestVersionedSaverService.saveNewVersion(pointOfInterest);
            nbPostCodePOI++;
        }
    }

    private boolean updatePostCodeQuay(org.rutebanken.tiamat.model.StopPlace newStopPlace) {
        logger.info("Récupération du code postal de l'arrêt : {}", newStopPlace.getName().getValue());

        boolean postCodeUpdated = false;
        for (Quay quay : newStopPlace.getQuays()) {
            if (StringUtils.isEmpty(quay.getZipCode())) {
                DtoGeocode geocodeData = ImporterUtils.getGeocodeDataByReverseGeocoding(quay.getCentroid().getX(), quay.getCentroid().getY());
                if (StringUtils.isEmpty(geocodeData.getPostCode())) {
                    logger.info("Code postal manquant pour l'arrêt : {}", newStopPlace.getName().getValue());
                } else {
                    quay.setZipCode(geocodeData.getPostCode());
                    postCodeUpdated = true;
                    nbPostCodeQuays++;
                }
            }
        }
        return postCodeUpdated;
    }
}
