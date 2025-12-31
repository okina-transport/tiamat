package org.rutebanken.tiamat.service.batch;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.model.PointOfInterest;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.repository.ParkingRepository;
import org.rutebanken.tiamat.repository.PointOfInterestRepository;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Service
public class MissingInseeCodeService {
    private static final Logger logger = LoggerFactory.getLogger(MissingInseeCodeService.class);

    private final PointOfInterestRepository pointOfInterestRepository;
    private final StopPlaceRepository stopPlaceRepository;
    private final ParkingRepository parkingRepository;
    private final UpdateInseeCodeService updateInseeCodeService;

    public MissingInseeCodeService(PointOfInterestRepository pointOfInterestRepository, StopPlaceRepository stopPlaceRepository, ParkingRepository parkingRepository, UpdateInseeCodeService updateInseeCodeService) {
        this.pointOfInterestRepository = pointOfInterestRepository;
        this.stopPlaceRepository = stopPlaceRepository;
        this.parkingRepository = parkingRepository;
        this.updateInseeCodeService = updateInseeCodeService;
    }

    public void getMissingInseeCode() {
        getMissingInseeCodeQuays();
        getMissingInseeCodePoi();
        getMissingInseeCodeParking();
    }

    private void getMissingInseeCodeQuays() {
        logger.info("Démarrage de la récupération des codes INSEE manquants des quais.");

        Set<Long> stopPlacesIds = stopPlaceRepository.getStopPlaceWithQuaysWithoutInseeCode();
        logger.info("Nombre total de StopPlaces à traiter : {}", stopPlacesIds.size());

        if (stopPlacesIds.isEmpty()) {
            logger.info("Aucun StopPlace à traiter.");
            return;
        }

        List<Long> stopPlacesIdsList = new ArrayList<>(stopPlacesIds);
        int batchSize = 1000;
        int totalBatches = (stopPlacesIdsList.size() + batchSize - 1) / batchSize;
        long totalMissingInseeCodeQuays = 0;
        int totalParentsProcessed = 0;
        int totalChildrenProcessed = 0;

        for (int i = 0; i < stopPlacesIdsList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, stopPlacesIdsList.size());
            List<Long> batchIds = stopPlacesIdsList.subList(i, end);
            int currentBatch = (i / batchSize) + 1;

            logger.info("Traitement du batch {}/{} ({} StopPlaces)", currentBatch, totalBatches, batchIds.size());

            List<StopPlace> batchStopPlaces = stopPlaceRepository.getStopPlaceInitializedForExport(new HashSet<>(batchIds));

            long batchMissingInseeCodeQuays = batchStopPlaces.stream()
                    .flatMap(stopPlace -> stopPlace.getQuays().stream())
                    .filter(quay -> StringUtils.isEmpty(quay.getInseeCode()))

                    .count();
            totalMissingInseeCodeQuays += batchMissingInseeCodeQuays;

            logger.info("Batch {}/{} - Codes INSEE de quais à récupérer : {}",
                    currentBatch, totalBatches, batchMissingInseeCodeQuays);

            List<String> parentStopPlacesRef = updateInseeCodeService.getParentStopPlacesRef(batchStopPlaces);
            List<StopPlace> childStopPlaces = updateInseeCodeService.removeStopPlacesWithParentRef(batchStopPlaces);

            if (!parentStopPlacesRef.isEmpty()) {
                logger.info("Batch {}/{} - Traitement de {} StopPlaces parents",
                        currentBatch, totalBatches, parentStopPlacesRef.size());
                updateInseeCodeService.updateParentStopPlaces(parentStopPlacesRef);
                totalParentsProcessed += parentStopPlacesRef.size();
            }

            if (!childStopPlaces.isEmpty()) {
                logger.info("Batch {}/{} - Traitement de {} StopPlaces enfants",
                        currentBatch, totalBatches, childStopPlaces.size());
                updateInseeCodeService.updateStopPlaces(childStopPlaces);
                totalChildrenProcessed += childStopPlaces.size();
            }

            logger.info("Batch {}/{} terminé", currentBatch, totalBatches);
        }

        logger.info("Récupération des codes INSEE manquants des quais terminée. " +
                        "Total quais manquants: {}, Parents traités: {}, Enfants traités: {}",
                totalMissingInseeCodeQuays, totalParentsProcessed, totalChildrenProcessed);
    }

    private void getMissingInseeCodePoi() {
        logger.info("Démarrage de la récupération des codes INSEE manquants des POI.");

        List<PointOfInterest> pointOfInterests = pointOfInterestRepository.getAllPOIWithoutInseeCode();
        logger.info("Nombre de codes INSEE de POI à récupérer : {}", pointOfInterests.size());

        if (!pointOfInterests.isEmpty()) {
            updateInseeCodeService.updatePOIInseeCode(pointOfInterests);
        }

        logger.info("Récupération des codes INSEE manquants des POI terminée.");
    }

    private void getMissingInseeCodeParking() {
        logger.info("Démarrage de la récupération des codes INSEE manquants des parkings.");

        List<Parking> parkings = parkingRepository.getAllParkingsWithoutInsee();
        logger.info("Nombre de codes INSEE de parkings à récupérer : {}", parkings.size());

        if (CollectionUtils.isNotEmpty(parkings)) {
            updateInseeCodeService.updateInseeCodeParkings(parkings);
        }

        logger.info("Récupération des codes INSEE manquants des parkings terminée.");
    }
}
