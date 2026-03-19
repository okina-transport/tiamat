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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.StringUtils;
import org.rutebanken.tiamat.importer.finder.NearbyPointOfInterestFinder;
import org.rutebanken.tiamat.importer.finder.PointOfInterestFromOriginalIdFinder;
import org.rutebanken.tiamat.model.*;
import org.rutebanken.tiamat.netex.mapping.NetexMapper;
import org.rutebanken.tiamat.repository.reference.ReferenceResolver;
import org.rutebanken.tiamat.versioning.save.PointOfInterestVersionedSaverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@Transactional
public class MergingPointOfInterestImporter {

    private static final Logger logger = LoggerFactory.getLogger(MergingPointOfInterestImporter.class);

    private final NetexMapper netexMapper;

    private final NearbyPointOfInterestFinder nearbyPointOfInterestFinder;

    private final PointOfInterestVersionedSaverService pointOfInterestVersionedSaverService;

    private final PointOfInterestFromOriginalIdFinder poiFromOriginalIdFinder;

    private final ReferenceResolver referenceResolver;

    @Autowired
    public MergingPointOfInterestImporter(PointOfInterestFromOriginalIdFinder poiFromOriginalIdFinder,
                                          NearbyPointOfInterestFinder nearbyParkingFinder,
                                          ReferenceResolver referenceResolver,
                                          NetexMapper netexMapper,
                                          PointOfInterestVersionedSaverService pointOfInterestVersionedSaverService) {
        this.poiFromOriginalIdFinder = poiFromOriginalIdFinder;
        this.nearbyPointOfInterestFinder = nearbyParkingFinder;
        this.referenceResolver = referenceResolver;
        this.netexMapper = netexMapper;
        this.pointOfInterestVersionedSaverService = pointOfInterestVersionedSaverService;
    }

    public org.rutebanken.netex.model.PointOfInterest importPointOfInterest(PointOfInterest pointOfInterest) {

        logger.debug("Transaction active: {}. Isolation level: {}", TransactionSynchronizationManager.isActualTransactionActive(), TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());

        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new RuntimeException("Transaction with required "
                    + "TransactionSynchronizationManager.isActualTransactionActive(): " + TransactionSynchronizationManager.isActualTransactionActive());
        }

        return netexMapper.mapToNetexModel(importPOIWithoutNetexMapping(pointOfInterest));
    }

    public PointOfInterest importPOIWithoutNetexMapping(PointOfInterest inputPointOfInterest) {
        final PointOfInterest databasePointOfInterest = findNearbyOrExistingPointOfInterest(inputPointOfInterest);

        final PointOfInterest pointOfInterest;
        if (databasePointOfInterest != null) {
            pointOfInterest = handleAlreadyExistingPointOfInterest(databasePointOfInterest, inputPointOfInterest);
        } else {
            pointOfInterest = handleCompletelyNewPointOfInterest(inputPointOfInterest);
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


    public PointOfInterest handleCompletelyNewPointOfInterest(PointOfInterest incomingPointOfInterest) {
        logger.debug("New point of interest : {}. Setting version to \"1\"", incomingPointOfInterest.getName());
        incomingPointOfInterest = pointOfInterestVersionedSaverService.saveNewVersion(incomingPointOfInterest);
        return updateCache(incomingPointOfInterest);
    }

    public PointOfInterest handleAlreadyExistingPointOfInterest(PointOfInterest existingPointOfInterest, PointOfInterest incomingPointOfInterest) {
        logger.debug("Found existing poi {} from incoming {}", existingPointOfInterest, incomingPointOfInterest);

        boolean keyValuesChanged = isKeyValuesUpdated(existingPointOfInterest, incomingPointOfInterest);

        String oldName = existingPointOfInterest.getName() != null ? existingPointOfInterest.getName().getValue() : null;
        String newName = incomingPointOfInterest.getName() != null ? incomingPointOfInterest.getName().getValue() : null;
        boolean nameChanged = !Objects.equals(oldName, newName);
        if (nameChanged) {
            existingPointOfInterest.setName(incomingPointOfInterest.getName());
        }

        boolean centroidChanged = !Objects.equals(
                existingPointOfInterest.getCentroid(),
                incomingPointOfInterest.getCentroid()
        );
        if (centroidChanged) {
            existingPointOfInterest.setCentroid(incomingPointOfInterest.getCentroid());
        }

        boolean allAreasWheelchairAccessibleChanged = !Objects.equals(existingPointOfInterest.isAllAreasWheelchairAccessible(), incomingPointOfInterest.isAllAreasWheelchairAccessible());
        if (allAreasWheelchairAccessibleChanged) {
            existingPointOfInterest.setAllAreasWheelchairAccessible(incomingPointOfInterest.isAllAreasWheelchairAccessible());
        }

        boolean operatorChanged = !StringUtils.equals(existingPointOfInterest.getOperator(), incomingPointOfInterest.getOperator());
        if (operatorChanged) {
            existingPointOfInterest.setOperator(incomingPointOfInterest.getOperator());
        }

        boolean accessibilityAssessmentChanged = isAccessibilityAssessmentUpdated(existingPointOfInterest, incomingPointOfInterest);

        boolean classificationsChanged = !CollectionUtils.isEqualCollection(incomingPointOfInterest.getClassifications(),existingPointOfInterest.getClassifications());
        if (classificationsChanged) {
            existingPointOfInterest.setClassifications(incomingPointOfInterest.getClassifications());
            logger.info("Updated classification to {} for point of interest {}", existingPointOfInterest.getClassifications(), existingPointOfInterest);
        }

        if (keyValuesChanged || nameChanged || centroidChanged || allAreasWheelchairAccessibleChanged || operatorChanged ||
                accessibilityAssessmentChanged || classificationsChanged) {
            logger.info("Updated existing point of interest {}. ", existingPointOfInterest);
            existingPointOfInterest = pointOfInterestVersionedSaverService.saveNewVersion(existingPointOfInterest);
            return updateCache(existingPointOfInterest);
        }

        logger.debug("No changes. Returning existing point of interest {}", existingPointOfInterest);
        return existingPointOfInterest;

    }

    private static boolean isKeyValuesUpdated(PointOfInterest existingPointOfInterest, PointOfInterest incomingPointOfInterest) {
        boolean keyValuesChanged;
        Map<String, Value> existingValueItems = existingPointOfInterest.getKeyValues();
        Map<String, Value> inputValueItems = incomingPointOfInterest.getKeyValues();
        boolean isEmptyExistingValueItems = MapUtils.isEmpty(existingValueItems);
        boolean isEmptyInputValueItems = MapUtils.isEmpty(inputValueItems);
        if (isEmptyExistingValueItems && isEmptyInputValueItems) {
            keyValuesChanged = false;
        } else if (isEmptyExistingValueItems || isEmptyInputValueItems) {
            keyValuesChanged = true;
            if (isEmptyExistingValueItems) {
                existingValueItems.putAll(inputValueItems);
            } else {
                inputValueItems.clear();
            }
        } else {
            boolean anyItemChanged = false;
            for (Map.Entry<String, Value> keyValueItem: inputValueItems.entrySet()) {
                String key = keyValueItem.getKey();
                Value newValueItemList = keyValueItem.getValue();
                if (existingValueItems.containsKey(key)) {
                    Value existingValueItemList = existingValueItems.get(key);
                    if (!SetUtils.isEqualSet(existingValueItemList.getItems(), newValueItemList.getItems())) {
                        anyItemChanged = true;
                        existingValueItems.put(key, newValueItemList);
                    }
                }
            }
            keyValuesChanged = anyItemChanged;
        }
        return keyValuesChanged;
    }

    private boolean isAccessibilityAssessmentUpdated(PointOfInterest existingPointOfInterest, PointOfInterest incomingPointOfInterest) {
        AccessibilityAssessment existingAccessibilityAssessment = existingPointOfInterest.getAccessibilityAssessment();
        AccessibilityAssessment inputAccessibilityAssessment = incomingPointOfInterest.getAccessibilityAssessment();
        if (existingAccessibilityAssessment == null && inputAccessibilityAssessment == null) {
            return false;
        }
        if (existingAccessibilityAssessment != null && inputAccessibilityAssessment == null) {
            existingPointOfInterest.setAccessibilityAssessment(incomingPointOfInterest.getAccessibilityAssessment());
            return true;
        }
        if (existingAccessibilityAssessment == null) {
            existingPointOfInterest.setAccessibilityAssessment(inputAccessibilityAssessment);
            return true;
        }
        boolean accessibilityAssessmentChanged = !Objects.equals(
                existingAccessibilityAssessment.getMobilityImpairedAccess(),
                inputAccessibilityAssessment.getMobilityImpairedAccess()
        ) && !Objects.equals(existingAccessibilityAssessment.getNetexId(), inputAccessibilityAssessment.getNetexId());

        if (accessibilityAssessmentChanged) {
            existingPointOfInterest.setAccessibilityAssessment(incomingPointOfInterest.getAccessibilityAssessment());
            existingAccessibilityAssessment.setVersion(existingPointOfInterest.getVersion() + 1);
        }
        if (isLimitationUpdated(existingAccessibilityAssessment.getLimitations(), inputAccessibilityAssessment.getLimitations())) {
            existingAccessibilityAssessment.setLimitations(inputAccessibilityAssessment.getLimitations());
            accessibilityAssessmentChanged = true;
        }

        return accessibilityAssessmentChanged;
    }

    private boolean isLimitationUpdated(List<AccessibilityLimitation> databaseLimitations, List<AccessibilityLimitation> inputLimitations) {
        if (CollectionUtils.isEmpty(databaseLimitations) && CollectionUtils.isEmpty(inputLimitations)) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(databaseLimitations) && CollectionUtils.isEmpty(inputLimitations)) {
            return true;
        }
        if (CollectionUtils.isEmpty(databaseLimitations)) {
            return true;
        }
        if (databaseLimitations.size() != inputLimitations.size()) {
            return true;
        }
        for (int i = 0; i < databaseLimitations.size(); i++) {
            AccessibilityLimitation accessibilityLimitation = databaseLimitations.get(i);
            AccessibilityLimitation newAcessibilityLimitation = inputLimitations.get(i);
            if (StringUtils.isBlank(newAcessibilityLimitation.getNetexId())) {
                newAcessibilityLimitation.setNetexId(accessibilityLimitation.getNetexId());
            }
            boolean updatedNetexId = !accessibilityLimitation.getNetexId().equals(newAcessibilityLimitation.getNetexId());
            boolean updatedWheelchairAccess = accessibilityLimitation.getWheelchairAccess() != newAcessibilityLimitation.getWheelchairAccess();
            boolean updatedStepFreeAccess = accessibilityLimitation.getStepFreeAccess() != newAcessibilityLimitation.getStepFreeAccess();
            boolean updatedEscalatorFreeAccess = accessibilityLimitation.getEscalatorFreeAccess() != newAcessibilityLimitation.getEscalatorFreeAccess();
            boolean updatedAudibleSignalsAvailable = accessibilityLimitation.getAudibleSignalsAvailable() != newAcessibilityLimitation.getAudibleSignalsAvailable();
            boolean updatedVisualSignsAvailable = accessibilityLimitation.getVisualSignsAvailable() != newAcessibilityLimitation.getVisualSignsAvailable();
            if (updatedNetexId || updatedWheelchairAccess || updatedStepFreeAccess || updatedEscalatorFreeAccess ||
                    updatedAudibleSignalsAvailable || updatedVisualSignsAvailable
            ) {
                return true;
            }
        }
        return false;
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
                logger.debug("Found nearby point of interest with name: {}, id: {}", nearbyPointOfInterest.getName(), nearbyPointOfInterest.getNetexId());
                return nearbyPointOfInterest;
            }
        }
        return null;
    }

}
