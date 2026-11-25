package org.rutebanken.tiamat.importer;

import org.entur.gbfs.http.GBFSHttpClient;
import org.entur.gbfs.mapper.GBFSMapperImpl;
import org.entur.gbfs.validation.GbfsValidatorFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.rutebanken.tiamat.model.job.AnalyzeImportError;
import org.rutebanken.tiamat.general.AnalyzeImportException;
import org.rutebanken.tiamat.general.MissingGbfsFeedException;
import org.rutebanken.tiamat.general.UnsupportedGbfsVersionException;
import org.rutebanken.tiamat.model.job.ImportWarnings;
import org.rutebanken.tiamat.model.job.AnalyzeImportErrorType;
import org.rutebanken.tiamat.model.job.JobError;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Checks, with the real GBFS validator, the blocking controls made on the gbfs.json file.
 */
class GbfsParkingImporterGbfsJsonCheckTest {

    private static final URI GBFS_URI = URI.create("http://mock-gbfs.test/gbfs.json");

    @ParameterizedTest
    @ValueSource(strings = {
            // GeoJSON: valid JSON, but not GBFS
            """
            {"type": "FeatureCollection", "features": [{"type": "Feature", "properties": {"id": "P1"},
             "geometry": {"type": "Point", "coordinates": [5.72, 45.19]}}]}
            """,
            "{}",
            "[]",
            "<html><body>Hello</body></html>",
            "",
            // GBFS 3.0 discovery file without its mandatory fields
            "{\"version\": \"3.0\"}"
    })
    void rejectsTargetWhichIsNotAGbfsJsonFile(String content) throws IOException {
        GbfsParkingImporter importer = importerServing(content);

        assertThatThrownBy(() -> importer.getGBFSParkingImportData(GBFS_URI))
                .isInstanceOfSatisfying(AnalyzeImportException.class, ex ->
                        assertThat(ex.getErrors())
                                .extracting(AnalyzeImportError::getType)
                                .containsExactly(AnalyzeImportErrorType.NOT_GBFS_FILE));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.1", "2.0", "2.1", "2.2"})
    void rejectsUnsupportedGbfsVersion(String version) throws IOException {
        GbfsParkingImporter importer = importerServing(gbfsJsonV1OrV2(version));

        assertThatThrownBy(() -> importer.getGBFSParkingImportData(GBFS_URI))
                .isInstanceOfSatisfying(UnsupportedGbfsVersionException.class, ex ->
                        assertThat(ex.toJobError()).isEqualTo(JobError.error(AnalyzeImportErrorType.UNSUPPORTED_GBFS_VERSION)
                                .withParam("version", version)
                                .withParam("supportedVersions", "2.3, 3.0")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.3", "3.0"})
    void acceptsSupportedGbfsVersion(String version) throws IOException {
        String gbfsJson = "3.0".equals(version) ? gbfsJsonV3() : gbfsJsonV1OrV2(version);
        GbfsParkingImporter importer = importerServing(gbfsJson);

        // The import fails further on (the feeds served are not valid), but not on the version control
        Throwable thrown = catchThrowable(() -> importer.getGBFSParkingImportData(GBFS_URI));

        assertThat(thrown).isNotInstanceOf(UnsupportedGbfsVersionException.class);
        if (thrown instanceof AnalyzeImportException analyzeImportException) {
            assertThat(analyzeImportException.getErrors())
                    .extracting(AnalyzeImportError::getType)
                    .doesNotContain(AnalyzeImportErrorType.NOT_GBFS_FILE, AnalyzeImportErrorType.UNSUPPORTED_GBFS_VERSION);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.3", "3.0"})
    void rejectsMissingRequiredFeedAndWarnsOnMissingPricingPlans(String version) throws IOException {
        // system_information and station_information only: vehicle_types and system_pricing_plans are missing
        String gbfsJson = "3.0".equals(version) ? gbfsJsonV3() : gbfsJsonV1OrV2(version);
        gbfsJson = gbfsJson.replaceAll(",\\s*\\{\"name\": \"vehicle_types\"[^}]*}", "");
        GbfsParkingImporter importer = importerServing(gbfsJson);

        ImportWarnings.start();
        Throwable thrown = catchThrowable(() -> importer.getGBFSParkingImportData(GBFS_URI));
        List<JobError> warnings = ImportWarnings.stop();

        assertThat(thrown).isInstanceOfSatisfying(MissingGbfsFeedException.class, ex ->
                assertThat(ex.toJobErrors()).containsExactly(
                        JobError.error(AnalyzeImportErrorType.MISSING_REQUIRED_GBFS_FEED).withParam("feed", "vehicle_types")));
        assertThat(warnings).containsExactly(
                JobError.warning(AnalyzeImportErrorType.MISSING_OPTIONAL_GBFS_FEED).withParam("feed", "system_pricing_plans"));
    }

    private static GbfsParkingImporter importerServing(String content) throws IOException {
        GBFSHttpClient httpClient = mock(GBFSHttpClient.class);
        when(httpClient.getData(any(URI.class)))
                .thenAnswer(invocation -> new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        return new GbfsParkingImporter(httpClient, new GBFSMapperImpl(),
                GbfsValidatorFactory.getGbfsJsonValidator(), null, null, null);
    }

    /**
     * Without vehicle_types, which only exists since GBFS 2.1: the file is valid for every version.
     */
    private static String gbfsJsonV1OrV2(String version) {
        return """
                {"last_updated": %d, "ttl": 0, "version": "%s",
                 "data": {"fr": {"feeds": [
                   {"name": "system_information", "url": "http://mock-gbfs.test/system_information.json"},
                   {"name": "station_information", "url": "http://mock-gbfs.test/station_information.json"}
                 ]}}}
                """.formatted(Instant.now().getEpochSecond(), version);
    }

    private static String gbfsJsonV3() {
        return """
                {"last_updated": "%s", "ttl": 0, "version": "3.0",
                 "data": {"feeds": [
                   {"name": "system_information", "url": "http://mock-gbfs.test/system_information.json"},
                   {"name": "station_information", "url": "http://mock-gbfs.test/station_information.json"},
                   {"name": "vehicle_types", "url": "http://mock-gbfs.test/vehicle_types.json"}
                 ]}}
                """.formatted(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
    }
}
