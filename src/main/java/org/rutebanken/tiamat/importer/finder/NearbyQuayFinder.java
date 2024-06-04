/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package org.rutebanken.tiamat.importer.finder;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.lang3.StringUtils;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.rutebanken.tiamat.domain.Provider;
import org.rutebanken.tiamat.general.PeriodicCacheLogger;
import org.rutebanken.tiamat.importer.AlternativeStopTypes;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopTypeEnumeration;
import org.rutebanken.tiamat.repository.QuayRepository;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Component
public class NearbyQuayFinder {

    private static final double BOUNDING_BOX_BUFFER = 0.004;

    private static final Logger logger = LoggerFactory.getLogger(NearbyQuayFinder.class);

    private final QuayRepository quayRepository;

    private final AlternativeStopTypes alternativeStopTypes;


    /**
     * Key is generated by using quay's name, type and envelope.
     * Value is optional NetexId
     */
    private final Cache<String, Optional<String>> nearbyStopCache;

    @Autowired
    public NearbyQuayFinder(QuayRepository quayRepository,
                            @Value("${nearbyStopPlaceFinderCache.maxSize:50000}") int maximumSize,
                            @Value("${nearbyStopPlaceFinderCache.expiresAfter:30}") int expiresAfter,
                            @Value("${nearbyStopPlaceFinderCache.expiresAfterTimeUnit:DAYS}") TimeUnit expiresAfterTimeUnit,
                            PeriodicCacheLogger periodicCacheLogger, AlternativeStopTypes alternativeStopTypes) {
        this.quayRepository = quayRepository;
        this.alternativeStopTypes = alternativeStopTypes;
        this.nearbyStopCache = CacheBuilder.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterWrite(expiresAfter, expiresAfterTimeUnit)
                .recordStats()
                .build();

        periodicCacheLogger.scheduleCacheStatsLogging(nearbyStopCache, logger);
    }

    public Quay find(Quay quay) {
        if (!quay.hasCoordinates()) {
            return null;
        }

        if (quay.getTransportMode() == null) {
            logger.warn("Quay does not have type. Cannot check for similar quays. {}", quay);
            return null;
        }

        final String key = createKey(quay);

        try {
            Optional<String> quayNetexId = nearbyStopCache.get(key, () -> {
                Envelope boundingBox = createBoundingBox(quay.getCentroid());

                String matchingQuayId = quayRepository.findNearbyQuay(boundingBox, quay.getName().getValue(), quay.getPublicCode());
                return Optional.ofNullable(matchingQuayId);
            });
            if (quayNetexId.isPresent()) {
                // Update cache for incoming envelope, so the same key will hopefullly match again
                nearbyStopCache.put(key, quayNetexId);
                return quayRepository.findFirstByNetexIdOrderByVersionDesc(quayNetexId.get());
            }
            return null;
        } catch (ExecutionException e) {
            logger.warn("Caught exception while finding quay by key and value.", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Update cache. For instance after modifying and saving quay.
     *
     * @param savedStopPlace
     */
    public void update(Quay savedStopPlace) {
        if (savedStopPlace.hasCoordinates() && savedStopPlace.getTransportMode() != null) {
            nearbyStopCache.put(createKey(savedStopPlace), Optional.ofNullable(savedStopPlace.getNetexId()));
        }
    }

    public final String createKey(Quay quay, Envelope envelope) {
        return quay.getName() + "-" + quay.getTransportMode().value() + "-" + envelope.toString();
    }

    public final String createKey(Quay quay) {
        return createKey(quay, createBoundingBox(quay.getCentroid()));
    }

    public Envelope createBoundingBox(Point point) {

        Geometry buffer = point.buffer(BOUNDING_BOX_BUFFER);

        Envelope envelope = buffer.getEnvelopeInternal();
        logger.trace("Created envelope {}", envelope.toString());

        return envelope;
    }
}
