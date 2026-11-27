package org.rutebanken.tiamat.versioning.save;

import org.junit.jupiter.api.Test;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.rutebanken.tiamat.model.AccessibilityAssessment;
import org.rutebanken.tiamat.model.LimitationStatusEnumeration;
import org.rutebanken.tiamat.model.ParkingArea;
import org.rutebanken.tiamat.repository.AccessibilityAssessmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ParkingAreasVersionedSaverServiceTest extends TiamatIntegrationTest {

    @Autowired
    private AccessibilityAssessmentRepository accessibilityAssessmentRepository;

    @Autowired
    private ParkingAreasVersionedSaverService parkingAreasVersionedSaverService;

    @Test
    public void saveParkingAreaWithAccessibilityAssessmentAlreadyInDatabase() {
        AccessibilityAssessment existingAssessment = new AccessibilityAssessment();
        existingAssessment.setVersion(1L);
        existingAssessment.setMobilityImpairedAccess(LimitationStatusEnumeration.TRUE);
        existingAssessment = accessibilityAssessmentRepository.save(existingAssessment);
        String assessmentNetexId = existingAssessment.getNetexId();

        AccessibilityAssessment importedAssessment = new AccessibilityAssessment();
        importedAssessment.setNetexId(assessmentNetexId);
        importedAssessment.setVersion(1L);
        importedAssessment.setMobilityImpairedAccess(LimitationStatusEnumeration.TRUE);

        ParkingArea parkingArea = new ParkingArea();
        parkingArea.setAccessibilityAssessment(importedAssessment);

        ParkingArea actual = parkingAreasVersionedSaverService.saveNewVersion(parkingArea);

        assertThat(actual.getAccessibilityAssessment().getNetexId()).isEqualTo(assessmentNetexId);
        assertThat(actual.getAccessibilityAssessment().getVersion()).isEqualTo(2L);
        assertThat(accessibilityAssessmentRepository.findByNetexId(assessmentNetexId)).hasSize(2);
    }
}
