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

import org.rutebanken.tiamat.importer.finder.NearbyPointOfInterestFinder;
import org.rutebanken.tiamat.importer.finder.PointOfInterestFromOriginalIdFinder;
import org.rutebanken.tiamat.model.*;
import org.rutebanken.tiamat.netex.mapping.NetexMapper;
import org.rutebanken.tiamat.repository.reference.ReferenceResolver;
import org.rutebanken.tiamat.versioning.VersionCreator;
import org.rutebanken.tiamat.versioning.save.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Component
@Qualifier("mergingPoiImporter")
@Transactional
public class MergingPointOfInterestImporter {

    private static final Logger logger = LoggerFactory.getLogger(MergingPointOfInterestImporter.class);

    private final NetexMapper netexMapper;

    private final NearbyPointOfInterestFinder nearbyPointOfInterestFinder;

    private final PointOfInterestVersionedSaverService pointOfInterestVersionedSaverService;

    private final PointOfInterestClassificationVersionedSaverService pointOfInterestClassificationVersionedSaverService;

    private final PointOfInterestFromOriginalIdFinder poiFromOriginalIdFinder;

    private final ReferenceResolver referenceResolver;

    private final VersionCreator versionCreator;

    private final MergingUtils mergingUtils;

    @Autowired
    public MergingPointOfInterestImporter(PointOfInterestFromOriginalIdFinder poiFromOriginalIdFinder,
                                          NearbyPointOfInterestFinder nearbyParkingFinder,
                                          ReferenceResolver referenceResolver,
                                          NetexMapper netexMapper,
                                          PointOfInterestVersionedSaverService pointOfInterestVersionedSaverService,
                                          PointOfInterestClassificationVersionedSaverService pointOfInterestClassificationVersionedSaverService,
                                          VersionCreator versionCreator,
                                          MergingUtils mergingUtils) {
        this.poiFromOriginalIdFinder = poiFromOriginalIdFinder;
        this.nearbyPointOfInterestFinder = nearbyParkingFinder;
        this.referenceResolver = referenceResolver;
        this.netexMapper = netexMapper;
        this.pointOfInterestVersionedSaverService = pointOfInterestVersionedSaverService;
        this.pointOfInterestClassificationVersionedSaverService = pointOfInterestClassificationVersionedSaverService;
        this.versionCreator = versionCreator;
        this.mergingUtils = mergingUtils;
    }

    public org.rutebanken.netex.model.PointOfInterest importPointOfInterest(PointOfInterest pointOfInterest) throws InterruptedException, ExecutionException {

        logger.debug("Transaction active: {}. Isolation level: {}", TransactionSynchronizationManager.isActualTransactionActive(), TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());

        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new RuntimeException("Transaction with required "
                    + "TransactionSynchronizationManager.isActualTransactionActive(): " + TransactionSynchronizationManager.isActualTransactionActive());
        }

        return netexMapper.mapToNetexModel(importPOIWithoutNetexMapping(pointOfInterest));
    }

    public PointOfInterest importPOIWithoutNetexMapping(PointOfInterest newPointOfInerest) throws InterruptedException, ExecutionException {
        final PointOfInterest foundPointOfInterest = findNearbyOrExistingPointOfInterest(newPointOfInerest);

        final PointOfInterest pointOfInterest;
        if (foundPointOfInterest != null) {
            pointOfInterest = handleAlreadyExistingPointOfInterest(foundPointOfInterest, newPointOfInerest);
        } else {
            pointOfInterest = handleCompletelyNewPointOfInterest(newPointOfInerest);
        }

        resolveAndFixParentSiteRef(pointOfInterest);

        return pointOfInterest;
    }

    private void resolveAndFixParentSiteRef(PointOfInterest pointOfInterest) {
        if (pointOfInterest != null && pointOfInterest.getParentSiteRef() != null) {
            DataManagedObjectStructure referencedStopPlace = referenceResolver.resolve(pointOfInterest.getParentSiteRef());
            pointOfInterest.getParentSiteRef().setRef(referencedStopPlace.getNetexId());
        }
    }


    public PointOfInterest handleCompletelyNewPointOfInterest(PointOfInterest incomingPointOfInterest) throws ExecutionException {
        logger.debug("New point of interest : {}. Setting version to \"1\"", incomingPointOfInterest.getName());
        incomingPointOfInterest = pointOfInterestVersionedSaverService.saveNewVersion(incomingPointOfInterest);
        return updateCache(incomingPointOfInterest);
    }

    public PointOfInterest handleAlreadyExistingPointOfInterest(PointOfInterest existingPointOfInterest, PointOfInterest incomingPointOfInterest) {
        logger.debug("Found existing poi {} from incoming {}", existingPointOfInterest, incomingPointOfInterest);

        PointOfInterest copyPointOfInterest = versionCreator.createCopy(existingPointOfInterest, PointOfInterest.class);
        String netexId = copyPointOfInterest.getNetexId();

        boolean keyValuesChanged = mergingUtils.updateKeyValues(copyPointOfInterest, incomingPointOfInterest, netexId);

        boolean nameChanged = mergingUtils.updateProperty(copyPointOfInterest.getName(), incomingPointOfInterest.getName(), copyPointOfInterest::setName, "name", netexId);
        boolean centroidChanged = mergingUtils.updateProperty(copyPointOfInterest.getCentroid(), incomingPointOfInterest.getCentroid(), copyPointOfInterest::setCentroid, "centroid", netexId);
        boolean allAreasWheelchairAccessibleChanged = mergingUtils.updateProperty(copyPointOfInterest.isAllAreasWheelchairAccessible(), incomingPointOfInterest.isAllAreasWheelchairAccessible(), copyPointOfInterest::setAllAreasWheelchairAccessible, "all areas wheelchair accessible", netexId);
        boolean operatorChanged = mergingUtils.updateProperty(copyPointOfInterest.getOperator(), incomingPointOfInterest.getOperator(), copyPointOfInterest::setOperator, "operator", netexId);

        boolean accessibilityAssessmentChanged = mergingUtils.updateAccessibilityAccessment(copyPointOfInterest, incomingPointOfInterest, netexId);

        boolean classificationsChanged = false;
        List<PointOfInterestClassification> pointOfInterestClassifications = new ArrayList<>();
        if (incomingPointOfInterest.getClassifications() != null && (!new HashSet<>(copyPointOfInterest.getClassifications()).containsAll(incomingPointOfInterest.getClassifications()) ||
                !new HashSet<>(incomingPointOfInterest.getClassifications()).containsAll(copyPointOfInterest.getClassifications()))) {

            copyPointOfInterest.getClassifications().clear();
            for (PointOfInterestClassification classification : incomingPointOfInterest.getClassifications()) {
                pointOfInterestClassifications.add(pointOfInterestClassificationVersionedSaverService.saveNewVersion(classification));
            }
            copyPointOfInterest.getClassifications().addAll(pointOfInterestClassifications);
            logger.info("Updated classification to {} for point of interest {}", copyPointOfInterest.getClassifications(), copyPointOfInterest);
            classificationsChanged = true;
        }

        if (keyValuesChanged || nameChanged || centroidChanged || allAreasWheelchairAccessibleChanged || operatorChanged ||
                accessibilityAssessmentChanged || classificationsChanged) {
            logger.info("Updated existing point of interest {}. ", copyPointOfInterest);
            copyPointOfInterest = pointOfInterestVersionedSaverService.saveNewVersion(copyPointOfInterest);
            return updateCache(copyPointOfInterest);
        }

        logger.debug("No changes. Returning existing point of interest {}", existingPointOfInterest);
        return existingPointOfInterest;

    }

    private PointOfInterest updateCache(PointOfInterest pointOfInterest) {
        poiFromOriginalIdFinder.update(pointOfInterest);
        nearbyPointOfInterestFinder.update(pointOfInterest);
        logger.info("Saved point of interest {}", pointOfInterest);
        return pointOfInterest;
    }


    private PointOfInterest findNearbyOrExistingPointOfInterest(PointOfInterest newPointOfInterest) {
        final PointOfInterest existingPointOfInterest = poiFromOriginalIdFinder.find(newPointOfInterest);
        if (existingPointOfInterest != null) {
            return existingPointOfInterest;
        }

        if (newPointOfInterest.getName() != null) {
            final PointOfInterest nearbyPointOfInterest = nearbyPointOfInterestFinder.find(newPointOfInterest);
            if (nearbyPointOfInterest != null) {
                logger.debug("Found nearby point of interest with name: {}, id: {}", nearbyPointOfInterest.getName(), nearbyPointOfInterest.getNetexId());
                return nearbyPointOfInterest;
            }
        }
        return null;
    }

}
