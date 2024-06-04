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

package org.rutebanken.tiamat.importer.merging;

import org.rutebanken.tiamat.geo.QuayCentroidComputer;
import org.rutebanken.tiamat.importer.finder.NearbyQuayFinder;
import org.rutebanken.tiamat.importer.finder.QuayFromOriginalIdFinder;
import org.rutebanken.tiamat.model.DataManagedObjectStructure;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.netex.mapping.NetexMapper;
import org.rutebanken.tiamat.repository.reference.ReferenceResolver;
import org.rutebanken.tiamat.versioning.VersionCreator;
import org.rutebanken.tiamat.versioning.save.QuayVersionedSaverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@Qualifier("mergingStopPlaceImporter")
@Transactional
public class MergingQuayImporter {

    private static final Logger logger = LoggerFactory.getLogger(MergingQuayImporter.class);

    private final QuayFromOriginalIdFinder quayFromOriginalIdFinder;

    private final NearbyQuayFinder nearbyQuayFinder;

    private final QuayCentroidComputer quayCentroidComputer;

    private final NetexMapper netexMapper;

    private final QuayVersionedSaverService quayVersionedSaverService;

    private final VersionCreator versionCreator;

    private final ReferenceResolver referenceResolver;

    @Autowired
    public MergingQuayImporter(QuayFromOriginalIdFinder quayFromOriginalIdFinder,
                               NearbyQuayFinder nearbyQuayFinder,
                               QuayCentroidComputer quayCentroidComputer,
                               NetexMapper netexMapper,
                               QuayVersionedSaverService quayVersionedSaverService,
                               VersionCreator versionCreator,
                               ReferenceResolver referenceResolver) {
        this.quayFromOriginalIdFinder = quayFromOriginalIdFinder;
        this.nearbyQuayFinder = nearbyQuayFinder;
        this.quayCentroidComputer = quayCentroidComputer;
        this.netexMapper = netexMapper;
        this.quayVersionedSaverService = quayVersionedSaverService;
        this.versionCreator = versionCreator;
        this.referenceResolver = referenceResolver;
    }

    public org.rutebanken.netex.model.Quay importQuay(Quay newQuay) {

        logger.debug("Transaction active: {}. Isolation level: {}", TransactionSynchronizationManager.isActualTransactionActive(), TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());

        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new RuntimeException("Transaction with required "
                    + "TransactionSynchronizationManager.isActualTransactionActive(): " + TransactionSynchronizationManager.isActualTransactionActive());
        }

        return netexMapper.mapToNetexModel(importQuayWithoutNetexMapping(newQuay));
    }

    public Quay importQuayWithoutNetexMapping(Quay incomingQuay) {

        final Quay foundQuay = findNearbyOrExistingQuay(incomingQuay);

        final Quay quay;
        if (foundQuay != null) {
            quay = handleCompletelyNewParking(foundQuay);

        } else {
            quay = handleCompletelyNewParking(incomingQuay);
        }

        resolveAndFixParentZoneRef(quay);

        return quay;
    }

    private void resolveAndFixParentZoneRef(Quay quay) {
        if (quay != null && quay.getParentZoneRef() != null) {
            DataManagedObjectStructure referencedQuay = referenceResolver.resolve(quay.getParentZoneRef());
            quay.getParentZoneRef().setRef(referencedQuay.getNetexId());
        }
    }

    public Quay handleCompletelyNewParking(Quay incomingQuay) {
        quayCentroidComputer.computeCentroidForQuay(incomingQuay);
        logger.debug("New quay: {}. Setting version to \"1\"", incomingQuay.getName());
        incomingQuay = quayVersionedSaverService.saveNewVersion(incomingQuay);
        return updateCache(incomingQuay);
    }

    private Quay updateCache(Quay quay) {
        // Keep the attached quay reference in case it is merged.
        quayFromOriginalIdFinder.update(quay);
        nearbyQuayFinder.update(quay);
        logger.info("Saved quay {}", quay);
        return quay;
    }

    private Quay findNearbyOrExistingQuay(Quay newQuay) {
        final Quay existingQuay = quayFromOriginalIdFinder.findQuay(newQuay);
        if (existingQuay != null) {
            return existingQuay;
        }

        if (newQuay.getName() != null) {
            final Quay nearbyQuay = nearbyQuayFinder.find(newQuay);
            if (nearbyQuay != null) {
                logger.debug("Found nearby quay with name: {}, id: {}", nearbyQuay.getName(), nearbyQuay.getNetexId());
                return nearbyQuay;
            }
        }
        return null;
    }

}
