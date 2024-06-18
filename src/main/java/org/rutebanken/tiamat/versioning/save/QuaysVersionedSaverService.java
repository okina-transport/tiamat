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

import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.repository.QuayRepository;
import org.rutebanken.tiamat.service.metrics.MetricsService;
import org.rutebanken.tiamat.versioning.VersionIncrementor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Transactional
@Service
public class QuaysVersionedSaverService {

    private static final Logger logger = LoggerFactory.getLogger(QuaysVersionedSaverService.class);

    @Autowired
    private QuayRepository quayRepository;

    @Autowired
    private VersionIncrementor versionIncrementor;

    @Autowired
    private MetricsService metricsService;

    public Quay saveNewVersion(Quay newVersion) {

        Quay existing = quayRepository.findFirstByNetexIdOrderByVersionDesc(newVersion.getNetexId());

        Quay result;
        if (existing != null) {
            logger.trace("existing: {}", existing);
            logger.trace("new: {}", newVersion);

            newVersion.setCreated(existing.getCreated());
            newVersion.setChanged(Instant.now());
            newVersion.setVersion(existing.getVersion());

            quayRepository.delete(existing);
        } else {
            newVersion.setCreated(Instant.now());
        }


        newVersion.setValidBetween(null);
        versionIncrementor.initiateOrIncrement(newVersion);

        result = quayRepository.save(newVersion);

        logger.info("Saved quay {}, version {}", result.getNetexId(), result.getVersion());

        metricsService.registerEntitySaved(newVersion.getClass());
        return result;
    }
}
