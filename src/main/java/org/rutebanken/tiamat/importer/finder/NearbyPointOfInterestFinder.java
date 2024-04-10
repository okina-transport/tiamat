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
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.rutebanken.tiamat.model.PointOfInterest;
import org.rutebanken.tiamat.repository.PointOfInterestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Component
public class NearbyPointOfInterestFinder implements PointOfInterestFinder {

    private static final double BOUNDING_BOX_BUFFER = 0.004;

    private static final Logger logger = LoggerFactory.getLogger(NearbyPointOfInterestFinder.class);

    private PointOfInterestRepository pointOfInterestRepository;

    /**
     * Key is generated by using parking's name, type and envelope.
     * Value is optional NetexId
     */
    private final Cache<String, Optional<String>> nearbyPoiCache;

    @Autowired
    public NearbyPointOfInterestFinder(PointOfInterestRepository pointOfInterestRepository,
                                       @Value("${nearbyPoiFinderCache.maxSize:50000}") int maximumSize,
                                       @Value("${nearbyPoiFinderCache.expiresAfter:30}") int expiresAfter,
                                       @Value("${nearbyPoiFinderCache.expiresAfterTimeUnit:DAYS}") TimeUnit expiresAfterTimeUnit) {
        this.pointOfInterestRepository = pointOfInterestRepository;
        this.nearbyPoiCache = CacheBuilder.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterWrite(expiresAfter, expiresAfterTimeUnit)
                .build();

    }

    @Override
    public PointOfInterest find(PointOfInterest pointOfInterest) {
        if(!pointOfInterest.hasCoordinates()) {
            return null;
        }

        try {
            if (pointOfInterest.getNetexId() == null) {
                Optional<String> pointOfInterestNetexId = nearbyPoiCache.get(createKey(pointOfInterest), () -> {
                    Envelope boundingBox = createBoundingBox(pointOfInterest.getCentroid());

                    String matchingPointOfInterestId = pointOfInterestRepository.findNearbyPOI(boundingBox, pointOfInterest.getName().getValue());

                    return Optional.ofNullable(matchingPointOfInterestId);
                });
                if (pointOfInterestNetexId.isPresent()) {
                    return pointOfInterestRepository.findFirstByNetexIdOrderByVersionDesc(pointOfInterestNetexId.get());
                }
            } else {
                return pointOfInterestRepository.findFirstByNetexIdOrderByVersionDesc(pointOfInterest.getNetexId());
            }
            return null;
        } catch (ExecutionException e) {
            logger.warn("Caught exception while finding point of interest by key and value.", e);
            throw new RuntimeException(e);
        }
    }

    public void update(PointOfInterest pointOfInterest) {
        if(pointOfInterest.hasCoordinates()) {
            nearbyPoiCache.put(createKey(pointOfInterest), Optional.ofNullable(pointOfInterest.getNetexId()));
        }
    }

    public final String createKey(PointOfInterest pointOfInterest, Envelope envelope) {
        return pointOfInterest.getName() + "-" + envelope.toString();
    }

    public final String createKey(PointOfInterest pointOfInterest) {
        return createKey(pointOfInterest, createBoundingBox(pointOfInterest.getCentroid()));
    }

    public Envelope createBoundingBox(Point point) {

        Geometry buffer = point.buffer(BOUNDING_BOX_BUFFER);

        Envelope envelope = buffer.getEnvelopeInternal();
        logger.trace("Created envelope {}", envelope.toString());

        return envelope;
    }
}
