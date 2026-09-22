package org.rutebanken.tiamat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rutebanken.tiamat.service.job.JobService;
import org.rutebanken.tiamat.service.stopplace.ExportFileSummary;

import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class BlobStoreServiceTest {

    private static final String EUROPE_PARIS = "Europe/Paris";
    private BlobStoreService blobStoreService;

    @Mock
    private JobService jobService;

    @BeforeEach()
    void init() {
        blobStoreService = new BlobStoreService("", "", "", "", jobService);
    }

    @ParameterizedTest
    @CsvSource({
            "POI_12_technique_T_20260415T144855Z.zip",
            "PARKING_12_technique_T_20260415T070852Z.zip.zip",
            "PARKING_12_technique_T_20260415T070852Z.zip",
            "PARKING_12_technique_T_20260415T070852Z.zip"})
    void keyNotFoundGenerateExportFileSummaryTest(String key) {
        ExportFileSummary exportFileSummary = blobStoreService.generateExportFileSummary("", key);

        assertThat(exportFileSummary).isNotNull();
        assertThat(exportFileSummary.getUserName()).isEqualTo("Mobi-iti");
        assertThat(exportFileSummary.getStartDate()).isNotNull();
        Instant instant = exportFileSummary.getStartDate();
        int year = instant.atZone(ZoneId.of(EUROPE_PARIS)).getYear();
        int month = instant.atZone(ZoneId.of(EUROPE_PARIS)).getMonthValue();
        assertThat(year).isEqualTo(2026);
        assertThat(month).isEqualTo(4);
    }

}