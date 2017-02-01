package org.rutebanken.tiamat.importer;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.operation.TransformException;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class QuayMerger {

    private static final Logger logger = LoggerFactory.getLogger(QuayMerger.class);

    @Value("${quayMerger.mergeDistanceMeters:10}")
    public final double MERGE_DISTANCE_METERS = 10;

    @Value("${quayMerger.maxCompassBearingDifference:60}")
    private final int maxCompassBearingDifference = 60;

    /**
     * Inspect quays from incoming AND matching stop place. If they do not exist from before, add them.
     */
    public boolean addNewQuaysOrAppendImportIds(StopPlace newStopPlace, StopPlace existingStopPlace) {

        AtomicInteger updatedQuays = new AtomicInteger();
        AtomicInteger addedQuays = new AtomicInteger();

        logger.debug("About to compare quays for {}", existingStopPlace.getId());

        if (newStopPlace.getQuays() == null) {
            newStopPlace.setQuays(new HashSet<>());
        }

        Set<Quay> result = addNewQuaysOrAppendImportIds(newStopPlace.getQuays(), existingStopPlace.getQuays(), updatedQuays, addedQuays);

        existingStopPlace.setQuays(result);

        logger.debug("Created {} quays and updated {} quays for stop place {}", addedQuays.get(), updatedQuays.get(), existingStopPlace);
        return addedQuays.get() > 0 || updatedQuays.get() > 0;
    }

    public Set<Quay> addNewQuaysOrAppendImportIds(Set<Quay> newQuays, Set<Quay> existingQuays, AtomicInteger updatedQuaysCounter, AtomicInteger addedQuaysCounter) {

        Set<Quay> result = new HashSet<>();
        if(existingQuays == null) {
            existingQuays = new HashSet<>();
        }
        result.addAll(existingQuays);

        for(Quay incomingQuay : newQuays) {

            boolean foundMatch = false;
            for(Quay alreadyAdded : result) {
                foundMatch = appendIdIfMatchingOriginalId(incomingQuay, alreadyAdded, updatedQuaysCounter);
                if(foundMatch) {
                    break;
                }
            }

            if(!foundMatch) {
                for (Quay alreadyAdded : result) {
                    foundMatch = appendIdIfCloseAndSimilarCompassBearing(incomingQuay, alreadyAdded, updatedQuaysCounter);
                    if (foundMatch) {
                        break;
                    }
                }
            }

            if(!foundMatch) {
                logger.info("Found no match for existing quay {}. Adding it!", incomingQuay);
                result.add(incomingQuay);
                incomingQuay.setCreated(ZonedDateTime.now());
                incomingQuay.setChanged(ZonedDateTime.now());
                addedQuaysCounter.incrementAndGet();
            }
        }

        return result;
    }

    private boolean appendIdIfCloseAndSimilarCompassBearing(Quay incomingQuay, Quay alreadyAdded, AtomicInteger updatedQuaysCounter) {

        if (areClose(incomingQuay, alreadyAdded) && hasCloseCompassBearing(incomingQuay, alreadyAdded)) {
            logger.info("New quay {} is close to existing quay {}. Appending it's ID", incomingQuay, alreadyAdded);
            boolean changed = alreadyAdded.getOriginalIds().addAll(incomingQuay.getOriginalIds());
            if (changed) {
                incomingQuay.setChanged(ZonedDateTime.now());
                updatedQuaysCounter.incrementAndGet();
            }
            return true;
        }
        return false;
    }

    /**
     * If the incoming Quay has an original ID that matches on any original ID on an existing Quay, append Ids.
     * @param incomingQuay incoming Quay with
     * @param alreadyAdded
     * @param updatedQuaysCounter
     * @return
     */
    private boolean appendIdIfMatchingOriginalId(Quay incomingQuay, Quay alreadyAdded, AtomicInteger updatedQuaysCounter) {
        Set<String> strippedAlreadyAddedIds = removePrefixesFromIds(alreadyAdded.getOriginalIds());
        Set<String> strippedIncomingIds = removePrefixesFromIds(incomingQuay.getOriginalIds());

        if(!Collections.disjoint(strippedAlreadyAddedIds, strippedIncomingIds)) {
            logger.info("New quay matches on original ID: {}. Adding all new IDs if any. Existing quay ID: {}", incomingQuay, alreadyAdded.getId());
            // The incoming quay could for some reason already have multiple imported IDs.
            boolean changed = alreadyAdded.getOriginalIds().addAll(incomingQuay.getOriginalIds());
            if(changed) {
                incomingQuay.setChanged(ZonedDateTime.now());
                updatedQuaysCounter.incrementAndGet();
            }
            return true;
        }
        return false;
    }

    private Set<String> removePrefixesFromIds(Set<String> originalIds) {
        Set<String> strippedIds = new HashSet<>(originalIds.size());
        originalIds.forEach(completeId -> {
            if(completeId.contains(":")) {
                strippedIds.add(completeId.substring(completeId.indexOf(':')));
            } else {
                logger.info("Cannot strip prefix from ID {} as it does not contain colon", completeId);
                strippedIds.add(completeId);
            }
        });
        return strippedIds;
    }

    public boolean areClose(Quay quay1, Quay quay2) {
        if (!quay1.hasCoordinates() || !quay2.hasCoordinates()) {
            return false;
        }

        try {
            double distanceInMeters = JTS.orthodromicDistance(
                    quay1.getCentroid().getCoordinate(),
                    quay2.getCentroid().getCoordinate(),
                    DefaultGeographicCRS.WGS84);

            return distanceInMeters < MERGE_DISTANCE_METERS;
        } catch (TransformException e) {
            logger.warn("Could not calculate distance", e);
            return false;
        }
    }

    public boolean hasCloseCompassBearing(Quay quay1, Quay quay2) {

        if(quay1.getCompassBearing() == null && quay2.getCompassBearing() == null) {
            return true;
        } else if ((quay1.getCompassBearing() == null && quay2.getCompassBearing() != null) || (quay1.getCompassBearing() != null && quay2.getCompassBearing() == null)) {
            return true;
        }

        int quayBearing1 = Math.round(quay1.getCompassBearing());
        int quayBearing2 = Math.round(quay2.getCompassBearing());

        int difference = Math.abs(getAngle(quayBearing1, quayBearing2));

        if (difference > maxCompassBearingDifference) {
            logger.debug("Quays have too much difference in compass bearing {}. {} {}", difference, quay1, quay2);
            return false;
        }

        logger.debug("Compass bearings for quays has less difference than the limit {}. {} {}", difference, quay1, quay2);
        return true;
    }

    private int getAngle(Integer bearing, Integer heading) {
        return ((((bearing - heading) % 360) + 540) % 360) - 180;

    }


}
