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

package org.rutebanken.tiamat.versioning.save;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.rutebanken.tiamat.changelog.EntityChangedListener;
import org.rutebanken.tiamat.general.PeriodicCacheLogger;
import org.rutebanken.tiamat.model.*;
import org.rutebanken.tiamat.repository.AccessibilityAssessmentRepository;
import org.rutebanken.tiamat.repository.AccessibilityLimitationRepository;
import org.rutebanken.tiamat.service.metrics.MetricsService;
import org.rutebanken.tiamat.versioning.ValidityUpdater;
import org.rutebanken.tiamat.versioning.VersionIncrementor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.rutebanken.tiamat.versioning.save.DefaultVersionedSaverService.MILLIS_BETWEEN_VERSIONS;

@Transactional
@Service
public class AccessibilityVersionedSaverService {

    private static final Logger logger = LoggerFactory.getLogger(AccessibilityVersionedSaverService.class);
    private final Cache<String, Optional<String>> nearbyStopCache;

    @Autowired
    private AccessibilityLimitationRepository accessibilityLimitationRepository;

    @Autowired
    private AccessibilityAssessmentRepository accessibilityAssessmentRepository;

    @Autowired
    private VersionIncrementor versionIncrementor;

    @Autowired
    private MetricsService metricsService;

    @Autowired
    private EntityChangedListener entityChangedListener;

    @Autowired
    private ValidityUpdater validityUpdater;

    @Autowired
    public AccessibilityVersionedSaverService(@Value("${nearbyStopPlaceFinderCache.maxSize:50000}") int maximumSize,
                                              @Value("${nearbyStopPlaceFinderCache.expiresAfter:30}") int expiresAfter,
                                              @Value("${nearbyStopPlaceFinderCache.expiresAfterTimeUnit:DAYS}") TimeUnit expiresAfterTimeUnit,
                                              PeriodicCacheLogger periodicCacheLogger) {
        this.nearbyStopCache = CacheBuilder.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterWrite(expiresAfter, expiresAfterTimeUnit)
                .recordStats()
                .build();

        periodicCacheLogger.scheduleCacheStatsLogging(nearbyStopCache, logger);
    }

    public AccessibilityAssessment saveNewVersionAssessment(AccessibilityAssessment newVersion, Boolean withHistory) {

        AccessibilityAssessment existing = accessibilityAssessmentRepository.findFirstByNetexIdOrderByVersionDesc(newVersion.getNetexId());

        AccessibilityAssessment result;
        if (existing != null) {
            Instant newVersionValidFrom = validityUpdater.updateValidBetween(existing, newVersion, Instant.now());

            logger.trace("existing: {}", existing);
            logger.trace("new: {}", newVersion);

            newVersion.setCreated(existing.getCreated());
            newVersion.setChanged(Instant.now());
            newVersion.setVersion(existing.getVersion());

            Instant oldversionTerminationTime = newVersionValidFrom.minusMillis(MILLIS_BETWEEN_VERSIONS);
            logger.debug("About to terminate previous version for {},{}", existing.getNetexId(), existing.getVersion());
            logger.debug("Found previous version {},{}. Terminating it.", existing.getNetexId(), existing.getVersion());
            validityUpdater.terminateVersion(existing, oldversionTerminationTime);

            if (!withHistory) {
                accessibilityAssessmentRepository.delete(existing);
            }
        } else {
            newVersion.setCreated(Instant.now());
        }

        newVersion.setValidBetween(null);
        versionIncrementor.initiateOrIncrement(newVersion);

        result = accessibilityAssessmentRepository.save(newVersion);

        logger.info("Saved assessment {}, version {}", result.getNetexId(), result.getVersion());

        metricsService.registerEntitySaved(newVersion.getClass());
        nearbyStopCache.put(newVersion.getNetexId(), Optional.ofNullable(newVersion.getNetexId()));
        entityChangedListener.onChange(newVersion);
        return result;
    }

    public AccessibilityLimitation saveNewVersionLimitation(AccessibilityLimitation newVersion, Boolean withHistory) {

        AccessibilityLimitation existing = accessibilityLimitationRepository.findFirstByNetexIdOrderByVersionDesc(newVersion.getNetexId());

        AccessibilityLimitation result;
        if (existing != null) {
            Instant newVersionValidFrom = validityUpdater.updateValidBetween(existing, newVersion, Instant.now());

            logger.trace("existing: {}", existing);
            logger.trace("new: {}", newVersion);

            newVersion.setCreated(existing.getCreated());
            newVersion.setChanged(Instant.now());
            newVersion.setVersion(existing.getVersion());

            Instant oldversionTerminationTime = newVersionValidFrom.minusMillis(MILLIS_BETWEEN_VERSIONS);
            logger.debug("About to terminate previous version for {},{}", existing.getNetexId(), existing.getVersion());
            logger.debug("Found previous version {},{}. Terminating it.", existing.getNetexId(), existing.getVersion());
            validityUpdater.terminateVersion(existing, oldversionTerminationTime);

            if (!withHistory) {
                accessibilityLimitationRepository.delete(existing);
            }
        } else {
            newVersion.setCreated(Instant.now());
        }


        newVersion.setValidBetween(null);
        versionIncrementor.initiateOrIncrement(newVersion);

        result = accessibilityLimitationRepository.save(newVersion);

        logger.info("Saved limitation {}, version {}", result.getNetexId(), result.getVersion());

        metricsService.registerEntitySaved(newVersion.getClass());
        nearbyStopCache.put(newVersion.getNetexId(), Optional.ofNullable(newVersion.getNetexId()));
        entityChangedListener.onChange(newVersion);
        return result;
    }
}
