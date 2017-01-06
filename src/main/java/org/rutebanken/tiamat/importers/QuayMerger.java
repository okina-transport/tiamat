package org.rutebanken.tiamat.importers;

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

    /**
     * The max distance for checking if two quays are nearby each other.
     * http://gis.stackexchange.com/questions/28799/what-is-the-unit-of-measurement-for-buffer-calculation
     * https://en.wikipedia.org/wiki/Decimal_degrees
     */
    @Value("${quayMerger.mergeDistanceMeters:10}")
    public static final double MERGE_DISTANCE_METERS = 10;

    private static final Logger logger = LoggerFactory.getLogger(QuayMerger.class);

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

    private boolean appendIdIfMatchingOriginalId(Quay incomingQuay, Quay alreadyAdded, AtomicInteger updatedQuaysCounter) {
        if(!Collections.disjoint(alreadyAdded.getOriginalIds(), incomingQuay.getOriginalIds())) {
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
        } else if (quay1.getCompassBearing() == null && quay2.getCompassBearing() != null || quay1.getCompassBearing() != null && quay2.getCompassBearing() == null) {
            return false;
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
