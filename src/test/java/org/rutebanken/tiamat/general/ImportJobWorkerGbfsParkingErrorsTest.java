package org.rutebanken.tiamat.general;

import org.entur.gbfs.validation.model.FileValidationError;
import org.entur.gbfs.validation.model.FileValidationResult;
import org.entur.gbfs.validation.model.ValidationResult;
import org.entur.gbfs.validation.model.ValidationSummary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.rutebanken.tiamat.model.ParkingTypeEnumeration;
import org.rutebanken.tiamat.model.SpecificParkingAreaUsageEnumeration;
import org.rutebanken.tiamat.model.gbfs.GbfsParkingImportParams;
import org.rutebanken.tiamat.model.job.*;
import org.rutebanken.tiamat.externalapis.gbfs.GbfsHttpStatusException;
import org.rutebanken.tiamat.repository.JobRepository;
import org.rutebanken.tiamat.repository.OrganisationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ImportJobWorkerGbfsParkingErrorsTest extends TiamatIntegrationTest {

    private static final String GLOBAL_URL = "http://mock-gbfs.test/gbfs.json";

    private static final String VALID_DISCOVERY_JSON = """
            {
              "last_updated": "2025-01-01T00:00:00Z",
              "ttl": 60,
              "version": "3.0",
              "data": {
                "feeds": [
                  { "name": "system_information", "url": "http://mock-gbfs.test/system_information.json" },
                  { "name": "station_information", "url": "http://mock-gbfs.test/station_information.json" },
                  { "name": "vehicle_types", "url": "http://mock-gbfs.test/vehicle_types.json" },
                  { "name": "system_pricing_plans", "url": "http://mock-gbfs.test/system_pricing_plans.json" }
                ]
              }
            }
            """;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ImportJobWorkerBuilder importJobWorkerBuilder;

    @Autowired
    private OrganisationRepository organisationRepository;

    @BeforeEach
    void bindRequestScope() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void unbindRequestScope() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void populatesStructuredErrorsOnEncodingFailure() throws IOException {
        byte[] invalidUtf8Bytes = {(byte) 0xFF, (byte) 0xFE, (byte) 0xFD};
        when(gbfsHttpClient.getData(any(URI.class))).thenAnswer(invocation -> new ByteArrayInputStream(invalidUtf8Bytes));

        Job job = runGbfsParkingImport();

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(JobError.error(AnalyzeImportErrorType.ENCODING).withParam("file", "gbfs.json"));
    }

    @Test
    void populatesNotGbfsFileErrorWhenTargetIsNotAGbfsJsonFile() throws IOException {
        byte[] notAGbfsJsonFile = "{}".getBytes(StandardCharsets.UTF_8);
        when(gbfsHttpClient.getData(any(URI.class))).thenAnswer(invocation -> new ByteArrayInputStream(notAGbfsJsonFile));
        when(gbfsValidator.validate(anyMap())).thenReturn(new ValidationResult(new ValidationSummary(null, 0L, 0), Map.of()));

        Job job = runGbfsParkingImport();

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).extracting(JobError::getCode).containsExactly(AnalyzeImportErrorType.NOT_GBFS_FILE);
    }

    @Test
    void populatesStructuredErrorsOnMissingRequiredData() throws IOException {
        byte[] placeholderBytes = "{}".getBytes(StandardCharsets.UTF_8);
        when(gbfsHttpClient.getData(any(URI.class))).thenAnswer(invocation -> new ByteArrayInputStream(placeholderBytes));

        FileValidationResult discoveryFile = new FileValidationResult(
                "gbfs", true, true, 0, null, VALID_DISCOVERY_JSON, "3.0", List.of());
        ValidationResult discoveryValidation = new ValidationResult(new ValidationSummary("3.0", 0L, 0), Map.of("gbfs", discoveryFile));

        FileValidationResult stationInformationWithMissingData = new FileValidationResult(
                "station_information", true, true, 1, null, null, "3.0",
                List.of(new FileValidationError("#/data/stations/0", "capacity", "capacity is required")));
        ValidationResult compositeValidation = new ValidationResult(
                new ValidationSummary("3.0", 0L, 1), Map.of("station_information", stationInformationWithMissingData));

        when(gbfsValidator.validate(anyMap())).thenReturn(discoveryValidation, compositeValidation);

        Job job = runGbfsParkingImport();

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).extracting(JobError::getCode).containsExactly(AnalyzeImportErrorType.MISSING_DATA);
    }

    @Test
    void cancelsImportWhenGbfsJsonAnswers503() throws IOException {
        when(gbfsHttpClient.getData(any(URI.class))).thenThrow(new GbfsHttpStatusException(503, URI.create(GLOBAL_URL)));

        long parkingsBefore = parkingRepository.count();
        long organisationsBefore = organisationRepository.count();

        Job job = runGbfsParkingImport();

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(JobError.error(AnalyzeImportErrorType.HTTP_ERROR)
                .withParam("file", "gbfs.json")
                .withParam("url", GLOBAL_URL)
                .withParam("httpStatus", "503"));
        assertNothingImported(parkingsBefore, organisationsBefore);
    }

    @Test
    void cancelsImportWhenGbfsServerCannotBeReached() throws IOException {
        when(gbfsHttpClient.getData(any(URI.class))).thenThrow(new java.net.ConnectException("Connection refused"));

        long parkingsBefore = parkingRepository.count();
        long organisationsBefore = organisationRepository.count();

        Job job = runGbfsParkingImport();

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(JobError.error(AnalyzeImportErrorType.HTTP_ERROR)
                .withParam("file", "gbfs.json")
                .withParam("url", GLOBAL_URL));
        assertNothingImported(parkingsBefore, organisationsBefore);
    }

    @Test
    void cancelsImportWhenGbfsServerSendsNoContent() throws IOException {
        when(gbfsHttpClient.getData(any(URI.class))).thenReturn(null);

        long parkingsBefore = parkingRepository.count();
        long organisationsBefore = organisationRepository.count();

        Job job = runGbfsParkingImport();

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(JobError.error(AnalyzeImportErrorType.HTTP_ERROR)
                .withParam("file", "gbfs.json")
                .withParam("url", GLOBAL_URL));
        assertNothingImported(parkingsBefore, organisationsBefore);
    }

    @Test
    void cancelsImportWhenAFeedAnswers503() throws IOException {
        String stationInformationUrl = "http://mock-gbfs.test/station_information.json";
        when(gbfsHttpClient.getData(any(URI.class))).thenAnswer(invocation -> {
            URI uri = invocation.getArgument(0);
            if (uri.toString().equals(stationInformationUrl)) {
                throw new GbfsHttpStatusException(503, uri);
            }
            return new ByteArrayInputStream(VALID_DISCOVERY_JSON.getBytes(StandardCharsets.UTF_8));
        });
        FileValidationResult discoveryFile = new FileValidationResult(
                "gbfs", true, true, 0, null, VALID_DISCOVERY_JSON, "3.0", List.of());
        when(gbfsValidator.validate(anyMap()))
                .thenReturn(new ValidationResult(new ValidationSummary("3.0", 0L, 0), Map.of("gbfs", discoveryFile)));

        long parkingsBefore = parkingRepository.count();
        long organisationsBefore = organisationRepository.count();

        Job job = runGbfsParkingImport();

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(JobError.error(AnalyzeImportErrorType.HTTP_ERROR)
                .withParam("file", "station_information.json")
                .withParam("url", stationInformationUrl)
                .withParam("httpStatus", "503"));
        assertNothingImported(parkingsBefore, organisationsBefore);
    }

    @Test
    void cancelsImportWhenGbfsVersionIsNotSupported() throws IOException {
        when(gbfsHttpClient.getData(any(URI.class))).thenAnswer(invocation -> new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)));
        FileValidationResult discoveryFileV22 = new FileValidationResult(
                "gbfs", true, true, 0, null, "{}", "2.2", List.of());
        when(gbfsValidator.validate(anyMap()))
                .thenReturn(new ValidationResult(new ValidationSummary("2.2", 0L, 0), Map.of("gbfs", discoveryFileV22)));

        long parkingsBefore = parkingRepository.count();
        long organisationsBefore = organisationRepository.count();

        Job job = runGbfsParkingImport();

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(JobError.error(AnalyzeImportErrorType.UNSUPPORTED_GBFS_VERSION)
                .withParam("version", "2.2")
                .withParam("supportedVersions", "2.3, 3.0"));
        assertNothingImported(parkingsBefore, organisationsBefore);
    }

    @Test
    void cancelsImportWhenRequiredFeedsAreMissing() throws IOException {
        String discoveryWithoutStationInformationAndVehicleTypes = """
                {
                  "last_updated": "2025-01-01T00:00:00Z",
                  "ttl": 60,
                  "version": "3.0",
                  "data": {
                    "feeds": [
                      { "name": "system_information", "url": "http://mock-gbfs.test/system_information.json" },
                      { "name": "system_pricing_plans", "url": "http://mock-gbfs.test/system_pricing_plans.json" }
                    ]
                  }
                }
                """;
        mockDiscovery(discoveryWithoutStationInformationAndVehicleTypes);

        long parkingsBefore = parkingRepository.count();
        long organisationsBefore = organisationRepository.count();

        Job job = runGbfsParkingImport();

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(
                JobError.error(AnalyzeImportErrorType.MISSING_REQUIRED_GBFS_FEED).withParam("feed", "station_information"),
                JobError.error(AnalyzeImportErrorType.MISSING_REQUIRED_GBFS_FEED).withParam("feed", "vehicle_types"));
        assertNothingImported(parkingsBefore, organisationsBefore);
    }

    @Test
    void reportsWarningWhenSystemPricingPlansIsMissing() throws IOException {
        String discoveryWithoutPricingPlans = """
                {
                  "last_updated": "2025-01-01T00:00:00Z",
                  "ttl": 60,
                  "version": "3.0",
                  "data": {
                    "feeds": [
                      { "name": "system_information", "url": "http://mock-gbfs.test/system_information.json" },
                      { "name": "station_information", "url": "http://mock-gbfs.test/station_information.json" },
                      { "name": "vehicle_types", "url": "http://mock-gbfs.test/vehicle_types.json" }
                    ]
                  }
                }
                """;
        mockDiscovery(discoveryWithoutPricingPlans);
        FileValidationResult stationInformationWithMissingData = new FileValidationResult(
                "station_information", true, true, 1, null, null, "3.0",
                List.of(new FileValidationError("#/data/stations/0", "capacity", "capacity is required")));
        FileValidationResult discoveryFile = new FileValidationResult(
                "gbfs", true, true, 0, null, discoveryWithoutPricingPlans, "3.0", List.of());
        when(gbfsValidator.validate(anyMap())).thenReturn(
                new ValidationResult(new ValidationSummary("3.0", 0L, 0), Map.of("gbfs", discoveryFile)),
                new ValidationResult(new ValidationSummary("3.0", 0L, 1), Map.of("station_information", stationInformationWithMissingData)));

        Job job = runGbfsParkingImport();

        // the warning is reported after the errors, whatever the status of the job
        assertThat(job.getErrors()).containsExactly(
                JobError.error(AnalyzeImportErrorType.MISSING_DATA),
                JobError.warning(AnalyzeImportErrorType.MISSING_OPTIONAL_GBFS_FEED).withParam("feed", "system_pricing_plans"));
    }

    @Test
    void cancelsImportWhenAFeedIsNotValidUtf8() throws IOException {
        mockDiscovery(VALID_DISCOVERY_JSON);
        byte[] latin1StationInformation = "{\"name\": \"Hôtel de Ville\"}".getBytes(StandardCharsets.ISO_8859_1);
        when(gbfsHttpClient.getData(any(URI.class))).thenAnswer(invocation -> {
            URI uri = invocation.getArgument(0);
            return uri.toString().endsWith("/station_information.json")
                    ? new ByteArrayInputStream(latin1StationInformation)
                    : new ByteArrayInputStream(VALID_DISCOVERY_JSON.getBytes(StandardCharsets.UTF_8));
        });

        long parkingsBefore = parkingRepository.count();
        long organisationsBefore = organisationRepository.count();

        Job job = runGbfsParkingImport();

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(
                JobError.error(AnalyzeImportErrorType.ENCODING).withParam("file", "station_information.json"));
        assertNothingImported(parkingsBefore, organisationsBefore);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"last_updated\": \"2025-01-01T00:00:00Z\", \"data\": {",
            "<html><body>Maintenance</body></html>",
            "",
            "[]",
            "{\"data\": {}} trailing garbage"
    })
    void cancelsImportWhenAFeedIsNotReadableJson(String systemInformationContent) throws IOException {
        mockDiscovery(VALID_DISCOVERY_JSON);
        when(gbfsHttpClient.getData(any(URI.class))).thenAnswer(invocation -> {
            URI uri = invocation.getArgument(0);
            String content = uri.toString().endsWith("/system_information.json") ? systemInformationContent : VALID_DISCOVERY_JSON;
            return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        });

        long parkingsBefore = parkingRepository.count();
        long organisationsBefore = organisationRepository.count();

        Job job = runGbfsParkingImport();

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(
                JobError.error(AnalyzeImportErrorType.INVALID_JSON).withParam("file", "system_information.json"));
        assertNothingImported(parkingsBefore, organisationsBefore);
    }

    @Test
    void cancelsImportWhenRequiredFieldsAreMissing() throws IOException {
        String stationInformation = """
                {"data": {"stations": [
                  {"station_id": "41", "name": "A", "lat": 45.1, "lon": 5.7},
                  {"station_id": "42", "name": "B", "lon": 5.7},
                  {"name": "C", "lat": 45.1, "lon": 5.7}
                ]}}
                """;
        mockDiscovery(VALID_DISCOVERY_JSON);
        when(gbfsHttpClient.getData(any(URI.class))).thenAnswer(invocation -> {
            URI uri = invocation.getArgument(0);
            String content = uri.toString().endsWith("/station_information.json") ? stationInformation : VALID_DISCOVERY_JSON;
            return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        });
        FileValidationResult discoveryFile = new FileValidationResult(
                "gbfs", true, true, 0, null, VALID_DISCOVERY_JSON, "3.0", List.of());
        FileValidationResult stationInformationValidation = new FileValidationResult(
                "station_information", true, true, 2, null, null, "3.0", List.of(
                new FileValidationError("#/properties/data/properties/stations/items", "#/data/stations/1",
                        "#/data/stations/1: required key [lat] not found"),
                new FileValidationError("#/properties/data/properties/stations/items", "#/data/stations/2",
                        "#/data/stations/2: required key [station_id] not found")));
        FileValidationResult systemInformationValidation = new FileValidationResult(
                "system_information", true, true, 1, null, null, "3.0", List.of(
                new FileValidationError("#/properties/data", "#/data", "#/data: required key [timezone] not found")));
        when(gbfsValidator.validate(anyMap())).thenReturn(
                new ValidationResult(new ValidationSummary("3.0", 0L, 0), Map.of("gbfs", discoveryFile)),
                new ValidationResult(new ValidationSummary("3.0", 0L, 3), Map.of(
                        "station_information", stationInformationValidation,
                        "system_information", systemInformationValidation)));

        long parkingsBefore = parkingRepository.count();
        long organisationsBefore = organisationRepository.count();

        Job job = runGbfsParkingImport();

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(
                JobError.error(AnalyzeImportErrorType.MISSING_REQUIRED_FIELD)
                        .withParam("file", "station_information.json")
                        .withParam("entity", "station")
                        .withParam("entityId", "42")
                        .withParam("field", "lat"),
                JobError.error(AnalyzeImportErrorType.MISSING_REQUIRED_FIELD)
                        .withParam("file", "station_information.json")
                        .withParam("entity", "station")
                        .withParam("position", "3")
                        .withParam("field", "station_id"),
                JobError.error(AnalyzeImportErrorType.MISSING_REQUIRED_FIELD)
                        .withParam("file", "system_information.json")
                        .withParam("field", "timezone"));
        assertNothingImported(parkingsBefore, organisationsBefore);
    }

    @Test
    void cancelsImportWhenNumericFieldsAreNotNumbers() throws IOException {
        String stationInformation = """
                {"data": {"stations": [
                  {"station_id": "42", "name": "B", "lat": 45.1, "lon": "5,7", "capacity": 12.5}
                ]}}
                """;
        mockDiscovery(VALID_DISCOVERY_JSON);
        when(gbfsHttpClient.getData(any(URI.class))).thenAnswer(invocation -> {
            URI uri = invocation.getArgument(0);
            String content = uri.toString().endsWith("/station_information.json") ? stationInformation : VALID_DISCOVERY_JSON;
            return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        });
        FileValidationResult discoveryFile = new FileValidationResult(
                "gbfs", true, true, 0, null, VALID_DISCOVERY_JSON, "3.0", List.of());
        FileValidationResult stationInformationValidation = new FileValidationResult(
                "station_information", true, true, 2, null, null, "3.0", List.of(
                new FileValidationError("#/properties/data/properties/stations/items/properties/lon", "#/data/stations/0/lon",
                        "#/data/stations/0/lon: expected type: Number, found: String"),
                new FileValidationError("#/properties/data/properties/stations/items/properties/capacity", "#/data/stations/0/capacity",
                        "#/data/stations/0/capacity: expected type: Integer, found: BigDecimal")));
        when(gbfsValidator.validate(anyMap())).thenReturn(
                new ValidationResult(new ValidationSummary("3.0", 0L, 0), Map.of("gbfs", discoveryFile)),
                new ValidationResult(new ValidationSummary("3.0", 0L, 2), Map.of("station_information", stationInformationValidation)));

        long parkingsBefore = parkingRepository.count();
        long organisationsBefore = organisationRepository.count();

        Job job = runGbfsParkingImport();

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(
                JobError.error(AnalyzeImportErrorType.INVALID_FORMAT_NUMBER)
                        .withParam("file", "station_information.json")
                        .withParam("entity", "station")
                        .withParam("entityId", "42")
                        .withParam("field", "lon")
                        .withParam("expected", "number"),
                JobError.error(AnalyzeImportErrorType.INVALID_FORMAT_NUMBER)
                        .withParam("file", "station_information.json")
                        .withParam("entity", "station")
                        .withParam("entityId", "42")
                        .withParam("field", "capacity")
                        .withParam("expected", "integer"));
        assertNothingImported(parkingsBefore, organisationsBefore);
    }

    @Test
    void importsTheRestWhenAPricingPlanIsInError() throws IOException {
        Map<String, String> feeds = Map.of(
                "gbfs.json", VALID_DISCOVERY_JSON,
                "system_information.json", """
                        {"last_updated": "2025-01-01T00:00:00Z", "ttl": 60, "version": "3.0", "data": {
                          "system_id": "pricing_plan_test", "languages": ["fr"], "name": [{"text": "Vélos test", "language": "fr"}],
                          "opening_hours": "24/7", "feed_contact_email": "contact@example.com", "timezone": "Europe/Paris"}}
                        """,
                "station_information.json", """
                        {"last_updated": "2025-01-01T00:00:00Z", "ttl": 60, "version": "3.0", "data": {"stations": [
                          {"station_id": "PRICING_ST1", "name": [{"text": "Gare", "language": "fr"}], "lat": 45.19, "lon": 5.72, "capacity": 10}
                        ]}}
                        """,
                "vehicle_types.json", """
                        {"last_updated": "2025-01-01T00:00:00Z", "ttl": 60, "version": "3.0", "data": {"vehicle_types": [
                          {"vehicle_type_id": "bike", "form_factor": "bicycle", "propulsion_type": "human"}
                        ]}}
                        """,
                "system_pricing_plans.json", """
                        {"last_updated": "2025-01-01T00:00:00Z", "ttl": 60, "version": "3.0", "data": {"plans": [
                          {"plan_id": "PLAN_ABONNE", "name": [{"text": "Abonné", "language": "fr"}], "price": 0, "is_taxable": false,
                           "description": [{"text": "Abonnement", "language": "fr"}]}
                        ]}}
                        """);
        when(gbfsHttpClient.getData(any(URI.class))).thenAnswer(invocation -> {
            String path = ((URI) invocation.getArgument(0)).getPath();
            String file = path.substring(path.lastIndexOf('/') + 1);
            return new ByteArrayInputStream(feeds.get(file).getBytes(StandardCharsets.UTF_8));
        });
        FileValidationResult discoveryFile = new FileValidationResult(
                "gbfs", true, true, 0, null, VALID_DISCOVERY_JSON, "3.0", List.of());
        FileValidationResult valid = new FileValidationResult("x", true, true, 0, null, null, "3.0", List.of());
        FileValidationResult pricingPlansValidation = new FileValidationResult(
                "system_pricing_plans", false, true, 1, null, null, "3.0", List.of(
                new FileValidationError("#/properties/data/properties/plans/items", "#/data/plans/0",
                        "#/data/plans/0: required key [currency] not found")));
        when(gbfsValidator.validate(anyMap())).thenReturn(
                new ValidationResult(new ValidationSummary("3.0", 0L, 0), Map.of("gbfs", discoveryFile)),
                new ValidationResult(new ValidationSummary("3.0", 0L, 1), Map.of(
                        "system_information", valid,
                        "station_information", valid,
                        "vehicle_types", valid,
                        "system_pricing_plans", pricingPlansValidation)));

        long parkingsBefore = parkingRepository.count();

        Job job = runGbfsParkingImport();

        assertThat(job.getStatus()).isEqualTo(JobStatus.FINISHED);
        assertThat(job.getErrors()).containsExactly(
                JobError.warning(AnalyzeImportErrorType.MISSING_REQUIRED_FIELD)
                        .withParam("file", "system_pricing_plans.json")
                        .withParam("entity", "plan")
                        .withParam("entityId", "PLAN_ABONNE")
                        .withParam("field", "currency"));
        assertThat(parkingRepository.count()).isEqualTo(parkingsBefore + 1);
    }

    @Test
    void cancelsImportWhenThereIsNoStation() throws IOException {
        String emptyStationInformation = """
                {"last_updated": "2025-01-01T00:00:00Z", "ttl": 60, "version": "3.0", "data": {"stations": []}}
                """;
        Map<String, String> feeds = Map.of(
                "gbfs.json", VALID_DISCOVERY_JSON,
                "system_information.json", """
                        {"last_updated": "2025-01-01T00:00:00Z", "ttl": 60, "version": "3.0", "data": {
                          "system_id": "no_station_test", "languages": ["fr"], "name": [{"text": "Vélos test", "language": "fr"}],
                          "opening_hours": "24/7", "feed_contact_email": "contact@example.com", "timezone": "Europe/Paris"}}
                        """,
                "station_information.json", emptyStationInformation,
                "vehicle_types.json", """
                        {"last_updated": "2025-01-01T00:00:00Z", "ttl": 60, "version": "3.0", "data": {"vehicle_types": [
                          {"vehicle_type_id": "bike", "form_factor": "bicycle", "propulsion_type": "human"}
                        ]}}
                        """,
                "system_pricing_plans.json", """
                        {"last_updated": "2025-01-01T00:00:00Z", "ttl": 60, "version": "3.0", "data": {"plans": []}}
                        """);
        when(gbfsHttpClient.getData(any(URI.class))).thenAnswer(invocation -> {
            String path = ((URI) invocation.getArgument(0)).getPath();
            String file = path.substring(path.lastIndexOf('/') + 1);
            return new ByteArrayInputStream(feeds.get(file).getBytes(StandardCharsets.UTF_8));
        });
        FileValidationResult discoveryFile = new FileValidationResult(
                "gbfs", true, true, 0, null, VALID_DISCOVERY_JSON, "3.0", List.of());
        FileValidationResult valid = new FileValidationResult("x", true, true, 0, null, null, "3.0", List.of());
        when(gbfsValidator.validate(anyMap())).thenReturn(
                new ValidationResult(new ValidationSummary("3.0", 0L, 0), Map.of("gbfs", discoveryFile)),
                new ValidationResult(new ValidationSummary("3.0", 0L, 0), Map.of(
                        "system_information", valid,
                        "station_information", valid,
                        "vehicle_types", valid,
                        "system_pricing_plans", valid)));

        long parkingsBefore = parkingRepository.count();
        long organisationsBefore = organisationRepository.count();

        Job job = runGbfsParkingImport();

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(
                JobError.error(AnalyzeImportErrorType.NO_DATA_ROWS).withParam("file", "station_information.json"));
        assertNothingImported(parkingsBefore, organisationsBefore);
    }

    @Test
    void cancelsImportWhenAStationIdIsDuplicated() throws IOException {
        mockValidV3Feeds("""
                {"last_updated": "2025-01-01T00:00:00Z", "ttl": 60, "version": "3.0", "data": {"stations": [
                  {"station_id": "42", "name": [{"text": "A", "language": "fr"}], "lat": 45.19, "lon": 5.72},
                  {"station_id": "43", "name": [{"text": "B", "language": "fr"}], "lat": 45.19, "lon": 5.72},
                  {"station_id": "42", "name": [{"text": "C", "language": "fr"}], "lat": 45.19, "lon": 5.72},
                  {"station_id": "43", "name": [{"text": "D", "language": "fr"}], "lat": 45.19, "lon": 5.72},
                  {"station_id": "42", "name": [{"text": "E", "language": "fr"}], "lat": 45.19, "lon": 5.72}
                ]}}
                """, BIKE_VEHICLE_TYPES);

        long parkingsBefore = parkingRepository.count();
        long organisationsBefore = organisationRepository.count();

        Job job = runGbfsParkingImport();

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(
                duplicatedId("station_information.json", "station_id", "42"),
                duplicatedId("station_information.json", "station_id", "43"));
        assertNothingImported(parkingsBefore, organisationsBefore);
    }

    @Test
    void cancelsImportWhenAVehicleTypeIdIsDuplicated() throws IOException {
        mockValidV3Feeds("""
                {"last_updated": "2025-01-01T00:00:00Z", "ttl": 60, "version": "3.0", "data": {"stations": [
                  {"station_id": "42", "name": [{"text": "A", "language": "fr"}], "lat": 45.19, "lon": 5.72}
                ]}}
                """, """
                {"last_updated": "2025-01-01T00:00:00Z", "ttl": 60, "version": "3.0", "data": {"vehicle_types": [
                  {"vehicle_type_id": "bike", "form_factor": "bicycle", "propulsion_type": "human"},
                  {"vehicle_type_id": "bike", "form_factor": "cargo_bicycle", "propulsion_type": "human"}
                ]}}
                """);

        long parkingsBefore = parkingRepository.count();
        long organisationsBefore = organisationRepository.count();

        Job job = runGbfsParkingImport();

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(duplicatedId("vehicle_types.json", "vehicle_type_id", "bike"));
        assertNothingImported(parkingsBefore, organisationsBefore);
    }

    @Test
    void cancelsImportWhenStationCoordinatesAreInvalid() throws IOException {
        mockValidV3Feeds("""
                {"last_updated": "2025-01-01T00:00:00Z", "ttl": 60, "version": "3.0", "data": {"stations": [
                  {"station_id": "41", "name": [{"text": "A", "language": "fr"}], "lat": 45.19, "lon": 5.72},
                  {"station_id": "42", "name": [{"text": "B", "language": "fr"}], "lat": 0, "lon": 0}
                ]}}
                """, BIKE_VEHICLE_TYPES);

        long parkingsBefore = parkingRepository.count();
        long organisationsBefore = organisationRepository.count();

        Job job = runGbfsParkingImport();

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(JobError.error(AnalyzeImportErrorType.INVALID_FORMAT_COORDINATE)
                .withParam("file", "station_information.json")
                .withParam("entity", "station")
                .withParam("entityId", "42")
                .withParam("coordinates", "0, 0"));
        assertNothingImported(parkingsBefore, organisationsBefore);
    }

    @Test
    void importsStationsAndWarnsWhenCapacityIsMissing() throws IOException {
        mockValidV3Feeds("""
                {"last_updated": "2025-01-01T00:00:00Z", "ttl": 60, "version": "3.0", "data": {"stations": [
                  {"station_id": "41", "name": [{"text": "A", "language": "fr"}], "lat": 45.19, "lon": 5.72, "capacity": 10},
                  {"station_id": "42", "name": [{"text": "B", "language": "fr"}], "lat": 45.19, "lon": 5.72}
                ]}}
                """, BIKE_VEHICLE_TYPES);

        long parkingsBefore = parkingRepository.count();

        Job job = runGbfsParkingImport();

        assertThat(job.getStatus()).isEqualTo(JobStatus.FINISHED);
        assertThat(job.getErrors()).containsExactly(JobError.warning(AnalyzeImportErrorType.MISSING_OPTIONAL_FIELD)
                .withParam("file", "station_information.json")
                .withParam("entity", "station")
                .withParam("entityId", "42")
                .withParam("field", "capacity"));
        assertThat(parkingRepository.count()).isEqualTo(parkingsBefore + 2);
    }

    @Test
    void importsStationsAndWarnsWhenAddressIsTruncated() throws IOException {
        String longAddress = "a".repeat(1200);
        mockValidV3Feeds("""
                {"last_updated": "2025-01-01T00:00:00Z", "ttl": 60, "version": "3.0", "data": {"stations": [
                  {"station_id": "41", "name": [{"text": "A", "language": "fr"}], "lat": 45.19, "lon": 5.72, "capacity": 10,
                   "address": "1 rue de la Gare"},
                  {"station_id": "42", "name": [{"text": "B", "language": "fr"}], "lat": 45.20, "lon": 5.73, "capacity": 10,
                   "address": "%s"}
                ]}}
                """.formatted(longAddress), BIKE_VEHICLE_TYPES);

        long parkingsBefore = parkingRepository.count();

        Job job = runGbfsParkingImport();

        assertThat(job.getStatus()).isEqualTo(JobStatus.FINISHED);
        assertThat(job.getErrors()).containsExactly(JobError.warning(AnalyzeImportErrorType.FIELD_TRUNCATED)
                .withParam("file", "station_information.json")
                .withParam("entity", "station")
                .withParam("entityId", "42")
                .withParam("field", "address")
                .withParam("originalLength", "1200")
                .withParam("maxLength", "1000"));
        assertThat(parkingRepository.count()).isEqualTo(parkingsBefore + 2);
        assertThat(parkingRepository.findAll()).anySatisfy(parking ->
                assertThat(parking.getAddress()).isEqualTo("a".repeat(1000)));
    }

    @Test
    void cancelsImportWhenAFieldIsTooLong() throws IOException {
        mockValidV3Feeds("""
                {"last_updated": "2025-01-01T00:00:00Z", "ttl": 60, "version": "3.0", "data": {"stations": [
                  {"station_id": "41", "name": [{"text": "A", "language": "fr"}], "lat": 45.19, "lon": 5.72, "capacity": 10},
                  {"station_id": "42", "name": [{"text": "B", "language": "zh-Hant"}], "lat": 45.20, "lon": 5.73, "capacity": 10,
                   "rental_uris": {"web": "http://example.com/%s"}}
                ]}}
                """.formatted("a".repeat(300 - "http://example.com/".length())), BIKE_VEHICLE_TYPES);

        long parkingsBefore = parkingRepository.count();
        long organisationsBefore = organisationRepository.count();

        Job job = runGbfsParkingImport();

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(
                fieldTooLong("42", "name.language", 7, 5),
                fieldTooLong("42", "rental_uris.web", 300, 255));
        assertNothingImported(parkingsBefore, organisationsBefore);
    }

    private static JobError fieldTooLong(String stationId, String field, int length, int maxLength) {
        return JobError.error(AnalyzeImportErrorType.FIELD_TOO_LONG)
                .withParam("file", "station_information.json")
                .withParam("entity", "station")
                .withParam("entityId", stationId)
                .withParam("field", field)
                .withParam("length", String.valueOf(length))
                .withParam("maxLength", String.valueOf(maxLength));
    }

    @Test
    void reportsAnUnexpectedErrorWhenTheExceptionIsNotClassified() throws IOException {
        when(gbfsHttpClient.getData(any(URI.class))).thenThrow(new IllegalStateException("bug"));

        Job job = runGbfsParkingImport();

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrors()).containsExactly(JobError.error(AnalyzeImportErrorType.UNEXPECTED));
        assertThat(job.getMessage()).contains("IllegalStateException - bug");
    }

    private static final String BIKE_VEHICLE_TYPES = """
            {"last_updated": "2025-01-01T00:00:00Z", "ttl": 60, "version": "3.0", "data": {"vehicle_types": [
              {"vehicle_type_id": "bike", "form_factor": "bicycle", "propulsion_type": "human"}
            ]}}
            """;

    private static JobError duplicatedId(String file, String field, String id) {
        return JobError.error(AnalyzeImportErrorType.DUPLICATE_ID_FILE)
                .withParam("fileName", file)
                .withParam("field", field)
                .withParam("errorValue", id);
    }

    /**
     * Serves a GBFS 3.0 feed (discovery file VALID_DISCOVERY_JSON) whose files are all considered valid by the
     * validator.
     */
    private void mockValidV3Feeds(String stationInformation, String vehicleTypes) throws IOException {
        Map<String, String> feeds = Map.of(
                "gbfs.json", VALID_DISCOVERY_JSON,
                "system_information.json", """
                        {"last_updated": "2025-01-01T00:00:00Z", "ttl": 60, "version": "3.0", "data": {
                          "system_id": "duplicate_test", "languages": ["fr"], "name": [{"text": "Vélos test", "language": "fr"}],
                          "opening_hours": "24/7", "feed_contact_email": "contact@example.com", "timezone": "Europe/Paris"}}
                        """,
                "station_information.json", stationInformation,
                "vehicle_types.json", vehicleTypes,
                "system_pricing_plans.json", """
                        {"last_updated": "2025-01-01T00:00:00Z", "ttl": 60, "version": "3.0", "data": {"plans": []}}
                        """);
        when(gbfsHttpClient.getData(any(URI.class))).thenAnswer(invocation -> {
            String path = ((URI) invocation.getArgument(0)).getPath();
            String file = path.substring(path.lastIndexOf('/') + 1);
            return new ByteArrayInputStream(feeds.get(file).getBytes(StandardCharsets.UTF_8));
        });
        FileValidationResult discoveryFile = new FileValidationResult(
                "gbfs", true, true, 0, null, VALID_DISCOVERY_JSON, "3.0", List.of());
        FileValidationResult valid = new FileValidationResult("x", true, true, 0, null, null, "3.0", List.of());
        when(gbfsValidator.validate(anyMap())).thenReturn(
                new ValidationResult(new ValidationSummary("3.0", 0L, 0), Map.of("gbfs", discoveryFile)),
                new ValidationResult(new ValidationSummary("3.0", 0L, 0), Map.of(
                        "system_information", valid,
                        "station_information", valid,
                        "vehicle_types", valid,
                        "system_pricing_plans", valid)));
    }

    private void mockDiscovery(String discoveryJson) throws IOException {
        when(gbfsHttpClient.getData(any(URI.class)))
                .thenAnswer(invocation -> new ByteArrayInputStream(discoveryJson.getBytes(StandardCharsets.UTF_8)));
        FileValidationResult discoveryFile = new FileValidationResult(
                "gbfs", true, true, 0, null, discoveryJson, "3.0", List.of());
        when(gbfsValidator.validate(anyMap()))
                .thenReturn(new ValidationResult(new ValidationSummary("3.0", 0L, 0), Map.of("gbfs", discoveryFile)));
    }

    private void assertNothingImported(long parkingsBefore, long organisationsBefore) {
        assertThat(parkingRepository.count()).isEqualTo(parkingsBefore);
        assertThat(organisationRepository.count()).isEqualTo(organisationsBefore);
    }

    private Job runGbfsParkingImport() {
        GbfsParkingImportParams params = new GbfsParkingImportParams();
        params.setGlobalUrl(GLOBAL_URL);
        params.setParkingType(ParkingTypeEnumeration.CYCLE_RENTAL);
        params.setParkingAreaType(SpecificParkingAreaUsageEnumeration.PEDAL_CYCLE);

        Job job = new Job();
        job.setFileName(GLOBAL_URL);
        job.setType(JobType.GBFS_PARKING);
        job.setAction(JobAction.IMPORT);
        job.setStatus(JobStatus.PROCESSING);
        job.setStarted(Instant.now());
        jobRepository.save(job);

        ImportJobWorker worker = importJobWorkerBuilder
                .init(job)
                .withGbfsParkingImportParams(params)
                .build();
        worker.run();

        return jobRepository.findById(job.getId()).orElseThrow();
    }
}
