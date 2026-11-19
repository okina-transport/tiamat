package org.rutebanken.tiamat.service.parking;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.model.job.AnalyzeImportErrorType;
import org.rutebanken.tiamat.model.job.ImportWarnings;
import org.rutebanken.tiamat.model.job.JobError;

import static org.assertj.core.api.Assertions.assertThat;

class ParkingAddressTruncationTest {

    @AfterEach
    void stopWarnings() {
        ImportWarnings.stop();
    }

    @Test
    void keepsAddressUpToMaxLength() {
        ImportWarnings.start();
        Parking parking = new Parking();
        parking.setOriginalId("P1");
        String address = "a".repeat(Parking.ADDRESS_MAX_LENGTH);

        parking.setAddress(address);

        ParkingsImportedService.truncateAddress(parking);

        assertThat(parking.getAddress()).isEqualTo(address);
        assertThat(ImportWarnings.stop()).isEmpty();
    }

    @Test
    void truncatesLongAddressAndRaisesWarning() {
        ImportWarnings.start();
        Parking parking = new Parking();
        parking.setOriginalId("P1");

        parking.setAddress("a".repeat(1500));

        ParkingsImportedService.truncateAddress(parking);

        assertThat(parking.getAddress()).hasSize(Parking.ADDRESS_MAX_LENGTH);
        assertThat(ImportWarnings.stop()).containsExactly(JobError.warning(AnalyzeImportErrorType.FIELD_TRUNCATED)
                .withParam("field", "address")
                .withParam("entityId", "P1")
                .withParam("originalLength", "1500")
                .withParam("maxLength", "1000"));
    }

    @Test
    void countsAndCutsOnCodePoints() {
        ImportWarnings.start();
        Parking parking = new Parking();
        parking.setOriginalId("P1");
        // 999 characters followed by emojis, each one is a surrogate pair (2 Java chars, 1 character in database)
        String address = "a".repeat(999) + "🚗".repeat(2);

        parking.setAddress(address);

        ParkingsImportedService.truncateAddress(parking);

        assertThat(parking.getAddress()).isEqualTo("a".repeat(999) + "🚗");
        assertThat(ImportWarnings.stop()).extracting(warning -> warning.getParams().get("originalLength"))
                .containsExactly("1001");
    }

    @Test
    void truncatesWithoutWarningOutsideAnImportJob() {
        Parking parking = new Parking();

        parking.setAddress("a".repeat(1500));

        ParkingsImportedService.truncateAddress(parking);

        assertThat(parking.getAddress()).hasSize(Parking.ADDRESS_MAX_LENGTH);
        assertThat(ImportWarnings.stop()).isEmpty();
    }
}
