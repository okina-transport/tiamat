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

import org.rutebanken.tiamat.importer.KeyValueListAppender;
import org.rutebanken.tiamat.importer.finder.NearbyPointOfInterestFinder;
import org.rutebanken.tiamat.importer.finder.PointOfInterestFromOriginalIdFinder;
import org.rutebanken.tiamat.model.*;
import org.rutebanken.tiamat.netex.mapping.NetexMapper;
import org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Component
@Qualifier("mergingPoiImporter")
@Transactional
public class MergingPointOfInterestImporter {

    private static final Logger logger = LoggerFactory.getLogger(MergingPointOfInterestImporter.class);

    private final KeyValueListAppender keyValueListAppender;

    private final NetexMapper netexMapper;

    private final NearbyPointOfInterestFinder nearbyPointOfInterestFinder;

    private final PointOfInterestVersionedSaverService pointOfInterestVersionedSaverService;

    private final PointOfInterestClassificationVersionedSaverService pointOfInterestClassificationVersionedSaverService;

    private final AccessibilityVersionedSaverService accessibilityVersionedSaverService;

    private final PointOfInterestFromOriginalIdFinder poiFromOriginalIdFinder;

    private final ReferenceResolver referenceResolver;

    private final VersionCreator versionCreator;

    @Autowired
    public MergingPointOfInterestImporter(PointOfInterestFromOriginalIdFinder poiFromOriginalIdFinder,
                                          NearbyPointOfInterestFinder nearbyParkingFinder, ReferenceResolver referenceResolver,
                                          KeyValueListAppender keyValueListAppender, NetexMapper netexMapper,
                                          PointOfInterestVersionedSaverService pointOfInterestVersionedSaverService,
                                          PointOfInterestClassificationVersionedSaverService pointOfInterestClassificationVersionedSaverService,
                                          AccessibilityVersionedSaverService accessibilityVersionedSaverService,
                                          VersionCreator versionCreator) {
        this.poiFromOriginalIdFinder = poiFromOriginalIdFinder;
        this.nearbyPointOfInterestFinder = nearbyParkingFinder;
        this.referenceResolver = referenceResolver;
        this.keyValueListAppender = keyValueListAppender;
        this.netexMapper = netexMapper;
        this.pointOfInterestVersionedSaverService = pointOfInterestVersionedSaverService;
        this.pointOfInterestClassificationVersionedSaverService = pointOfInterestClassificationVersionedSaverService;
        this.accessibilityVersionedSaverService = accessibilityVersionedSaverService;
        this.versionCreator = versionCreator;
    }

    public org.rutebanken.netex.model.PointOfInterest importPointOfInterest(PointOfInterest pointOfInterest) throws InterruptedException, ExecutionException {

        logger.debug("Transaction active: {}. Isolation level: {}", TransactionSynchronizationManager.isActualTransactionActive(), TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());

        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new RuntimeException("Transaction with required "
                    + "TransactionSynchronizationManager.isActualTransactionActive(): " + TransactionSynchronizationManager.isActualTransactionActive());
        }

        return netexMapper.mapToNetexModel(importParkingWithoutNetexMapping(pointOfInterest));
    }

    public PointOfInterest importParkingWithoutNetexMapping(PointOfInterest newPointOfInerest) throws InterruptedException, ExecutionException {
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

        boolean keyValuesChanged = keyValueListAppender.appendToOriginalId(NetexIdMapper.ORIGINAL_ID_KEY, incomingPointOfInterest, copyPointOfInterest);
        boolean centroidChanged = (copyPointOfInterest.getCentroid() != null && incomingPointOfInterest.getCentroid() != null && !copyPointOfInterest.getCentroid().equals(incomingPointOfInterest.getCentroid()));

        boolean allAreasWheelchairAccessibleChanged = false;
        if ((copyPointOfInterest.isAllAreasWheelchairAccessible() == null && incomingPointOfInterest.isAllAreasWheelchairAccessible() != null) ||
                (copyPointOfInterest.isAllAreasWheelchairAccessible() != null && incomingPointOfInterest.isAllAreasWheelchairAccessible() != null
                        && !copyPointOfInterest.isAllAreasWheelchairAccessible().equals(incomingPointOfInterest.isAllAreasWheelchairAccessible()))) {

            copyPointOfInterest.setAllAreasWheelchairAccessible(incomingPointOfInterest.isAllAreasWheelchairAccessible());
            logger.info("Updated allAreasWheelchairAccessible value to {} for point of interest {}", copyPointOfInterest.isAllAreasWheelchairAccessible(), copyPointOfInterest);
            allAreasWheelchairAccessibleChanged = true;
        }

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

        boolean accessibilityAssessmentChanged = false;
        if (incomingPointOfInterest.getAccessibilityAssessment() != null && copyPointOfInterest.getAccessibilityAssessment() != null &&
                !incomingPointOfInterest.getAccessibilityAssessment().equals(copyPointOfInterest.getAccessibilityAssessment())) {
            copyPointOfInterest.setAccessibilityAssessment(accessibilityVersionedSaverService.saveNewVersionAssessment(incomingPointOfInterest.getAccessibilityAssessment()));
            logger.info("Updated accessibility assessment to {} for point of interest {}", copyPointOfInterest.getAccessibilityAssessment(), copyPointOfInterest);
            accessibilityAssessmentChanged = true;
        }

        boolean accessibilityLimitationsChanged = false;
        List<AccessibilityLimitation> accessibilityLimitations = new ArrayList<>();
        if (incomingPointOfInterest.getAccessibilityAssessment().getLimitations() != null && copyPointOfInterest.getAccessibilityAssessment().getLimitations() != null &&
                !incomingPointOfInterest.getAccessibilityAssessment().getLimitations().equals(copyPointOfInterest.getAccessibilityAssessment().getLimitations())) {

            copyPointOfInterest.getAccessibilityAssessment().getLimitations().clear();
            for (AccessibilityLimitation limitation : incomingPointOfInterest.getAccessibilityAssessment().getLimitations()) {
                accessibilityLimitations.add(accessibilityVersionedSaverService.saveNewVersionLimitation(limitation));
            }
            copyPointOfInterest.getAccessibilityAssessment().getLimitations().addAll(accessibilityLimitations);
            logger.info("Updated accessibility limitations to {} for point of interest {}", copyPointOfInterest.getAccessibilityAssessment().getLimitations(), copyPointOfInterest);
            accessibilityLimitationsChanged = true;
        }

        boolean nameChanged = false;
        if (!copyPointOfInterest.getName().equals(incomingPointOfInterest.getName())) {
            copyPointOfInterest.setName(incomingPointOfInterest.getName());
            nameChanged = true;
        }

        if (keyValuesChanged || centroidChanged || nameChanged || allAreasWheelchairAccessibleChanged  || classificationsChanged ||
                accessibilityAssessmentChanged || accessibilityLimitationsChanged) {
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
