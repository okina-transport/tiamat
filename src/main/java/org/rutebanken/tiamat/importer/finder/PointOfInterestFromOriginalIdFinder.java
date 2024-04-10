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
import org.rutebanken.tiamat.model.PointOfInterest;
import org.rutebanken.tiamat.repository.PointOfInterestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper.ORIGINAL_ID_KEY;

/**
 * Helper class to find stop places based on saved original ID key.
 * It uses a guava cache to avoid expensive calls to the database.
 */
@Component
public class PointOfInterestFromOriginalIdFinder implements PointOfInterestFinder {

    private static final Logger logger = LoggerFactory.getLogger(PointOfInterestFromOriginalIdFinder.class);

    private PointOfInterestRepository pointOfInterestRepository;

    private Cache<String, Optional<String>> keyValueCache;

    public PointOfInterestFromOriginalIdFinder(PointOfInterestRepository pointOfInterestRepository,
                                               @Value("${parkingFromOriginalIdFinderCache.maxSize:50000}") int maximumSize,
                                               @Value("${parkingFromOriginalIdFinderCache.expiresAfter:30}") int expiresAfter,
                                               @Value("${parkingFromOriginalIdFinderCache.expiresAfterTimeUnit:DAYS}") TimeUnit expiresAfterTimeUnit) {
        this.pointOfInterestRepository = pointOfInterestRepository;
        keyValueCache = CacheBuilder.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterWrite(expiresAfter, expiresAfterTimeUnit)
                .build();
    }

    @Override
    public PointOfInterest find(PointOfInterest pointOfInterest) {

        Set<String> originalIds = pointOfInterest.getOrCreateValues(ORIGINAL_ID_KEY);

        if(originalIds.isEmpty()) return null;

        PointOfInterest existingPointOfInterest = findByKeyValue(originalIds);

        if (existingPointOfInterest != null) {
            logger.debug("Found point of interest {} from original ID", existingPointOfInterest.getNetexId());
            return existingPointOfInterest;
        }
        return null;
    }

    public void update(PointOfInterest pointOfInterest) {
        if(pointOfInterest.getNetexId() == null) {
            logger.warn("Attempt to update cache when point of interest does not have any ID! stop place: {}", pointOfInterest);
            return;
        }
        for(String originalId : pointOfInterest.getOrCreateValues(ORIGINAL_ID_KEY)) {
            keyValueCache.put(keyValKey(ORIGINAL_ID_KEY, originalId), Optional.ofNullable(pointOfInterest.getNetexId()));
        }
    }

    private PointOfInterest findByKeyValue(Set<String> originalIds) {
        for(String originalId : originalIds) {
            String cacheKey = keyValKey(ORIGINAL_ID_KEY, originalId);
            Optional<String> matchingPointOfInterestNetexId = keyValueCache.getIfPresent(cacheKey);
            if(matchingPointOfInterestNetexId != null && matchingPointOfInterestNetexId.isPresent()) {
                logger.debug("Cache match. Key {}, point of interest id: {}", cacheKey, matchingPointOfInterestNetexId.get());
                return pointOfInterestRepository.findFirstByNetexIdOrderByVersionDesc(matchingPointOfInterestNetexId.get());
            }
        }

        // No cache match
        String pointOfInterestNetexId = pointOfInterestRepository.findFirstByKeyValues(ORIGINAL_ID_KEY, originalIds);
        if(pointOfInterestNetexId != null) {
            return pointOfInterestRepository.findFirstByNetexIdOrderByVersionDesc(pointOfInterestNetexId);
        }
        return null;
    }

    private String keyValKey(String key, String value) {
        return key + "-" + value;
    }
}
