package org.rutebanken.tiamat.service.stopplace.report;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.rutebanken.tiamat.model.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StopPlaceReportCsvExporterTest extends TiamatIntegrationTest {

    @Autowired
    private StopPlaceReportCsvExporter stopPlaceReportCsvExporter;

    @Test
    void exportStopPlacesWithRequestedColumns() {
        Quay quay2 = createQuay("2", "75056", 2.35220, 48.85660);
        Quay quay1 = createQuay("1", null, 2.35221, 48.85661);
        StopPlace stopPlace = createStopPlace("Export Arrets", StopTypeEnumeration.ONSTREET_BUS, 2.3522, 48.8566, quay2, quay1);
        stopPlace.setProvider("PROV1");
        stopPlace.getOrCreateValues("imported-id").add("PROV1:StopPlace:42");
        stopPlaceRepository.save(stopPlace);

        String csv = export(StopPlaceReportExportRequest.Type.STOP_PLACES,
                List.of("name", "modality", "id", "importedId", "position", "inseeCode", "provider", "quays", "parking", "tags"),
                "Export Arrets");

        assertThat(csv).startsWith("﻿");
        assertThat(lines(csv)).containsExactly(
                "name;modality;id;importedId;position;inseeCode;provider;quays;parking;tags",
                "\"Export Arrets\";\"onstreetBus\";\"" + stopPlace.getNetexId() + "\";\"PROV1:StopPlace:42\";\"48.8566,2.3522\";\"75056\";\"PROV1\";\""
                        + quay1.getNetexId() + "," + quay2.getNetexId() + "\";\"\";\"\"");
    }

    @Test
    void exportParentStopPlaceFollowedByItsChildren() {
        StopPlace child = createStopPlace("Export Parent", StopTypeEnumeration.ONSTREET_BUS, 2.3522, 48.8566, createQuay("1", "75056", 2.3522, 48.8566));

        StopPlace parent = new StopPlace(new EmbeddableMultilingualString("Export Parent"));
        parent.setParentStopPlace(true);
        parent.setCentroid(geometryFactory.createPoint(new Coordinate(2.3522, 48.8566)));
        parent.getChildren().add(child);
        parent = stopPlaceRepository.save(parent);

        child = parent.getChildren().iterator().next();
        child.setParentSiteRef(new SiteRefStructure(parent.getNetexId(), String.valueOf(parent.getVersion())));
        child = stopPlaceRepository.save(child);

        String csv = export(StopPlaceReportExportRequest.Type.STOP_PLACES, List.of("id", "name", "modality", "inseeCode"), "Export Parent");

        assertThat(lines(csv)).containsExactly(
                "id;name;modality;inseeCode",
                "\"" + parent.getNetexId() + "\";\"Export Parent\";\"onstreetBus\";\"75056\"",
                "\"" + child.getNetexId() + "\";\"Export Parent\";\"onstreetBus\";\"75056\"");
    }

    @Test
    void exportQuaysWithAccessibilityDeducedFromStopPlace() {
        Quay quayWithoutAssessment = createQuay("1", null, 2.3522, 48.8566);
        quayWithoutAssessment.setPrivateCode(new PrivateCodeStructure("P1", "type"));
        StopPlace stopPlace = createStopPlace("Export Quais", StopTypeEnumeration.ONSTREET_BUS, 2.3522, 48.8566, quayWithoutAssessment);
        stopPlace.setAccessibilityAssessment(createAssessment(LimitationStatusEnumeration.TRUE));
        stopPlaceRepository.save(stopPlace);

        String csv = export(StopPlaceReportExportRequest.Type.QUAYS,
                List.of("stopPlaceId", "stopPlaceName", "id", "publicCode", "privateCode", "wheelchairAccess"), "Export Quais");

        assertThat(lines(csv)).containsExactly(
                "stopPlaceId;stopPlaceName;id;publicCode;privateCode;wheelchairAccess",
                "\"" + stopPlace.getNetexId() + "\";\"Export Quais\";\"" + quayWithoutAssessment.getNetexId() + "\";\"1\";\"P1\";\"TRUE\"");
    }

    @Test
    void stopPlaceAccessibilityIsDeducedFromQuaysWhenStopPlaceHasNone() {
        Quay accessibleQuay = createQuay("1", null, 2.3522, 48.8566);
        accessibleQuay.setAccessibilityAssessment(createAssessment(LimitationStatusEnumeration.TRUE));
        Quay notAccessibleQuay = createQuay("2", null, 2.3522, 48.8566);
        notAccessibleQuay.setAccessibilityAssessment(createAssessment(LimitationStatusEnumeration.FALSE));
        StopPlace stopPlace = createStopPlace("Export Accessibilite", StopTypeEnumeration.ONSTREET_BUS, 2.3522, 48.8566, accessibleQuay, notAccessibleQuay);
        stopPlaceRepository.save(stopPlace);

        String csv = export(StopPlaceReportExportRequest.Type.STOP_PLACES, List.of("wheelchairAccess"), "Export Accessibilite");

        assertThat(lines(csv)).containsExactly("wheelchairAccess", "\"PARTIAL\"");
    }

    @Test
    void exportAllStopPlacesWithoutResultLimit() {
        int numberOfStopPlaces = 105;
        for (int i = 0; i < numberOfStopPlaces; i++) {
            stopPlaceRepository.save(createStopPlace("Export Sans Limite", StopTypeEnumeration.ONSTREET_BUS, 2.3522 + i * 0.01, 48.8566));
        }

        String csv = export(StopPlaceReportExportRequest.Type.STOP_PLACES, List.of("id"), "Export Sans Limite");

        assertThat(lines(csv)).hasSize(numberOfStopPlaces + 1);
    }

    @Test
    void rejectUnknownColumn() {
        StopPlaceReportExportRequest request = new StopPlaceReportExportRequest();
        request.setType(StopPlaceReportExportRequest.Type.QUAYS);
        request.setColumns(List.of("id", "mergeId"));

        assertThatThrownBy(() -> stopPlaceReportCsvExporter.export(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mergeId");
    }

    @Test
    void positionHasTheSamePrecisionAsTheReportPage() {
        assertThat(StopPlaceReportCsvExporter.jsDecimalPrecision(48.85661234567)).isEqualTo("48.856612");
        assertThat(StopPlaceReportCsvExporter.jsDecimalPrecision(2.3522)).isEqualTo("2.3522");
        assertThat(StopPlaceReportCsvExporter.jsDecimalPrecision(-1.55345678912)).isEqualTo("-1.5534568");
        assertThat(StopPlaceReportCsvExporter.jsDecimalPrecision(48.0)).isEqualTo("48");
    }

    private String export(StopPlaceReportExportRequest.Type type, List<String> columns, String query) {
        StopPlaceReportExportRequest request = new StopPlaceReportExportRequest();
        request.setType(type);
        request.setColumns(columns);
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("query", query);
        arguments.put("allVersions", true);
        request.setArguments(arguments);
        return stopPlaceReportCsvExporter.export(request);
    }

    private static List<String> lines(String csv) {
        return Arrays.asList(csv.substring(1).split("\r\n"));
    }

    private StopPlace createStopPlace(String name, StopTypeEnumeration stopPlaceType, double longitude, double latitude, Quay... quays) {
        StopPlace stopPlace = new StopPlace(new EmbeddableMultilingualString(name));
        stopPlace.setStopPlaceType(stopPlaceType);
        stopPlace.setTransportMode(VehicleModeEnumeration.BUS);
        stopPlace.setCentroid(geometryFactory.createPoint(new Coordinate(longitude, latitude)));
        stopPlace.setQuays(new HashSet<>(Arrays.asList(quays)));
        return stopPlace;
    }

    private Quay createQuay(String publicCode, String inseeCode, double longitude, double latitude) {
        Quay quay = new Quay();
        quay.setPublicCode(publicCode);
        quay.setInseeCode(inseeCode);
        quay.setCentroid(geometryFactory.createPoint(new Coordinate(longitude, latitude)));
        return quay;
    }

    private static AccessibilityAssessment createAssessment(LimitationStatusEnumeration wheelchairAccess) {
        AccessibilityLimitation limitation = new AccessibilityLimitation();
        limitation.setWheelchairAccess(wheelchairAccess);
        AccessibilityAssessment assessment = new AccessibilityAssessment();
        List<AccessibilityLimitation> limitations = new ArrayList<>();
        limitations.add(limitation);
        assessment.setLimitations(limitations);
        return assessment;
    }
}
