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


import org.rutebanken.helper.organisation.ReflectionAuthorizationService;
import org.rutebanken.tiamat.auth.UsernameFetcher;
import org.rutebanken.tiamat.model.PointOfInterest;
import org.rutebanken.tiamat.model.PointOfInterestClassification;
import org.rutebanken.tiamat.repository.PointOfInterestClassificationRepository;
import org.rutebanken.tiamat.repository.PointOfInterestRepository;
import org.rutebanken.tiamat.repository.reference.ReferenceResolver;
import org.rutebanken.tiamat.service.metrics.MetricsService;
import org.rutebanken.tiamat.versioning.VersionIncrementor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
public class PointOfInterestClassificationVersionedSaverService {

    private static final Logger logger = LoggerFactory.getLogger(PointOfInterestClassificationVersionedSaverService.class);

    @Autowired
    private PointOfInterestClassificationRepository poiClassRepository;

    @Autowired
    private UsernameFetcher usernameFetcher;

    @Autowired
    private ReferenceResolver referenceResolver;

    @Autowired
    private VersionIncrementor versionIncrementor;

    @Autowired
    private MetricsService metricsService;

    @Autowired
    private ReflectionAuthorizationService reflectionAuthorizationService;

    public PointOfInterestClassification saveNewVersion(PointOfInterestClassification newVersion) {

        PointOfInterestClassification result;
        newVersion.setValidBetween(null);
        versionIncrementor.initiateOrIncrement(newVersion);
        newVersion.setChangedBy(usernameFetcher.getUserNameForAuthenticatedUser());


        if  (newVersion.getParent() != null){
            PointOfInterestClassification recoveredParent = poiClassRepository.findFirstByNetexIdOrderByVersionDesc(newVersion.getParent().getNetexId());
            newVersion.setParent(recoveredParent);

        }

        result = poiClassRepository.save(newVersion);

        logger.info("Saved POI {}, version {}, name {}", result.getNetexId(), result.getVersion(), result.getName());

        metricsService.registerEntitySaved(newVersion.getClass());
        return result;
    }
}
