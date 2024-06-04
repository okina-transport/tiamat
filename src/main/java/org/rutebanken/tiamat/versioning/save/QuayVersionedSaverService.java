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

import org.locationtech.jts.geom.Point;
import org.rutebanken.tiamat.auth.StopPlaceAuthorizationService;
import org.rutebanken.tiamat.auth.UsernameFetcher;
import org.rutebanken.tiamat.changelog.EntityChangedListener;
import org.rutebanken.tiamat.diff.TiamatObjectDiffer;
import org.rutebanken.tiamat.geo.ZoneDistanceChecker;
import org.rutebanken.tiamat.importer.finder.NearbyQuayFinder;
import org.rutebanken.tiamat.importer.finder.StopPlaceByQuayOriginalIdFinder;
import org.rutebanken.tiamat.model.*;
import org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper;
import org.rutebanken.tiamat.repository.QuayRepository;
import org.rutebanken.tiamat.repository.TariffZoneRepository;
import org.rutebanken.tiamat.service.TariffZonesLookupService;
import org.rutebanken.tiamat.service.TopographicPlaceLookupService;
import org.rutebanken.tiamat.service.metrics.MetricsService;
import org.rutebanken.tiamat.versioning.ValidityUpdater;
import org.rutebanken.tiamat.versioning.VersionIncrementor;
import org.rutebanken.tiamat.versioning.util.AccessibilityAssessmentOptimizer;
import org.rutebanken.tiamat.versioning.validate.SubmodeValidator;
import org.rutebanken.tiamat.versioning.validate.VersionValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static org.rutebanken.tiamat.versioning.save.DefaultVersionedSaverService.MILLIS_BETWEEN_VERSIONS;


@Transactional
@Service
public class QuayVersionedSaverService {

    private static final Logger logger = LoggerFactory.getLogger(QuayVersionedSaverService.class);

    public static final int ADJACENT_STOP_PLACE_MAX_DISTANCE_IN_METERS = 30;

    public static final InterchangeWeightingEnumeration DEFAULT_WEIGHTING = InterchangeWeightingEnumeration.INTERCHANGE_ALLOWED;

    @Autowired
    private ZoneDistanceChecker zoneDistanceChecker;

    @Autowired
    private QuayRepository quayRepository;

    @Autowired
    private TariffZoneRepository tariffZoneRepository;

    @Autowired
    private AccessibilityAssessmentOptimizer accessibilityAssessmentOptimizer;

    @Autowired
    private TopographicPlaceLookupService countyAndMunicipalityLookupService;

    @Autowired
    private TariffZonesLookupService tariffZonesLookupService;

    @Autowired
    private StopPlaceByQuayOriginalIdFinder stopPlaceByQuayOriginalIdFinder;

    @Autowired
    private NearbyQuayFinder nearbyQuayFinder;

    @Autowired
    private EntityChangedListener entityChangedListener;

    @Autowired
    private SubmodeValidator submodeValidator;

    @Autowired
    private StopPlaceAuthorizationService stopPlaceAuthorizationService;

    @Autowired
    private ValidityUpdater validityUpdater;

    @Autowired
    private VersionIncrementor versionIncrementor;

    @Autowired
    private UsernameFetcher usernameFetcher;

    @Autowired
    private VersionValidator versionValidator;

    @Autowired
    private TiamatObjectDiffer tiamatObjectDiffer;

    @Autowired
    private MetricsService metricsService;

    public Quay saveNewVersion(Quay newVersion) {
        return saveNewVersion(null, newVersion);
    }

    public Quay saveNewVersion(Quay existingVersion, Quay newVersion) {

//        if (existingVersion == null) {
//            existingVersion = quayRepository.findFirstByNetexIdOrderByVersionDesc(newVersion.getNetexId());
//        }

        Instant changed = Instant.now();

        logger.debug("Rearrange accessibility assessments for: {}", newVersion);
        accessibilityAssessmentOptimizer.optimizeAccessibilityAssessmentsQuay(newVersion);

//        Instant newVersionValidFrom = validityUpdater.updateValidBetween(existingVersion, newVersion, defaultValidFrom);

        if (existingVersion == null) {
            logger.debug("Existing version is not present, which means new entity. {}", newVersion);
            newVersion.setCreated(changed);
        } else {
            newVersion.setChanged(changed);
            logger.debug("About to terminate previous version for {},{}", existingVersion.getNetexId(), existingVersion.getVersion());
            Quay existingVersionRefetched = quayRepository.findFirstByNetexIdOrderByVersionDesc(existingVersion.getNetexId());
            logger.debug("Found previous version {},{}. Terminating it.", existingVersionRefetched.getNetexId(), existingVersionRefetched.getVersion());
        }

        newVersion = versionIncrementor.initiateOrIncrementVersionsQuay(newVersion);

        newVersion.setChangedBy(usernameFetcher.getUserNameForAuthenticatedUser());
        logger.info("Quay [{}], version {} changed by user [{}]. {}", newVersion.getNetexId(), newVersion.getVersion(), newVersion.getChangedBy(), newVersion.getValidBetween());

        newVersion = quayRepository.save(newVersion);
        logger.debug("Saved quay with id: {}", newVersion.getId());

        if (existingVersion != null) {
            tiamatObjectDiffer.logDifference(existingVersion, newVersion);
        }
        metricsService.registerEntitySaved(newVersion.getClass());

        nearbyQuayFinder.update(newVersion);
        entityChangedListener.onChange(newVersion);

        return newVersion;
    }
}
