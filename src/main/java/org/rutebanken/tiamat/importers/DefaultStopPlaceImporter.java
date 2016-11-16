package org.rutebanken.tiamat.importers;

import com.google.common.util.concurrent.Striped;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.rutebanken.tiamat.model.*;
import org.rutebanken.tiamat.netexmapping.NetexIdMapper;
import org.rutebanken.tiamat.pelias.CountyAndMunicipalityLookupService;
import org.rutebanken.tiamat.repository.QuayRepository;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Qualifier("defaultStopPlaceImporter")
public class DefaultStopPlaceImporter implements StopPlaceImporter {

    private static final Logger logger = LoggerFactory.getLogger(DefaultStopPlaceImporter.class);

    /**
     * The max distance for checking if two quays are nearby each other.
     * http://gis.stackexchange.com/questions/28799/what-is-the-unit-of-measurement-for-buffer-calculation
     * https://en.wikipedia.org/wiki/Decimal_degrees
     */
    public static final double DISTANCE = 0.0001;

    private TopographicPlaceCreator topographicPlaceCreator;

    private CountyAndMunicipalityLookupService countyAndMunicipalityLookupService;

    private QuayRepository quayRepository;

    private StopPlaceRepository stopPlaceRepository;

    private StopPlaceFromOriginalIdFinder stopPlaceFromOriginalIdFinder;

    private NearbyStopPlaceFinder nearbyStopPlaceFinder;

//    private KeyStringValueAppender keyStringValueAppender;

    private KeyValueListAppender keyValueListAppender;

    private static DecimalFormat format = new DecimalFormat("#.#");

    private static Striped<Semaphore> stripedSemaphores = Striped.lazyWeakSemaphore(Integer.MAX_VALUE, 1);


    @Autowired
    public DefaultStopPlaceImporter(TopographicPlaceCreator topographicPlaceCreator,
                                    CountyAndMunicipalityLookupService countyAndMunicipalityLookupService,
                                    QuayRepository quayRepository, StopPlaceRepository stopPlaceRepository,
                                    StopPlaceFromOriginalIdFinder stopPlaceFromOriginalIdFinder,
                                    NearbyStopPlaceFinder nearbyStopPlaceFinder, KeyValueListAppender keyValueListAppender) {
        this.topographicPlaceCreator = topographicPlaceCreator;
        this.countyAndMunicipalityLookupService = countyAndMunicipalityLookupService;
        this.quayRepository = quayRepository;
        this.stopPlaceRepository = stopPlaceRepository;
        this.stopPlaceFromOriginalIdFinder = stopPlaceFromOriginalIdFinder;
        this.nearbyStopPlaceFinder = nearbyStopPlaceFinder;
        this.keyValueListAppender = keyValueListAppender;
    }

    @Override
    public StopPlace importStopPlace(StopPlace newStopPlace, SiteFrame siteFrame,
                                     AtomicInteger topographicPlacesCreatedCounter) throws InterruptedException, ExecutionException {
        String semaphoreKey = getStripedSemaphoreKey(newStopPlace);
        Semaphore semaphore = stripedSemaphores.get(semaphoreKey);
        semaphore.acquire();
        logger.info("Aquired semaphore '{}' for stop place {}", semaphoreKey, newStopPlace);

        try {
            logger.info("Import stop place {}", newStopPlace);

            final StopPlace foundStopPlace = findNearbyOrExistingStopPlace(newStopPlace);

            final StopPlace stopPlace;
            if (foundStopPlace != null) {
                stopPlace = handleAlreadyExistingStopPlace(foundStopPlace, newStopPlace);
            } else {
                stopPlace = handleCompletelyNewStopPlace(newStopPlace, siteFrame, topographicPlacesCreatedCounter);
            }
            return stopPlace;
        } finally {
            semaphore.release();
            logger.info("Released semaphore '{}'", semaphoreKey);
        }
    }


    public StopPlace handleCompletelyNewStopPlace(StopPlace newStopPlace, SiteFrame siteFrame, AtomicInteger topographicPlacesCreatedCounter) throws ExecutionException {

        if (hasTopographicPlaces(siteFrame)) {
            topographicPlaceCreator.setTopographicReference(newStopPlace,
                    siteFrame.getTopographicPlaces().getTopographicPlace(),
                    topographicPlacesCreatedCounter);
        } else {
            lookupCountyAndMunicipality(newStopPlace, topographicPlacesCreatedCounter);
        }

        if (newStopPlace.getQuays() != null) {
            logger.info("Importing quays for new stop place {}", newStopPlace);
            newStopPlace.getQuays().forEach(quay -> {
                if (!quay.hasCoordinates()) {
                    logger.warn("Quay does not have coordinates.", quay.getId());
                }
                logger.info("Saving quay {}", quay);
                quayRepository.save(quay);
                logger.debug("Saved quay. Got id {} back", quay.getId());
            });
        }

        return saveAndUpdateCache(newStopPlace);
    }

    private StopPlace saveAndUpdateCache(StopPlace stopPlace) {
        stopPlaceRepository.save(stopPlace);
        stopPlaceFromOriginalIdFinder.update(stopPlace);
        nearbyStopPlaceFinder.update(stopPlace);
        logger.info("Saved stop place {}", stopPlace);
        return stopPlace;
    }

    private boolean hasTopographicPlaces(SiteFrame siteFrame) {
        return siteFrame.getTopographicPlaces() != null
                && siteFrame.getTopographicPlaces().getTopographicPlace() != null
                && !siteFrame.getTopographicPlaces().getTopographicPlace().isEmpty();
    }

    public StopPlace handleAlreadyExistingStopPlace(StopPlace foundStopPlace, StopPlace newStopPlace) {
        logger.info("Found existing stop place {} from incoming {}", foundStopPlace, newStopPlace);

        boolean quaysChanged = addAndSaveNewQuays(newStopPlace, foundStopPlace);
        boolean originalIdChanged = keyValueListAppender.appendToOriginalId(NetexIdMapper.ORIGINAL_ID_KEY, newStopPlace, foundStopPlace);

        if (originalIdChanged || quaysChanged) {
            logger.info("Updated existing stop place {}. ", foundStopPlace);
            foundStopPlace.getQuays().forEach(q -> logger.info("Stop place {}:  Quay {}: {}", foundStopPlace.getId(), q.getId(), q.getName()));
            saveAndUpdateCache(foundStopPlace);
        }
        return foundStopPlace;
    }

    private void lookupCountyAndMunicipality(StopPlace stopPlace, AtomicInteger topographicPlacesCreatedCounter) {
        try {
            countyAndMunicipalityLookupService.populateCountyAndMunicipality(stopPlace, topographicPlacesCreatedCounter);
        } catch (IOException | InterruptedException e) {
            logger.warn("Could not lookup county and municipality for stop place with id {}", stopPlace.getId());
        }
    }

    /**
     * Inspect quays from incoming AND matching stop place. If they do not exist from before, add them.
     */
    public boolean addAndSaveNewQuays(StopPlace newStopPlace, StopPlace foundStopPlace) {

        AtomicInteger updatedQuays = new AtomicInteger();
        AtomicInteger createdQuays = new AtomicInteger();


        logger.debug("About to compare quays for {}", foundStopPlace.getId());

        if (foundStopPlace.getQuays() == null) {
            foundStopPlace.setQuays(new HashSet<>());
        }

        if (newStopPlace.getQuays() == null) {
            foundStopPlace.setQuays(new HashSet<>());
        }

        if (foundStopPlace.getQuays().isEmpty() && !newStopPlace.getQuays().isEmpty()) {
            logger.debug("Existing stop place {} does not have any quays, using all quays from incoming stop {}, {}", foundStopPlace, newStopPlace, newStopPlace.getName());
            for (Quay newQuay : newStopPlace.getQuays()) {
                saveNewQuay(newQuay, foundStopPlace, createdQuays);
            }
        } else if (!newStopPlace.getQuays().isEmpty() && !newStopPlace.getQuays().isEmpty()) {
            logger.debug("Comparing existing: {}, incoming: {}. Removing/ignoring quays that has matching coordinates (but keeping their ID)", foundStopPlace, newStopPlace);

            Set<Quay> quaysToAdd = new HashSet<>();
            for (Quay newQuay : newStopPlace.getQuays()) {
                Optional<Quay> optionalExistingQuay = findQuayWithCoordinates(newQuay, foundStopPlace.getQuays(), quaysToAdd);
                if (optionalExistingQuay.isPresent()) {
                    Quay existingQuay = optionalExistingQuay.get();
                    logger.debug("Found matching quay {} for incoming quay {}. Appending original ID to the key if required {}", existingQuay, newQuay, NetexIdMapper.ORIGINAL_ID_KEY);
                    boolean changed = keyValueListAppender.appendToOriginalId(NetexIdMapper.ORIGINAL_ID_KEY, newQuay, existingQuay);

                    if (changed) {
                        logger.info("Updated quay {}, {}", existingQuay.getId(), existingQuay);
                        updatedQuays.incrementAndGet();
                        quayRepository.save(existingQuay);
                    }
                } else {
                    logger.info("Incoming {} does not match any existing quays for {}. Adding and saving it.", newQuay, foundStopPlace);
                    saveNewQuay(newQuay, foundStopPlace, createdQuays);
                }
            }
        }

        logger.debug("Created {} quays and updated {} quays for stop place {}", createdQuays.get(), updatedQuays.get(), foundStopPlace);
        return createdQuays.get() > 0;
    }

    private void saveNewQuay(Quay newQuay, StopPlace existingStopPlace, AtomicInteger createdQuays) {
        newQuay.setId(null);
        existingStopPlace.getQuays().add(newQuay);
        quayRepository.save(newQuay);
        createdQuays.incrementAndGet();
    }

    /**
     * Find first matching quay that has the same coordinates as the new Quay.
     */
    public Optional<Quay> findQuayWithCoordinates(Quay newQuay, Collection<Quay> existingQuays, Collection<Quay> quaysToAdd) {
        List<Quay> concatenatedQuays = new ArrayList<>();
        concatenatedQuays.addAll(existingQuays);
        concatenatedQuays.addAll(quaysToAdd);

        for (Quay alreadyAddedOrExistingQuay : concatenatedQuays) {
            boolean areClose = areClose(alreadyAddedOrExistingQuay, newQuay);
            logger.info("Does quay {} and {} have the same coordinates? {}", alreadyAddedOrExistingQuay, newQuay, areClose);
            if (areClose) {
                return Optional.of(alreadyAddedOrExistingQuay);
            }
        }
        return Optional.empty();
    }

    public boolean areClose(Quay quay1, Quay quay2) {
        if (!quay1.hasCoordinates() || !quay2.hasCoordinates()) {
            return false;
        }

        Geometry buffer = quay1.getCentroid().buffer(DISTANCE);
        boolean intersects = buffer.intersects(quay2.getCentroid());
        return intersects;
    }

    private StopPlace findNearbyOrExistingStopPlace(StopPlace newStopPlace) {
        final StopPlace existingStopPlace = stopPlaceFromOriginalIdFinder.find(newStopPlace);
        if (existingStopPlace != null) {
            return existingStopPlace;
        }

        if (newStopPlace.getName() != null) {
            final StopPlace nearbyStopPlace = nearbyStopPlaceFinder.find(newStopPlace);
            if (nearbyStopPlace != null) {
                logger.debug("Found nearby stop place with name: {}, id: {}", nearbyStopPlace.getName(), nearbyStopPlace.getId());
                return nearbyStopPlace;
            }
        }
        return null;
    }

    private String getStripedSemaphoreKey(StopPlace stopPlace) {
        final String semaphoreKey;
        if (stopPlace.getName() != null
                && stopPlace.getName().getValue() != null
                && !stopPlace.getName().getValue().isEmpty()) {
            semaphoreKey = "name-" + stopPlace.getName().getValue();
        } else {
            semaphoreKey = "all";
        }
        return semaphoreKey;
    }
}
