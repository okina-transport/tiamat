package no.rutebanken.tiamat.importers;

import no.rutebanken.tiamat.model.*;
import no.rutebanken.tiamat.repository.QuayRepository;
import no.rutebanken.tiamat.repository.StopPlaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Qualifier("defaultStopPlaceImporter")
public class DefaultStopPlaceImporter implements StopPlaceImporter{

    private static final Logger logger = LoggerFactory.getLogger(DefaultStopPlaceImporter.class);

    public static final String ORIGINAL_ID_KEY = "imported-id";

    private TopographicPlaceCreator topographicPlaceCreator;

    private QuayRepository quayRepository;

    private StopPlaceRepository stopPlaceRepository;


    @Autowired
    public DefaultStopPlaceImporter(TopographicPlaceCreator topographicPlaceCreator, QuayRepository quayRepository, StopPlaceRepository stopPlaceRepository) {
        this.topographicPlaceCreator = topographicPlaceCreator;
        this.quayRepository = quayRepository;
        this.stopPlaceRepository = stopPlaceRepository;
    }

    public StopPlace findExistingStopPlaceFromOriginalId(StopPlace stopPlace) {

        StopPlace existingStopPlace = stopPlaceRepository.findByKeyValue(ORIGINAL_ID_KEY, stopPlace.getId());


        if(existingStopPlace != null) {
            logger.info("Found stop place {} from original ID key {}", existingStopPlace.getId(), stopPlace.getId());
            return existingStopPlace;
        }

        /*
        if(stopPlace.getId() != null) {

            StopPlace existingStopPlace = stopPlaceRepository.findOne(stopPlace.getId());
            if(existingStopPlace != null) {
                logger.info("Found existing stop place from ID: {}", stopPlace.getId());
                return existingStopPlace;
            }
        }

        if(stopPlace.getKeyList() != null) {
            return stopPlace.getKeyList().getKeyValue()
                    .stream()
                    .filter(keyValueStructure -> keyValueStructure.getKey().equals(ORIGINAL_ID_KEY))
                    .map(KeyValueStructure::getValue)
                    .map(value -> stopPlaceRepository.findByKeyValue(ORIGINAL_ID_KEY, value))
                    .filter(existingStopPlace ->  existingStopPlace != null)
                    .peek(existingStopPlace -> logger.info("Found stop place from original ID. Local ID is: {}", existingStopPlace.getId()))
                    .findFirst()
                    .orElseGet(null);
        }*/

        return null;

    }


    @Override
    public StopPlace importStopPlace(StopPlace stopPlace, SiteFrame siteFrame,
                                     AtomicInteger topographicPlacesCreatedCounter) throws InterruptedException, ExecutionException {
        if (stopPlace.getCentroid() == null
                || stopPlace.getCentroid().getLocation() == null
                || stopPlace.getCentroid().getLocation().getGeometryPoint() == null) {
            logger.info("Ignoring stop place {} - {} because it lacks geometry", stopPlace.getName(), stopPlace.getId());
            return null;
        }

        StopPlace existingStopPlace = findExistingStopPlaceFromOriginalId(stopPlace);
        if (existingStopPlace != null) {
            return existingStopPlace;
        }

        // TODO: Hack to avoid 'detached entity passed to persist'.
        stopPlace.getCentroid().getLocation().setId(0);

        topographicPlaceCreator.setTopographicReference(stopPlace,
                siteFrame.getTopographicPlaces().getTopographicPlace(),
                topographicPlacesCreatedCounter);
        resetIdAndKeepOriginalId(stopPlace);

        if (stopPlace.getQuays() != null) {
            logger.debug("Stop place has {} quays", stopPlace.getQuays().size());
            stopPlace.getQuays().forEach(quay -> {
                resetIdAndKeepOriginalId(quay);
                logger.debug("Saving quay ");


//                quay.getCentroid().setId(null);
//                quay.getCentroid().getLocation().setId(0);

                quayRepository.save(quay);
            });
        }

        stopPlaceRepository.save(stopPlace);
        logger.debug("Saving stop place {} {}", stopPlace.getName(), stopPlace.getId());
        return stopPlace;
    }

    public void resetIdAndKeepOriginalId(DataManagedObjectStructure dataManagedObjectStructure) {
        if(dataManagedObjectStructure.getId() != null) {
            KeyValueStructure importedId = new KeyValueStructure();
            importedId.setKey(ORIGINAL_ID_KEY);
            importedId.setValue(dataManagedObjectStructure.getId());
            if(dataManagedObjectStructure.getKeyList() == null) {
                dataManagedObjectStructure.setKeyList(new KeyListStructure());
            }
            dataManagedObjectStructure.getKeyList().getKeyValue().add(importedId);
            dataManagedObjectStructure.setId(null);
            logger.debug("Moved ID {} to key {}", importedId.getValue(), ORIGINAL_ID_KEY);
        }
    }
}
