package org.rutebanken.tiamat.service.stopplace.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.apache.commons.lang3.StringUtils;
import org.rutebanken.tiamat.auth.StopPlaceAuthorizationService;
import org.rutebanken.tiamat.exporter.params.ExportParams;
import org.rutebanken.tiamat.exporter.params.StopPlaceSearch;
import org.rutebanken.tiamat.model.StopTypeEnumeration;
import org.rutebanken.tiamat.repository.search.StopPlaceQueryFromSearchBuilder;
import org.rutebanken.tiamat.rest.graphql.fetchers.StopPlaceSearchArgumentsMapper;
import org.rutebanken.tiamat.service.stopplace.StopPlaceMergeCandidateFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.rutebanken.tiamat.exporter.params.ExportParams.newExportParamsBuilder;
import static org.rutebanken.tiamat.exporter.params.StopPlaceSearch.newStopPlaceSearchBuilder;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.*;

@Service
public class StopPlaceReportCsvExporter {

    private static final Logger logger = LoggerFactory.getLogger(StopPlaceReportCsvExporter.class);

    public static final List<String> STOP_PLACE_COLUMNS = List.of("name", "modality", "id", "county", "muncipality",
            "importedId", "importedName", "position", "inseeCode", "mergeId", "provider", "quays", "parking",
            "wheelchairAccess", "stepFreeAccess", "shelterEquipment", "waitingRoomEquipment", "sanitaryEquipment",
            "generalSign", "tags");

    public static final List<String> QUAY_COLUMNS = List.of("stopPlaceId", "stopPlaceName", "id", "importedId",
            "importedName", "position", "privateCode", "publicCode", "wheelchairAccess", "stepFreeAccess",
            "shelterEquipment", "waitingRoomEquipment", "sanitaryEquipment", "generalSign");

    private static final String DELIMITER = ";";
    private static final String LINE_SEPARATOR = "\r\n";
    private static final String BOM = "﻿";

    private static final int SQL_IN_CHUNK_SIZE = 1000;

    private static final int NB_OF_VERSIONS_TO_KEEP = 10;

    private static final String IMPORTED_ID_KEY = "imported-id";
    private static final String IMPORTED_NAME_KEY = "imported-name";

    private static final String UNKNOWN = "UNKNOWN";
    private static final String TRUE = "TRUE";
    private static final String FALSE = "FALSE";
    private static final String PARTIAL = "PARTIAL";

    private static final String SHELTER_EQUIPMENT_TYPE = "ShelterEquipment";
    private static final String WAITING_ROOM_EQUIPMENT_TYPE = "WaitingRoomEquipment";
    private static final String SANITARY_EQUIPMENT_TYPE = "SanitaryEquipment";
    private static final String GENERAL_SIGN_TYPE = "GeneralSign";
    private static final String TRANSPORT_MODE_SIGN_CONTENT = "TRANSPORT_MODE";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PersistenceContext
    private EntityManager entityManager;

    private final StopPlaceQueryFromSearchBuilder stopPlaceQueryFromSearchBuilder;

    private final StopPlaceAuthorizationService stopPlaceAuthorizationService;

    private final StopPlaceMergeCandidateFinder stopPlaceMergeCandidateFinder;

    public StopPlaceReportCsvExporter(StopPlaceQueryFromSearchBuilder stopPlaceQueryFromSearchBuilder,
                                      StopPlaceAuthorizationService stopPlaceAuthorizationService,
                                      StopPlaceMergeCandidateFinder stopPlaceMergeCandidateFinder) {
        this.stopPlaceQueryFromSearchBuilder = stopPlaceQueryFromSearchBuilder;
        this.stopPlaceAuthorizationService = stopPlaceAuthorizationService;
        this.stopPlaceMergeCandidateFinder = stopPlaceMergeCandidateFinder;
    }

    @Transactional(readOnly = true)
    public String export(StopPlaceReportExportRequest request) {
        long start = System.currentTimeMillis();
        List<String> columns = validateColumns(request);
        Map<String, Object> arguments = toSearchArguments(request.getArguments());
        boolean hasParkingFilter = Boolean.TRUE.equals(arguments.get(HAS_PARKING));
        boolean isStopPlaceExport = request.getType() == StopPlaceReportExportRequest.Type.STOP_PLACES;

        List<SearchHit> hits = searchStopPlaces(arguments);
        List<SearchHit> results = StopPlaceSearchArgumentsMapper.shouldResolveParents(arguments) ? resolveParents(hits) : hits;
        ReportData data = new ReportData();
        List<ReportRow> rows = toRows(results, data);
        loadData(rows, columns, isStopPlaceExport, hasParkingFilter, data);

        if (hasParkingFilter) {
            rows = rows.stream()
                    .filter(row -> !data.parkingFor(row).isEmpty())
                    .collect(Collectors.toList());
        }

        String csv = isStopPlaceExport ? buildStopPlacesCsv(rows, columns, data) : buildQuaysCsv(rows, columns, data);
        logger.info("Report CSV export of {} built with {} rows in {} ms", request.getType(), rows.size(), System.currentTimeMillis() - start);
        return csv;
    }

    private List<String> validateColumns(StopPlaceReportExportRequest request) {
        if (request.getType() == null) {
            throw new IllegalArgumentException("Missing export type, allowed values: " + Arrays.toString(StopPlaceReportExportRequest.Type.values()));
        }
        List<String> allowedColumns = request.getType() == StopPlaceReportExportRequest.Type.STOP_PLACES ? STOP_PLACE_COLUMNS : QUAY_COLUMNS;
        List<String> columns = request.getColumns() == null ? List.of() : request.getColumns();
        List<String> unknownColumns = columns.stream().filter(column -> !allowedColumns.contains(column)).collect(Collectors.toList());
        if (!unknownColumns.isEmpty()) {
            throw new IllegalArgumentException("Unknown columns " + unknownColumns + ", allowed columns: " + allowedColumns);
        }
        return columns;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toSearchArguments(Map<String, Object> jsonArguments) {
        Map<String, Object> arguments = new HashMap<>(jsonArguments == null ? Map.of() : jsonArguments);
        arguments.values().removeIf(Objects::isNull);
        if (arguments.get(STOP_PLACE_TYPE) != null) {
            arguments.put(STOP_PLACE_TYPE, ((List<String>) arguments.get(STOP_PLACE_TYPE)).stream()
                    .filter(Objects::nonNull)
                    .map(StopTypeEnumeration::fromValue)
                    .collect(Collectors.toList()));
        }
        if (arguments.get(POINT_IN_TIME) != null) {
            arguments.put(POINT_IN_TIME, Instant.parse((String) arguments.get(POINT_IN_TIME)));
        }
        if (arguments.get(NEARBY_RADIUS) != null) {
            arguments.put(NEARBY_RADIUS, ((Number) arguments.get(NEARBY_RADIUS)).intValue());
        }
        return arguments;
    }

    @SuppressWarnings("unchecked")
    private List<SearchHit> searchStopPlaces(Map<String, Object> arguments) {
        ExportParams.Builder exportParamsBuilder = newExportParamsBuilder();
        StopPlaceSearch.Builder stopPlaceSearchBuilder = newStopPlaceSearchBuilder();
        exportParamsBuilder.setProviderList(stopPlaceAuthorizationService.getFilteredProviders());

        Boolean allVersions = StopPlaceSearchArgumentsMapper.applySearchFlags(arguments, stopPlaceSearchBuilder);
        StopPlaceSearchArgumentsMapper.applyStandardFilters(arguments, allVersions, StopPlaceSearchArgumentsMapper.getPointInTime(arguments),
                exportParamsBuilder, stopPlaceSearchBuilder);
        ExportParams exportParams = exportParamsBuilder.setStopPlaceSearch(stopPlaceSearchBuilder.build()).build();

        Pair<String, Map<String, Object>> queryWithParams = stopPlaceQueryFromSearchBuilder.buildQueryString(exportParams);
        Query query = entityManager.createNativeQuery("select sub.id, sub.netex_id, sub.version, sub.parent_stop_place, sub.parent_site_ref, sub.parent_site_ref_version " +
                "from (" + queryWithParams.getFirst() + ") sub");
        queryWithParams.getSecond().forEach(query::setParameter);

        List<SearchHit> hits = ((List<Object[]>) query.getResultList()).stream()
                .map(row -> new SearchHit(toLong(row[0]), (String) row[1], toLong(row[2]), Boolean.TRUE.equals(row[3]), (String) row[4], (String) row[5]))
                .collect(Collectors.toList());
        return keepLastVersions(hits);
    }

    private List<SearchHit> keepLastVersions(List<SearchHit> hits) {
        Map<String, Long> maxVersions = new HashMap<>();
        hits.forEach(hit -> maxVersions.merge(hit.netexId, hit.version, Math::max));
        return hits.stream()
                .filter(hit -> hit.version >= maxVersions.get(hit.netexId) - NB_OF_VERSIONS_TO_KEEP)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<SearchHit> resolveParents(List<SearchHit> hits) {
        List<SearchHit> result = hits.stream().filter(hit -> hit.parent).collect(Collectors.toList());
        List<SearchHit> nonParentHits = hits.stream().filter(hit -> !hit.parent).collect(Collectors.toList());

        Set<String> parentNetexIds = nonParentHits.stream()
                .map(hit -> hit.parentSiteRef)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, SearchHit> parentsByNetexIdAndVersion = new HashMap<>();
        forEachChunk(parentNetexIds, chunk -> {
            List<Object[]> rows = entityManager.createNativeQuery("select s.id, s.netex_id, s.version from stop_place s where s.netex_id in (:netexIds)")
                    .setParameter("netexIds", chunk)
                    .getResultList();
            rows.forEach(row -> {
                SearchHit parent = new SearchHit(toLong(row[0]), (String) row[1], toLong(row[2]), true, null, null);
                parentsByNetexIdAndVersion.put(parent.netexId + ":" + parent.version, parent);
            });
        });

        Set<String> resultNetexIdsAndVersions = result.stream().map(hit -> hit.netexId + ":" + hit.version).collect(Collectors.toCollection(HashSet::new));
        for (SearchHit hit : nonParentHits) {
            if (hit.parentSiteRef == null) {
                result.add(hit);
                continue;
            }
            SearchHit parent = parentsByNetexIdAndVersion.get(hit.parentSiteRef + ":" + hit.parentSiteRefVersion);
            if (parent == null) {
                logger.warn("Could not resolve parent {} {} of stop place {}", hit.parentSiteRef, hit.parentSiteRefVersion, hit.netexId);
            } else if (resultNetexIdsAndVersions.add(parent.netexId + ":" + parent.version)) {
                result.add(parent);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<ReportRow> toRows(List<SearchHit> results, ReportData data) {
        Set<Long> parentIds = results.stream().filter(hit -> hit.parent).map(hit -> hit.id).collect(Collectors.toSet());
        Map<Long, List<Long>> childrenByParentId = data.childrenIdsByParentId;
        forEachChunk(parentIds, chunk -> {
            List<Object[]> rows = entityManager.createNativeQuery("select c.stop_place_id, c.children_id from stop_place_children c " +
                            "where c.stop_place_id in (:ids) order by c.stop_place_id, c.children_id")
                    .setParameter("ids", chunk)
                    .getResultList();
            rows.forEach(row -> childrenByParentId.computeIfAbsent(toLong(row[0]), id -> new ArrayList<>()).add(toLong(row[1])));
        });

        List<ReportRow> rows = new ArrayList<>();
        for (SearchHit hit : results) {
            if (hit.parent) {
                rows.add(new ReportRow(hit.id, true, null));
                childrenByParentId.getOrDefault(hit.id, List.of()).forEach(childId -> rows.add(new ReportRow(childId, false, hit.id)));
            } else {
                rows.add(new ReportRow(hit.id, false, null));
            }
        }
        return rows;
    }

    private void loadData(List<ReportRow> rows, List<String> columns, boolean isStopPlaceExport, boolean hasParkingFilter, ReportData data) {
        Set<Long> stopPlaceIds = rows.stream().map(row -> row.stopPlaceId).collect(Collectors.toCollection(LinkedHashSet::new));
        rows.stream().map(row -> row.parentId).filter(Objects::nonNull).forEach(stopPlaceIds::add);

        loadStopPlaces(stopPlaceIds, data);
        loadQuays(stopPlaceIds, data);

        Set<String> netexIds = data.stopPlaces.values().stream().map(stopPlace -> stopPlace.netexId).collect(Collectors.toSet());

        if (isStopPlaceExport && (columns.contains("importedId") || columns.contains("importedName"))) {
            loadStopPlaceImportedIdsAndNames(stopPlaceIds, data);
        }
        if (!isStopPlaceExport && (columns.contains("importedId") || columns.contains("importedName"))) {
            loadQuayImportedIdsAndNames(data);
        }
        if (isStopPlaceExport && (columns.contains("county") || columns.contains("muncipality"))) {
            loadTopographicPlaces(data);
        }
        if (columns.contains("wheelchairAccess") || columns.contains("stepFreeAccess")) {
            loadAccessibilityLimitations(data);
        }
        if (columns.contains("shelterEquipment") || columns.contains("waitingRoomEquipment")
                || columns.contains("sanitaryEquipment") || columns.contains("generalSign")) {
            loadEquipments(data);
        }
        if (isStopPlaceExport && columns.contains("tags")) {
            loadTags(netexIds, data);
        }
        // Parking are only loaded when needed, as it is a costly information
        if (hasParkingFilter || (isStopPlaceExport && columns.contains("parking"))) {
            loadParking(netexIds, data);
        }
        if (isStopPlaceExport && columns.contains("mergeId")) {
            data.mergeGroups = stopPlaceMergeCandidateFinder.computeMergeGroups();
        }
    }

    @SuppressWarnings("unchecked")
    private void loadStopPlaces(Set<Long> stopPlaceIds, ReportData data) {
        forEachChunk(stopPlaceIds, chunk -> {
            List<Object[]> rows = entityManager.createNativeQuery("select s.id, s.netex_id, s.name_value, s.stop_place_type, s.parent_stop_place, s.provider, " +
                            "ST_Y(s.centroid), ST_X(s.centroid), s.topographic_place_id, s.accessibility_assessment_id, s.place_equipments_id " +
                            "from stop_place s where s.id in (:ids)")
                    .setParameter("ids", chunk)
                    .getResultList();
            rows.forEach(row -> {
                StopPlaceData stopPlace = new StopPlaceData();
                stopPlace.id = toLong(row[0]);
                stopPlace.netexId = (String) row[1];
                stopPlace.name = (String) row[2];
                stopPlace.stopPlaceType = row[3] == null ? null : StopTypeEnumeration.valueOf((String) row[3]).value();
                stopPlace.parent = Boolean.TRUE.equals(row[4]);
                stopPlace.provider = (String) row[5];
                stopPlace.latitude = toDouble(row[6]);
                stopPlace.longitude = toDouble(row[7]);
                stopPlace.topographicPlaceId = toLong(row[8]);
                stopPlace.accessibilityAssessmentId = toLong(row[9]);
                stopPlace.placeEquipmentId = toLong(row[10]);
                data.stopPlaces.put(stopPlace.id, stopPlace);
            });
        });
    }

    @SuppressWarnings("unchecked")
    private void loadQuays(Set<Long> stopPlaceIds, ReportData data) {
        forEachChunk(stopPlaceIds, chunk -> {
            List<Object[]> rows = entityManager.createNativeQuery("select spq.stop_place_id, q.id, q.netex_id, q.public_code, q.private_code_value, " +
                            "ST_Y(q.centroid), ST_X(q.centroid), q.accessibility_assessment_id, q.place_equipments_id, q.insee_code " +
                            "from stop_place_quays spq join quay q on q.id = spq.quays_id where spq.stop_place_id in (:ids) order by spq.stop_place_id, q.id")
                    .setParameter("ids", chunk)
                    .getResultList();
            rows.forEach(row -> {
                QuayData quay = new QuayData();
                quay.id = toLong(row[1]);
                quay.netexId = (String) row[2];
                quay.publicCode = (String) row[3];
                quay.privateCode = (String) row[4];
                quay.latitude = toDouble(row[5]);
                quay.longitude = toDouble(row[6]);
                quay.accessibilityAssessmentId = toLong(row[7]);
                quay.placeEquipmentId = toLong(row[8]);
                quay.inseeCode = (String) row[9];
                data.quaysByStopPlaceId.computeIfAbsent(toLong(row[0]), id -> new ArrayList<>()).add(quay);
            });
        });
        data.quaysByStopPlaceId.values().forEach(StopPlaceReportCsvExporter::sortQuays);
    }

    private static void sortQuays(List<QuayData> quays) {
        quays.sort(Comparator.comparing((QuayData quay) -> toNumberOrNull(quay.publicCode), Comparator.nullsLast(Comparator.naturalOrder())));
    }

    @SuppressWarnings("unchecked")
    private void loadStopPlaceImportedIdsAndNames(Set<Long> stopPlaceIds, ReportData data) {
        forEachChunk(stopPlaceIds, chunk -> {
            List<Object[]> rows = entityManager.createNativeQuery("select kv.stop_place_id, kv.key_values_key, vi.items " +
                            "from stop_place_key_values kv join value_items vi on vi.value_id = kv.key_values_id " +
                            "where kv.key_values_key in (:keys) and kv.stop_place_id in (:ids)")
                    .setParameter("keys", List.of(IMPORTED_ID_KEY, IMPORTED_NAME_KEY))
                    .setParameter("ids", chunk)
                    .getResultList();
            rows.forEach(row -> addKeyValue(data.stopPlaces.get(toLong(row[0])), (String) row[1], (String) row[2]));
        });
    }

    @SuppressWarnings("unchecked")
    private void loadQuayImportedIdsAndNames(ReportData data) {
        Map<Long, QuayData> quaysById = data.quaysById();
        forEachChunk(quaysById.keySet(), chunk -> {
            List<Object[]> rows = entityManager.createNativeQuery("select kv.quay_id, kv.key_values_key, vi.items " +
                            "from quay_key_values kv join value_items vi on vi.value_id = kv.key_values_id " +
                            "where kv.key_values_key in (:keys) and kv.quay_id in (:ids)")
                    .setParameter("keys", List.of(IMPORTED_ID_KEY, IMPORTED_NAME_KEY))
                    .setParameter("ids", chunk)
                    .getResultList();
            rows.forEach(row -> addKeyValue(quaysById.get(toLong(row[0])), (String) row[1], (String) row[2]));
        });
    }

    private static void addKeyValue(ImportedData importedData, String key, String value) {
        if (importedData == null || value == null) {
            return;
        }
        if (IMPORTED_ID_KEY.equals(key)) {
            importedData.importedIds.add(value);
        } else {
            importedData.importedNames.add(value);
        }
    }

    @SuppressWarnings("unchecked")
    private void loadTopographicPlaces(ReportData data) {
        Set<Long> topographicPlaceIds = data.stopPlaces.values().stream()
                .map(stopPlace -> stopPlace.topographicPlaceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        forEachChunk(topographicPlaceIds, chunk -> {
            List<Object[]> rows = entityManager.createNativeQuery("select tp.id, tp.name_value, ptp.name_value from topographic_place tp " +
                            "left join topographic_place ptp on ptp.netex_id = tp.parent_ref and cast(ptp.version as varchar) = tp.parent_ref_version " +
                            "where tp.id in (:ids)")
                    .setParameter("ids", chunk)
                    .getResultList();
            rows.forEach(row -> data.topographicPlaces.put(toLong(row[0]), new String[]{(String) row[1], (String) row[2]}));
        });
    }

    @SuppressWarnings("unchecked")
    private void loadAccessibilityLimitations(ReportData data) {
        Set<Long> assessmentIds = new HashSet<>();
        data.stopPlaces.values().forEach(stopPlace -> addIfNonNull(assessmentIds, stopPlace.accessibilityAssessmentId));
        data.quaysById().values().forEach(quay -> addIfNonNull(assessmentIds, quay.accessibilityAssessmentId));
        forEachChunk(assessmentIds, chunk -> {
            List<Object[]> rows = entityManager.createNativeQuery("select aal.accessibility_assessment_id, al.wheelchair_access, al.step_free_access " +
                            "from accessibility_assessment_limitations aal join accessibility_limitation al on al.id = aal.limitations_id " +
                            "where aal.accessibility_assessment_id in (:ids) order by aal.accessibility_assessment_id, al.id")
                    .setParameter("ids", chunk)
                    .getResultList();
            // Like in the GraphQL API, only the first limitation of an assessment is used
            rows.forEach(row -> data.limitationsByAssessmentId.putIfAbsent(toLong(row[0]), new Limitations((String) row[1], (String) row[2])));
        });
    }

    @SuppressWarnings("unchecked")
    private void loadEquipments(ReportData data) {
        Set<Long> placeEquipmentIds = new HashSet<>();
        data.stopPlaces.values().forEach(stopPlace -> addIfNonNull(placeEquipmentIds, stopPlace.placeEquipmentId));
        data.quaysById().values().forEach(quay -> addIfNonNull(placeEquipmentIds, quay.placeEquipmentId));
        forEachChunk(placeEquipmentIds, chunk -> {
            List<Object[]> rows = entityManager.createNativeQuery("select j.place_equipment_id, e.dtype, e.sign_content_type, e.private_code_value " +
                            "from installed_equipment_version_structure_installed_equipment j " +
                            "join installed_equipment_version_structure e on e.id = j.installed_equipment_id " +
                            "where j.place_equipment_id in (:ids) order by j.place_equipment_id, e.id")
                    .setParameter("ids", chunk)
                    .getResultList();
            rows.forEach(row -> data.equipmentsByPlaceEquipmentId.computeIfAbsent(toLong(row[0]), id -> new ArrayList<>())
                    .add(new Equipment((String) row[1], (String) row[2], (String) row[3])));
        });
    }

    @SuppressWarnings("unchecked")
    private void loadTags(Set<String> netexIds, ReportData data) {
        forEachChunk(netexIds, chunk -> {
            List<Object[]> rows = entityManager.createNativeQuery("select t.netex_reference, t.name from tag t where t.netex_reference in (:netexIds) order by t.netex_reference, t.name")
                    .setParameter("netexIds", chunk)
                    .getResultList();
            rows.forEach(row -> data.tagsByNetexId.computeIfAbsent((String) row[0], id -> new ArrayList<>()).add((String) row[1]));
        });
    }

    @SuppressWarnings("unchecked")
    private void loadParking(Set<String> netexIds, ReportData data) {
        data.parkingLoaded = true;
        forEachChunk(netexIds, chunk -> {
            List<Object[]> rows = entityManager.createNativeQuery("select p.parent_site_ref, p.netex_id from parking p " +
                            "where p.parent_site_ref in (:netexIds) and p.version = (select max(pv.version) from parking pv where pv.netex_id = p.netex_id) " +
                            "order by p.parent_site_ref, p.netex_id")
                    .setParameter("netexIds", chunk)
                    .getResultList();
            rows.forEach(row -> data.parkingByStopPlaceNetexId.computeIfAbsent((String) row[0], id -> new ArrayList<>()).add((String) row[1]));
        });
    }

    private String buildStopPlacesCsv(List<ReportRow> rows, List<String> columns, ReportData data) {
        StringBuilder csv = new StringBuilder(BOM).append(String.join(DELIMITER, columns));
        for (ReportRow row : rows) {
            StopPlaceData stopPlace = data.stopPlaces.get(row.stopPlaceId);
            if (stopPlace == null) {
                continue;
            }
            StopPlaceData parent = row.parentId == null ? null : data.stopPlaces.get(row.parentId);
            List<QuayData> quays = stopPlace.parent ? List.of() : data.quaysOf(stopPlace);
            csv.append(LINE_SEPARATOR).append(columns.stream()
                    .map(column -> cell(stopPlaceValue(column, row, stopPlace, parent, quays, data)))
                    .collect(Collectors.joining(DELIMITER)));
        }
        return csv.toString();
    }

    private Object stopPlaceValue(String column, ReportRow row, StopPlaceData stopPlace, StopPlaceData parent, List<QuayData> quays, ReportData data) {
        switch (column) {
            case "name":
                return rowName(stopPlace, parent);
            case "modality":
                if (stopPlace.parent) {
                    return data.childrenOf(stopPlace).stream()
                            .map(child -> StringUtils.defaultString(child.stopPlaceType))
                            .collect(Collectors.joining(","));
                }
                return stopPlace.stopPlaceType;
            case "id":
                return stopPlace.netexId;
            case "county":
                return topographicPlaceName(stopPlace, parent, data, 1);
            case "muncipality":
                return topographicPlaceName(stopPlace, parent, data, 0);
            case "importedId":
                return String.join(",", stopPlace.importedIds);
            case "importedName":
                return String.join(",", stopPlace.importedNames);
            case "position":
                return position(stopPlace.latitude, stopPlace.longitude);
            case "inseeCode":
                return inseeCode(stopPlace, data);
            case "mergeId":
                return data.mergeGroups == null ? null : data.mergeGroups.get(stopPlace.netexId);
            case "provider":
                return stopPlace.provider;
            case "quays":
                return quays.stream().map(quay -> quay.netexId).collect(Collectors.joining(","));
            case "parking":
                return String.join(",", data.parkingFor(row));
            case "wheelchairAccess":
                return stopPlaceAccessibility(stopPlace, quays, data).wheelchairAccess;
            case "stepFreeAccess":
                return stopPlaceAccessibility(stopPlace, quays, data).stepFreeAccess;
            case "shelterEquipment":
                return data.hasEquipment(stopPlace.placeEquipmentId, SHELTER_EQUIPMENT_TYPE);
            case "waitingRoomEquipment":
                return data.hasEquipment(stopPlace.placeEquipmentId, WAITING_ROOM_EQUIPMENT_TYPE);
            case "sanitaryEquipment":
                return data.hasEquipment(stopPlace.placeEquipmentId, SANITARY_EQUIPMENT_TYPE);
            case "generalSign":
                return data.transportModeSigns(stopPlace.placeEquipmentId);
            case "tags":
                return String.join(",", data.tagsByNetexId.getOrDefault(stopPlace.netexId, List.of()));
            default:
                throw new IllegalArgumentException("Unknown stop place column " + column);
        }
    }

    private String buildQuaysCsv(List<ReportRow> rows, List<String> columns, ReportData data) {
        StringBuilder csv = new StringBuilder(BOM).append(String.join(DELIMITER, columns));
        for (ReportRow row : rows) {
            StopPlaceData stopPlace = data.stopPlaces.get(row.stopPlaceId);
            if (stopPlace == null || stopPlace.parent) {
                continue;
            }
            StopPlaceData parent = row.parentId == null ? null : data.stopPlaces.get(row.parentId);
            List<QuayData> quays = data.quaysOf(stopPlace);
            Limitations stopPlaceAccessibility = stopPlaceAccessibility(stopPlace, quays, data);
            for (QuayData quay : quays) {
                Limitations quayAccessibility = quay.accessibilityAssessmentId == null
                        ? stopPlaceAccessibility
                        : data.limitationsOfAssessment(quay.accessibilityAssessmentId);
                csv.append(LINE_SEPARATOR).append(columns.stream()
                        .map(column -> cell(quayValue(column, stopPlace, parent, quay, quayAccessibility, data)))
                        .collect(Collectors.joining(DELIMITER)));
            }
        }
        return csv.toString();
    }

    private Object quayValue(String column, StopPlaceData stopPlace, StopPlaceData parent, QuayData quay, Limitations accessibility, ReportData data) {
        switch (column) {
            case "stopPlaceId":
                return stopPlace.netexId;
            case "stopPlaceName":
                return rowName(stopPlace, parent);
            case "id":
                return quay.netexId;
            case "importedId":
                return String.join(",", quay.importedIds);
            case "importedName":
                return String.join(",", quay.importedNames);
            case "position":
                return position(quay.latitude, quay.longitude);
            case "privateCode":
                return quay.privateCode;
            case "publicCode":
                return quay.publicCode;
            case "wheelchairAccess":
                return accessibility.wheelchairAccess;
            case "stepFreeAccess":
                return accessibility.stepFreeAccess;
            case "shelterEquipment":
                return data.hasEquipment(quay.placeEquipmentId, SHELTER_EQUIPMENT_TYPE);
            case "waitingRoomEquipment":
                return data.hasEquipment(quay.placeEquipmentId, WAITING_ROOM_EQUIPMENT_TYPE);
            case "sanitaryEquipment":
                return data.hasEquipment(quay.placeEquipmentId, SANITARY_EQUIPMENT_TYPE);
            case "generalSign":
                return data.transportModeSigns(quay.placeEquipmentId);
            default:
                throw new IllegalArgumentException("Unknown quay column " + column);
        }
    }

    private static String rowName(StopPlaceData stopPlace, StopPlaceData parent) {
        if (StringUtils.isEmpty(stopPlace.name) && parent != null) {
            return parent.name;
        }
        return stopPlace.name;
    }

    private static String topographicPlaceName(StopPlaceData stopPlace, StopPlaceData parent, ReportData data, int index) {
        String[] names = data.topographicPlaces.get(stopPlace.topographicPlaceId);
        String name = names == null ? null : names[index];
        if (StringUtils.isEmpty(name) && parent != null) {
            String[] parentNames = data.topographicPlaces.get(parent.topographicPlaceId);
            return parentNames == null ? null : parentNames[index];
        }
        return name;
    }

    private static String inseeCode(StopPlaceData stopPlace, ReportData data) {
        List<StopPlaceData> stopPlaces = new ArrayList<>();
        stopPlaces.add(stopPlace);
        if (stopPlace.parent) {
            stopPlaces.addAll(data.childrenOf(stopPlace));
        }
        return stopPlaces.stream()
                .flatMap(sp -> data.quaysOf(sp).stream())
                .map(quay -> quay.inseeCode)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElse(null);
    }

    private static Limitations stopPlaceAccessibility(StopPlaceData stopPlace, List<QuayData> quays, ReportData data) {
        if (stopPlace.accessibilityAssessmentId != null) {
            return data.limitationsOfAssessment(stopPlace.accessibilityAssessmentId);
        }
        return new Limitations(
                limitationFromQuays(quays, data, limitations -> limitations.wheelchairAccess),
                limitationFromQuays(quays, data, limitations -> limitations.stepFreeAccess));
    }

    private static String limitationFromQuays(List<QuayData> quays, ReportData data, Function<Limitations, String> limitationGetter) {
        if (quays.isEmpty()) {
            return UNKNOWN;
        }
        boolean isUnknown = false;
        boolean isPartial = false;
        int trueCount = 0;
        int falseCount = 0;
        for (QuayData quay : quays) {
            Limitations limitations = quay.accessibilityAssessmentId == null ? null : data.limitationsByAssessmentId.get(quay.accessibilityAssessmentId);
            String limitation = limitations == null ? null : limitationGetter.apply(limitations);
            if (UNKNOWN.equals(limitation)) {
                isUnknown = true;
            } else if (PARTIAL.equals(limitation)) {
                isPartial = true;
            } else if (TRUE.equals(limitation)) {
                trueCount++;
            } else if (FALSE.equals(limitation)) {
                falseCount++;
            }
        }
        if (isUnknown) return UNKNOWN;
        if (isPartial) return PARTIAL;
        if (trueCount == quays.size()) return TRUE;
        if (falseCount == quays.size()) return FALSE;
        if (trueCount > 0 && falseCount > 0) return PARTIAL;
        return UNKNOWN;
    }

    private static String position(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return null;
        }
        return jsDecimalPrecision(latitude) + "," + jsDecimalPrecision(longitude);
    }

    static String jsDecimalPrecision(double number) {
        String integerPart = BigDecimal.valueOf(number).toPlainString().split("\\.")[0];
        BigDecimal rounded = new BigDecimal(number).round(new MathContext(integerPart.length() + 6, RoundingMode.HALF_UP));
        double value = rounded.doubleValue();
        if (value == Math.rint(value) && Math.abs(value) < 1e15) {
            return String.valueOf((long) value);
        }
        return BigDecimal.valueOf(value).toPlainString();
    }

    private String cell(Object value) {
        if (value == null || Boolean.FALSE.equals(value) || (value instanceof String string && string.isEmpty())) {
            return "\"\"";
        }
        if (Boolean.TRUE.equals(value)) {
            return "true";
        }
        try {
            return objectMapper.writeValueAsString(value.toString());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to write CSV value " + value, e);
        }
    }

    private static <T> void forEachChunk(Collection<T> values, Consumer<List<T>> chunkConsumer) {
        List<T> list = new ArrayList<>(values);
        for (int i = 0; i < list.size(); i += SQL_IN_CHUNK_SIZE) {
            chunkConsumer.accept(list.subList(i, Math.min(i + SQL_IN_CHUNK_SIZE, list.size())));
        }
    }

    private static void addIfNonNull(Set<Long> ids, Long id) {
        if (id != null) {
            ids.add(id);
        }
    }

    private static Long toLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private static Double toDouble(Object value) {
        return value == null ? null : ((Number) value).doubleValue();
    }

    private static BigDecimal toNumberOrNull(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private record SearchHit(Long id, String netexId, Long version, boolean parent, String parentSiteRef, String parentSiteRefVersion) {
    }

    private record ReportRow(Long stopPlaceId, boolean parent, Long parentId) {
    }

    private static class ImportedData {
        final List<String> importedIds = new ArrayList<>();
        final List<String> importedNames = new ArrayList<>();
    }

    private static class StopPlaceData extends ImportedData {
        Long id;
        String netexId;
        String name;
        String stopPlaceType;
        boolean parent;
        String provider;
        Double latitude;
        Double longitude;
        Long topographicPlaceId;
        Long accessibilityAssessmentId;
        Long placeEquipmentId;
    }

    private static class QuayData extends ImportedData {
        Long id;
        String netexId;
        String publicCode;
        String privateCode;
        Double latitude;
        Double longitude;
        Long accessibilityAssessmentId;
        Long placeEquipmentId;
        String inseeCode;
    }

    private record Limitations(String wheelchairAccess, String stepFreeAccess) {
    }

    private record Equipment(String type, String signContentType, String privateCode) {
    }

    private static class ReportData {
        final Map<Long, StopPlaceData> stopPlaces = new HashMap<>();
        final Map<Long, List<QuayData>> quaysByStopPlaceId = new HashMap<>();
        final Map<Long, List<Long>> childrenIdsByParentId = new HashMap<>();
        final Map<Long, String[]> topographicPlaces = new HashMap<>();
        final Map<Long, Limitations> limitationsByAssessmentId = new HashMap<>();
        final Map<Long, List<Equipment>> equipmentsByPlaceEquipmentId = new HashMap<>();
        final Map<String, List<String>> tagsByNetexId = new HashMap<>();
        final Map<String, List<String>> parkingByStopPlaceNetexId = new HashMap<>();
        boolean parkingLoaded;
        Map<String, String> mergeGroups;

        List<QuayData> quaysOf(StopPlaceData stopPlace) {
            return quaysByStopPlaceId.getOrDefault(stopPlace.id, List.of());
        }

        Map<Long, QuayData> quaysById() {
            Map<Long, QuayData> quaysById = new HashMap<>();
            quaysByStopPlaceId.values().forEach(quays -> quays.forEach(quay -> quaysById.put(quay.id, quay)));
            return quaysById;
        }

        List<StopPlaceData> childrenOf(StopPlaceData parent) {
            return childrenIdsByParentId.getOrDefault(parent.id, List.of()).stream()
                    .map(stopPlaces::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        Limitations limitationsOfAssessment(Long assessmentId) {
            return limitationsByAssessmentId.getOrDefault(assessmentId, new Limitations(UNKNOWN, UNKNOWN));
        }

        List<String> parkingFor(ReportRow row) {
            StopPlaceData stopPlace = stopPlaces.get(row.stopPlaceId());
            if (!parkingLoaded || stopPlace == null || stopPlace.parent) {
                return List.of();
            }
            return parkingByStopPlaceNetexId.getOrDefault(stopPlace.netexId, List.of());
        }

        boolean hasEquipment(Long placeEquipmentId, String equipmentType) {
            return placeEquipmentId != null && equipmentsByPlaceEquipmentId.getOrDefault(placeEquipmentId, List.of()).stream()
                    .anyMatch(equipment -> equipmentType.equals(equipment.type()));
        }

        String transportModeSigns(Long placeEquipmentId) {
            if (placeEquipmentId == null) {
                return null;
            }
            return equipmentsByPlaceEquipmentId.getOrDefault(placeEquipmentId, List.of()).stream()
                    .filter(equipment -> GENERAL_SIGN_TYPE.equals(equipment.type()))
                    .filter(equipment -> TRANSPORT_MODE_SIGN_CONTENT.equals(equipment.signContentType()))
                    .map(Equipment::privateCode)
                    .filter(StringUtils::isNotEmpty)
                    .collect(Collectors.joining(";"));
        }
    }
}
