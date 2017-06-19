package org.rutebanken.tiamat.importer.finder;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheStats;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.rutebanken.tiamat.importer.AlternativeStopTypes;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.model.StopTypeEnumeration;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class NearbyStopPlaceFinder {

    private static final double BOUNDING_BOX_BUFFER = 0.004;

    private static final Logger logger = LoggerFactory.getLogger(NearbyStopPlaceFinder.class);

    private final StopPlaceRepository stopPlaceRepository;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final AlternativeStopTypes alternativeStopTypes;

    /**
     * Key is generated by using stop place's name, type and envelope.
     * Value is optional NetexId
     */
    private final Cache<String, Optional<String>> nearbyStopCache;

    @Autowired
    public NearbyStopPlaceFinder(StopPlaceRepository stopPlaceRepository,
                                 @Value("${nearbyStopPlaceFinderCache.maxSize:50000}") int maximumSize,
                                 @Value("${nearbyStopPlaceFinderCache.expiresAfter:30}") int expiresAfter,
                                 @Value("${nearbyStopPlaceFinderCache.expiresAfterTimeUnit:DAYS}") TimeUnit expiresAfterTimeUnit, AlternativeStopTypes alternativeStopTypes) {
        this.stopPlaceRepository = stopPlaceRepository;
        this.alternativeStopTypes = alternativeStopTypes;
        this.nearbyStopCache = CacheBuilder.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterWrite(expiresAfter, expiresAfterTimeUnit)
                .recordStats()
                .build();


        final CacheStats[] last = {null};

        scheduler.scheduleAtFixedRate(() -> {
            CacheStats newStats = nearbyStopCache.stats();
            if (last[0] == null || !newStats.toString().equals(last[0].toString())) {
                logger.info("{}", newStats);
            }
            last[0] = newStats;

        }, 1, 2, TimeUnit.MINUTES);
    }

    public StopPlace find(StopPlace stopPlace) {
        return find(stopPlace, false);
    }

    public StopPlace find(StopPlace stopPlace, boolean allowOther) {

        if (!stopPlace.hasCoordinates()) {
            return null;
        }

        if (stopPlace.getStopPlaceType() == null) {
            logger.warn("Stop place does not have type. Cannot check for similar stop places. {}", stopPlace);
            return null;
        }

        final String key = createKey(stopPlace);

        try {
            Optional<String> stopPlaceNetexId = nearbyStopCache.get(key, () -> {
                Envelope boundingBox = createBoundingBox(stopPlace.getCentroid());

                String matchingStopPlaceId;
                if (stopPlace.getStopPlaceType().equals(StopTypeEnumeration.OTHER) && allowOther) {
                    // Allow finding stop places of any type if stop place type is other and allowOther is true
                    matchingStopPlaceId = stopPlaceRepository.findNearbyStopPlace(boundingBox, stopPlace.getName().getValue());
                } else {
                    matchingStopPlaceId = stopPlaceRepository.findNearbyStopPlace(boundingBox, stopPlace.getName().getValue(), stopPlace.getStopPlaceType());

                    if (matchingStopPlaceId == null) {
                        Set<StopTypeEnumeration> alternativeTypes = alternativeStopTypes.getAlternativeTypes(stopPlace.getStopPlaceType());

                        if (alternativeTypes != null) {
                            for (StopTypeEnumeration alternativeType : alternativeTypes) {
                                matchingStopPlaceId = stopPlaceRepository.findNearbyStopPlace(boundingBox, stopPlace.getName().getValue(), alternativeType);
                                if (matchingStopPlaceId != null) {
                                    logger.info("Found matching stop place based on alternative type {} from type {}", alternativeType, stopPlace.getStopPlaceType());
                                    break;
                                }
                            }
                        }
                    }
                }

                return Optional.ofNullable(matchingStopPlaceId);
            });
            if (stopPlaceNetexId.isPresent()) {
                // Update cache for incoming envelope, so the same key will hopefullly match again
                nearbyStopCache.put(key, stopPlaceNetexId);
                return stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(stopPlaceNetexId.get());
            }
            return null;
        } catch (ExecutionException e) {
            logger.warn("Caught exception while finding stop place by key and value.", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Update cache. For instance after modifying and saving stop place.
     *
     * @param savedStopPlace
     */
    public void update(StopPlace savedStopPlace) {
        if (savedStopPlace.hasCoordinates() && savedStopPlace.getStopPlaceType() != null) {
            nearbyStopCache.put(createKey(savedStopPlace), Optional.ofNullable(savedStopPlace.getNetexId()));
        }
    }

    public final String createKey(StopPlace stopPlace, Envelope envelope) {
        return stopPlace.getName() + "-" + stopPlace.getStopPlaceType().value() + "-" + envelope.toString();
    }

    public final String createKey(StopPlace stopPlace) {
        return createKey(stopPlace, createBoundingBox(stopPlace.getCentroid()));
    }

    public Envelope createBoundingBox(Point point) {

        Geometry buffer = point.buffer(BOUNDING_BOX_BUFFER);

        Envelope envelope = buffer.getEnvelopeInternal();
        logger.trace("Created envelope {}", envelope.toString());

        return envelope;
    }
}
