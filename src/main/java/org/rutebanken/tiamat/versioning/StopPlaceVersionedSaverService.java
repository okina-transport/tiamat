package org.rutebanken.tiamat.versioning;

import org.rutebanken.tiamat.model.*;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.rutebanken.tiamat.versioning.util.AccessibilityAssessmentOptimizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;


@Transactional
@Service
public class StopPlaceVersionedSaverService {

    @Autowired
    private StopPlaceRepository stopPlaceRepository;

    @Autowired
    private VersionCreator versionCreator;

    @Autowired
    private AccessibilityAssessmentOptimizer accessibilityAssessmentOptimizer;


    public StopPlace createNewVersion(StopPlace stopPlace) {

        if (stopPlace.getNetexId() == null) {
            stopPlace.setCreated(ZonedDateTime.now());
        } else {
            stopPlace.setChanged(ZonedDateTime.now());
        }
        return versionCreator.createNextVersion(stopPlace);
    }


    public StopPlace saveNewVersion(StopPlace newVersion) {
        return saveNewVersion(null, newVersion);
    }

    public StopPlace saveNewVersion(StopPlace existingVersion, StopPlace newVersion) {

        if (existingVersion == newVersion) {
            throw new IllegalArgumentException("Existing and new StopPlace must be different objects");
        }

        // Rearrange AccessibilityAssessment if necessary
        accessibilityAssessmentOptimizer.optimizeAccessibilityAssessments(newVersion);

        StopPlace stopPlaceToSave;
        if (existingVersion == null) {
            stopPlaceToSave = versionCreator.initiateFirstVersionWithAvailabilityCondition(newVersion);
            stopPlaceToSave.setCreated(ZonedDateTime.now());
        } else if (existingVersion.getVersion() == newVersion.getVersion() |
                !existingVersion.getNetexId().equals(newVersion.getNetexId())) {

            throw new IllegalArgumentException("Existing and new StopPlace do not match");
        } else {

            stopPlaceToSave = newVersion;
            stopPlaceToSave.setChanged(ZonedDateTime.now());

            // TODO: Add support for "valid from/to" being explicitly set

            // Invalidate existing version
            StopPlace existingStopPlace = stopPlaceRepository.findFirstByNetexIdAndVersion(existingVersion.getNetexId(), existingVersion.getVersion());
            existingStopPlace = versionCreator.terminateVersion(existingStopPlace, ZonedDateTime.now());

            // Update previous version
            stopPlaceRepository.save(existingStopPlace);
        }

        if (stopPlaceToSave.getQuays() != null) {
            stopPlaceToSave.getQuays().forEach(quay -> {
                if (quay.getNetexId() == null) {
                    quay = versionCreator.initiateFirstVersionWithAvailabilityCondition(quay, Quay.class);
                }
                AccessibilityAssessment accessibilityAssessment = quay.getAccessibilityAssessment();

                if (accessibilityAssessment != null) {
                    if (accessibilityAssessment.getNetexId() == null) {
                        accessibilityAssessment = versionCreator.initiateFirstVersionWithAvailabilityCondition(accessibilityAssessment, AccessibilityAssessment.class);
                    }
                    if (accessibilityAssessment.getLimitations() != null && !accessibilityAssessment.getLimitations().isEmpty()) {
                        AccessibilityLimitation limitation = accessibilityAssessment.getLimitations().get(0);
                        if (limitation.getNetexId() == null) {
                            limitation = versionCreator.initiateFirstVersionWithAvailabilityCondition(limitation, AccessibilityLimitation.class);
                        }
                    }
                }
                if (quay.getPlaceEquipments() != null) {
                    if (quay.getPlaceEquipments().getNetexId() == null) {
                        quay.setPlaceEquipments(versionCreator.initiateFirstVersionWithAvailabilityCondition(quay.getPlaceEquipments(), PlaceEquipment.class));
                    }
                    if (quay.getPlaceEquipments().getInstalledEquipment() != null) {
                        quay.getPlaceEquipments().getInstalledEquipment().forEach(installed -> {
                            if (installed.getNetexId() == null) {
                                installed = versionCreator.initiateFirstVersionWithAvailabilityCondition(installed, installed.getClass());
                            }
                        });
                    }
                }

            });
            if (stopPlaceToSave.getPlaceEquipments() != null) {
                if (stopPlaceToSave.getPlaceEquipments().getNetexId() == null) {
                    stopPlaceToSave.setPlaceEquipments(versionCreator.initiateFirstVersionWithAvailabilityCondition(stopPlaceToSave.getPlaceEquipments(), PlaceEquipment.class));
                }
                if (stopPlaceToSave.getPlaceEquipments().getInstalledEquipment() != null) {
                    stopPlaceToSave.getPlaceEquipments().getInstalledEquipment().forEach(installed -> {
                        if (installed.getNetexId() == null) {
                            installed = versionCreator.initiateFirstVersionWithAvailabilityCondition(installed, installed.getClass());
                        }
                    });
                }
            }
        }
        // Save latest version
        return stopPlaceRepository.save(stopPlaceToSave);
    }
}
