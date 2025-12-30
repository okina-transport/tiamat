package org.rutebanken.tiamat.repository.search;

import org.junit.jupiter.api.Test;
import org.rutebanken.tiamat.exporter.params.ExportParams;
import org.rutebanken.tiamat.exporter.params.StopPlaceSearch;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ExportParamsAndStopPlaceSearchValidatorTest {

    private ExportParamsAndStopPlaceSearchValidator validator = new ExportParamsAndStopPlaceSearchValidator();

    @Test
    public void versionAndPointInTimeCannotBeCombined() {

        assertThrows(IllegalArgumentException.class, () -> {
            ExportParams exportParams = ExportParams.newExportParamsBuilder()
                    .setStopPlaceSearch(StopPlaceSearch.newStopPlaceSearchBuilder()
                            .setVersion(1L)
                            .setPointInTime(Instant.now())
                            .build())
                    .build();

            validator.validateExportParams(exportParams);

                });

    }

    @Test
    public void versionValidityAndPointInTimeCannotBeCombined() {

        assertThrows(IllegalArgumentException.class, () -> {
            ExportParams exportParams = ExportParams.newExportParamsBuilder()
                    .setStopPlaceSearch(StopPlaceSearch.newStopPlaceSearchBuilder()
                            .setVersionValidity(ExportParams.VersionValidity.CURRENT_FUTURE)
                            .setPointInTime(Instant.now())
                            .build())
                    .build();

            validator.validateExportParams(exportParams);

                });

    }

    @Test
    public void versionValidityAndAllversionsCannotBeCombined() {

        assertThrows(IllegalArgumentException.class, () -> {
            ExportParams exportParams = ExportParams.newExportParamsBuilder()
                    .setStopPlaceSearch(StopPlaceSearch.newStopPlaceSearchBuilder()
                            .setVersionValidity(ExportParams.VersionValidity.CURRENT_FUTURE)
                            .setAllVersions(true)
                            .build())
                    .build();

            validator.validateExportParams(exportParams);

                });

    }

}