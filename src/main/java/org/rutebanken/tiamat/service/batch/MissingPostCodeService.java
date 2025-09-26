package org.rutebanken.tiamat.service.batch;

import org.apache.commons.lang3.StringUtils;
import org.rutebanken.tiamat.model.PointOfInterest;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.repository.PointOfInterestRepository;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Service
public class MissingPostCodeService {
    private static final Logger logger = LoggerFactory.getLogger(MissingPostCodeService.class);

    @Autowired private PointOfInterestRepository pointOfInterestRepository;
    @Autowired private StopPlaceRepository stopPlaceRepository;
    @Autowired private UpdatePostCodeService updatePostCodeService;


    public void getMissingPostCode() {
        getMissingPostCodeQuays();
        getMissingPostCodePoi();
    }

    private void getMissingPostCodeQuays() {
        logger.info("Démarrage de la récupération des codes postaux manquants des quais.");

        Set<Long> stopPlacesIds = stopPlaceRepository.getStopPlaceWithQuaysWithoutPostCode();
        logger.info("Nombre total de StopPlaces à traiter : {}", stopPlacesIds.size());

        if (stopPlacesIds.isEmpty()) {
            logger.info("Aucun StopPlace à traiter.");
            return;
        }

        List<Long> stopPlacesIdsList = new ArrayList<>(stopPlacesIds);
        int batchSize = 1000;
        int totalBatches = (stopPlacesIdsList.size() + batchSize - 1) / batchSize;
        long totalMissingPostCodeQuays = 0;
        int totalParentsProcessed = 0;
        int totalChildrenProcessed = 0;

        for (int i = 0; i < stopPlacesIdsList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, stopPlacesIdsList.size());
            List<Long> batchIds = stopPlacesIdsList.subList(i, end);
            int currentBatch = (i / batchSize) + 1;

            logger.info("Traitement du batch {}/{} ({} StopPlaces)", currentBatch, totalBatches, batchIds.size());

            List<StopPlace> batchStopPlaces = stopPlaceRepository.getStopPlaceInitializedForExport(new HashSet<>(batchIds));

            long batchMissingPostCodeQuays = batchStopPlaces.stream()
                    .flatMap(stopPlace -> stopPlace.getQuays().stream())
                    .filter(quay -> StringUtils.isEmpty(quay.getZipCode()))

                    .count();
            totalMissingPostCodeQuays += batchMissingPostCodeQuays;

            logger.info("Batch {}/{} - Codes postaux de quais à récupérer : {}",
                    currentBatch, totalBatches, batchMissingPostCodeQuays);

            List<String> parentStopPlacesRef = updatePostCodeService.getParentStopPlacesRef(batchStopPlaces);
            List<StopPlace> childStopPlaces = updatePostCodeService.removeStopPlacesWithParentRef(batchStopPlaces);

            if (!parentStopPlacesRef.isEmpty()) {
                logger.info("Batch {}/{} - Traitement de {} StopPlaces parents",
                        currentBatch, totalBatches, parentStopPlacesRef.size());
                updatePostCodeService.updateParentStopPlaces(parentStopPlacesRef);
                totalParentsProcessed += parentStopPlacesRef.size();
            }

            if (!childStopPlaces.isEmpty()) {
                logger.info("Batch {}/{} - Traitement de {} StopPlaces enfants",
                        currentBatch, totalBatches, childStopPlaces.size());
                updatePostCodeService.updateStopPlaces(childStopPlaces);
                totalChildrenProcessed += childStopPlaces.size();
            }

            logger.info("Batch {}/{} terminé", currentBatch, totalBatches);
        }

        logger.info("Récupération des codes postaux manquants des quais terminée. " +
                        "Total quais manquants: {}, Parents traités: {}, Enfants traités: {}",
                totalMissingPostCodeQuays, totalParentsProcessed, totalChildrenProcessed);
    }

    private void getMissingPostCodePoi() {
        logger.info("Démarrage de la récupération des codes postaux manquants des POI.");

        List<PointOfInterest> pointOfInterests = pointOfInterestRepository.getAllPOIWithoutPostcode();
        logger.info("Nombre de codes postaux de POI à récupérer : {}", pointOfInterests.size());

        if (!pointOfInterests.isEmpty()) {
            updatePostCodeService.updatePOIPostCodes(pointOfInterests);
        }

        logger.info("Récupération des codes postaux manquants des POI terminée.");
    }
}
