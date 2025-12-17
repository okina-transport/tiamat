package org.rutebanken.tiamat.service.batch;

import org.apache.commons.lang3.StringUtils;
import org.rutebanken.tiamat.externalapis.DtoGeocode;
import org.rutebanken.tiamat.importer.ImporterUtils;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.model.PointOfInterest;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.repository.ParkingRepository;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.rutebanken.tiamat.versioning.VersionCreator;
import org.rutebanken.tiamat.versioning.save.PointOfInterestVersionedSaverService;
import org.rutebanken.tiamat.versioning.save.StopPlaceVersionedSaverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@Transactional
public class UpdatePostCodeService {
    private static final Logger logger = LoggerFactory.getLogger(UpdatePostCodeService.class);

    private final StopPlaceRepository stopPlaceRepository;
    private final StopPlaceVersionedSaverService stopPlaceVersionedSaverService;
    private final VersionCreator versionCreator;
    private final EntityManager entityManager;
    private final PointOfInterestVersionedSaverService pointOfInterestVersionedSaverService;
    private final ParkingRepository parkingRepository;

    public UpdatePostCodeService(StopPlaceRepository stopPlaceRepository, StopPlaceVersionedSaverService stopPlaceVersionedSaverService, VersionCreator versionCreator, EntityManager entityManager, PointOfInterestVersionedSaverService pointOfInterestVersionedSaverService, ParkingRepository parkingRepository) {
        this.stopPlaceRepository = stopPlaceRepository;
        this.stopPlaceVersionedSaverService = stopPlaceVersionedSaverService;
        this.versionCreator = versionCreator;
        this.entityManager = entityManager;
        this.pointOfInterestVersionedSaverService = pointOfInterestVersionedSaverService;
        this.parkingRepository = parkingRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateParentStopPlaces(List<String> parentStopPlacesRef) {
        int processed = 0;
        for (String oldParentStopPlaceRef : parentStopPlacesRef) {
            StopPlace existingParentStopPlace = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(oldParentStopPlaceRef);
            if (existingParentStopPlace != null) {
                StopPlace newParentStopPlace = versionCreator.createCopy(existingParentStopPlace, StopPlace.class);
                boolean postCodeUpdated = false;

                for (StopPlace oldStopPlace : existingParentStopPlace.getChildren()) {
                    StopPlace newStopPlace = versionCreator.createCopy(oldStopPlace, StopPlace.class);
                    if (updatePostCodeQuay(newStopPlace)) {
                        postCodeUpdated = true;
                        updateChildStopPlace(newParentStopPlace, newStopPlace);
                    }
                }
                if (postCodeUpdated) {
                    stopPlaceVersionedSaverService.saveNewVersion(existingParentStopPlace, newParentStopPlace);
                    processed++;

                    if (processed % 10 == 0) {
                        entityManager.flush();
                        entityManager.clear();
                    }
                }
            }
        }
        if (processed > 0) {
            entityManager.flush();
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStopPlaces(List<StopPlace> stopPlaces) {
        int processed = 0;
        for (StopPlace oldStopPlace : stopPlaces) {
            StopPlace newStopPlace = versionCreator.createCopy(oldStopPlace, StopPlace.class);
            if (updatePostCodeQuay(newStopPlace)) {
                stopPlaceVersionedSaverService.saveNewVersion(oldStopPlace, newStopPlace);
                processed++;

                if (processed % 50 == 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
            }
        }
        if (processed > 0) {
            entityManager.flush();
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updatePOIPostCodes(List<PointOfInterest> pointOfInterests) {
        int processed = 0;
        for (PointOfInterest pointOfInterest : pointOfInterests) {
            updatePostCodePOI(pointOfInterest);
            processed++;

            if (processed % 100 == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        if (processed > 0) {
            entityManager.flush();
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateParkings(List<Parking> parkings) {
        int processed = 0;
        for (Parking parking : parkings) {
            Optional<String> insee = ImporterUtils.getInseeFromLatLng(parking.getCentroid().getX(), parking.getCentroid().getY());
            if (insee.isPresent()) {
                parking.setInsee(insee.get());
                parkingRepository.save(parking);
                processed++;
            } else {
                logger.info("Code INSEE non trouvé pour le parking : {}", parking.getNetexId());
            }
        }
        if (processed > 0) {
            parkingRepository.flush();
        }
    }

    public void updatePostCodePOI(PointOfInterest pointOfInterest) {
        DtoGeocode geocodeData = ImporterUtils.getGeocodeDataByReverseGeocoding(pointOfInterest.getCentroid().getX(), pointOfInterest.getCentroid().getY());
        if (StringUtils.isNotEmpty(geocodeData.getPostCode())) {
            pointOfInterest.setPostalCode(geocodeData.getPostCode());
            pointOfInterestVersionedSaverService.saveNewVersionForPostalCodeProcess(pointOfInterest);
        }
        else{
            logger.info("Code postal non trouvé pour le point d'intérêt : {} - {}", pointOfInterest.getNetexId(), pointOfInterest.getName().getValue());
        }
    }



    public boolean updatePostCodeQuay(StopPlace newStopPlace) {
        boolean postCodeUpdated = false;
        for (Quay quay : newStopPlace.getQuays()) {
            if (StringUtils.isEmpty(quay.getZipCode())) {
                DtoGeocode geocodeData = ImporterUtils.getGeocodeDataByReverseGeocoding(quay.getCentroid().getX(), quay.getCentroid().getY());
                if (StringUtils.isNotEmpty(geocodeData.getPostCode())) {
                    quay.setZipCode(geocodeData.getPostCode());
                    postCodeUpdated = true;
                }
                else{
                    logger.info("Code postal non trouvé pour l'arrêt : {} - {}", quay.getNetexId(), newStopPlace.getName().getValue());
                }
            }
        }
        return postCodeUpdated;
    }

    public List<String> getParentStopPlacesRef(List<StopPlace> stopPlaces) {
        return stopPlaces.stream()
                .filter(stopPlace -> stopPlace.getParentSiteRef() != null)
                .map(stopPlace -> stopPlace.getParentSiteRef().getRef())
                .distinct()
                .collect(Collectors.toList());
    }

    public List<StopPlace> removeStopPlacesWithParentRef(List<StopPlace> stopPlaces) {
        return stopPlaces.stream()
                .filter(stopPlace -> stopPlace.getParentSiteRef() == null)
                .collect(Collectors.toList());
    }

    public void updateChildStopPlace(StopPlace newParentStopPlace, StopPlace newStopPlace) {
        newParentStopPlace.getChildren().removeIf(stopPlace -> stopPlace.getNetexId().equals(newStopPlace.getNetexId()));
        newParentStopPlace.getChildren().add(newStopPlace);
    }

}
