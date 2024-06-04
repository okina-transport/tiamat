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
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.rutebanken.tiamat.general.PeriodicCacheLogger;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.netex.id.NetexIdHelper;
import org.rutebanken.tiamat.repository.QuayRepository;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper.ORIGINAL_ID_KEY;

/**
 * Helper class to find stop places based on saved original ID key.
 * It uses a guava cache to avoid expensive calls to the database.
 */
@Component
public class QuayFromOriginalIdFinder {

    private static final Logger logger = LoggerFactory.getLogger(QuayFromOriginalIdFinder.class);

    private QuayRepository quayRepository;

    /**
     * One original ID can be used in multiple stop places
     */
    private Cache<String, Set<String>> keyValueCache;

    private final NetexIdHelper netexIdHelper;

    @Autowired
    public QuayFromOriginalIdFinder(QuayRepository quayRepository,
                                    @Value("${stopPlaceFromOriginalIdFinderCache.maxSize:50000}") int maximumSize,
                                    @Value("${stopPlaceFromOriginalIdFinderCache.expiresAfter:30}") int expiresAfter,
                                    @Value("${stopPlaceFromOriginalIdFinderCache.expiresAfterTimeUnit:DAYS}") TimeUnit expiresAfterTimeUnit,
                                    PeriodicCacheLogger periodicCacheLogger, NetexIdHelper netexIdHelper) {
        this.quayRepository = quayRepository;
        this.netexIdHelper = netexIdHelper;
        keyValueCache = CacheBuilder.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterWrite(expiresAfter, expiresAfterTimeUnit)
                .recordStats()
                .build();

        periodicCacheLogger.scheduleCacheStatsLogging(keyValueCache, logger);
    }

    public List<Quay> find(Quay quay) {

        Set<String> originalIds = quay.getOrCreateValues(ORIGINAL_ID_KEY);

        if(originalIds.isEmpty()) return Lists.newArrayList();

        List<Quay> existingQuays = findByKeyValue(originalIds);

        return existingQuays;
    }

    public Quay findQuay(Quay quay) {

        Set<String> originalIds = quay.getOrCreateValues(ORIGINAL_ID_KEY);

        if(originalIds.isEmpty()) return null;

        Quay existingQuay = null;
        if (findByKeyValue(originalIds).size() != 0 ) existingQuay = Optional.of(findByKeyValue(originalIds).stream().findFirst().get()).orElse(null);

        if (existingQuay != null) {
            logger.debug("Found quay {} from original ID", existingQuay.getNetexId());
            return existingQuay;
        }
        return null;
    }

    public void update(Quay quay) {

        if(quay.getNetexId() == null) {
            logger.warn("Attempt to update cache when quay does not have any ID! quay: {}", quay);
            return;
        }

        for(String originalId : quay.getOrCreateValues(ORIGINAL_ID_KEY)) {

            String cacheKey = keyValKey(ORIGINAL_ID_KEY, originalId);

            Set<String> cachedValue = keyValueCache.getIfPresent(cacheKey);

            if(cachedValue != null && !cachedValue.isEmpty()) {
                logger.debug("Found existing value cached.");
                cachedValue.add(originalId);
                keyValueCache.put(cacheKey, cachedValue);
            } else {
                keyValueCache.put(cacheKey, Sets.newHashSet(originalId));
            }
        }
    }

    private List<Quay> findByKeyValue(Set<String> originalIds) {
        logger.debug("Looking for quay from original IDs: {}", originalIds);

        // No cache match
        Set<String> quayNetexIds = quayRepository.findByKeyValues(ORIGINAL_ID_KEY, originalIds, true);
        return quayNetexIds
                .stream()
                .map(quayNetexId -> quayRepository.findFirstByNetexIdOrderByVersionDesc(quayNetexId))
                .peek(this::update)
                .collect(Collectors.toList());
    }

    private String keyValKey(String key, String value) {
        return key + "-" + value;
    }
}
