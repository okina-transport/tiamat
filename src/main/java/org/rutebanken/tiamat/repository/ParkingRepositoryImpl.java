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

package org.rutebanken.tiamat.repository;

import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Hibernate;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.rutebanken.tiamat.geo.GeometryTransformer;
import org.rutebanken.tiamat.model.*;
import org.rutebanken.tiamat.repository.iterator.ScrollableResultIterator;
import org.rutebanken.tiamat.repository.search.SearchHelper;
import org.rutebanken.tiamat.rest.dto.DTOClusterMarker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@Transactional
public class ParkingRepositoryImpl implements ParkingRepositoryCustom {

    /**
     * When selecting parkings and there are multiple versions of the same parking by netex_id, and you only need the highest version by number.
     */
    protected static final String SQL_MAX_VERSION_OF_PARKING = "p.version = (select max(pv.version) from parking pv where pv.netex_id = p.netex_id) ";
    private static final Logger logger = LoggerFactory.getLogger(ParkingRepositoryImpl.class);
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private GeometryFactory geometryFactory;
    @Autowired
    private SearchHelper searchHelper;


    @Value("${cluster.marker.maximum.distance:10000}")
    protected long maximumDistance;

    /**
     * Find parking's netex ID by key value
     *
     * @param key    key in key values for parking
     * @param values list of values to check for
     * @return parking's netex ID
     */
    @Override
    public String findFirstByKeyValues(String key, Set<String> values) {

        Query query = entityManager.createNativeQuery("SELECT p.netex_id " +
                "FROM parking p " +
                "INNER JOIN parking_key_values pkv " +
                "ON pkv.parking_id = p.id " +
                "INNER JOIN value_items v " +
                "ON pkv.key_values_id = v.value_id " +
                "WHERE pkv.key_values_key = :key " +
                "AND v.items IN ( :values ) " +
                "AND p.version = (SELECT MAX(pv.version) FROM parking pv WHERE pv.netex_id = p.netex_id)");

        query.setParameter("key", key);
        query.setParameter("values", values);

        try {
            @SuppressWarnings("unchecked")
            List<String> results = query.getResultList();
            if (results.isEmpty()) {
                return null;
            } else {
                return results.get(0);
            }
        } catch (NoResultException noResultException) {
            return null;
        }
    }

    @Override
    public Set<Long> scrollParkings() {
        Iterator<Parking> ip = scrollParkings(getParkings());
        Set<Long> result = new HashSet<>();
        while (ip.hasNext()) {
            result.add(ip.next().getId());
        }
        return result;
    }

    @Override
    public Iterator<Parking> scrollParkings(Set<Long> stopPlaceIds) {
        return scrollParkings(getParkingsByStopPlaceIdsSQL(stopPlaceIds));
    }

    @Override
    public int countResult(Set<Long> stopPlaceIds) {
        if (stopPlaceIds == null || stopPlaceIds.isEmpty()) {
            return 0;
        }
        return countResult(getParkingsByStopPlaceIdsSQL(stopPlaceIds));
    }

    @Override
    public int countResult() {
        return countResult(getParkings());
    }

    private int countResult(Pair<String, Map<String, Object>> sqlWithParams) {
        Session session = entityManager.unwrap(Session.class);
        NativeQuery query = session.createNativeQuery("SELECT COUNT(*) from (" + sqlWithParams.getFirst() + ") as numberOfParkings");
        searchHelper.addParams(query, sqlWithParams.getSecond());
        return ((Long) query.uniqueResult()).intValue();
    }

    private Iterator<Parking> scrollParkings(Pair<String, Map<String, Object>> sqlWithParams) {
        final int fetchSize = 100;

        Session session = entityManager.unwrap(Session.class);
        NativeQuery query = session.createNativeQuery(sqlWithParams.getFirst());
        searchHelper.addParams(query, sqlWithParams.getSecond());

        query.addEntity(Parking.class);
        query.setReadOnly(true);
        query.setFetchSize(fetchSize);
        query.setCacheable(false);
        ScrollableResults results = query.scroll(ScrollMode.FORWARD_ONLY);

        ScrollableResultIterator<Parking> parkingEntityIterator = new ScrollableResultIterator<>(results, fetchSize, session);

        return parkingEntityIterator;
    }

    private Pair<String, Map<String, Object>> getParkingsByStopPlaceIdsSQL(Set<Long> stopPlaceIds) {

        String sql = "SELECT p.* " +
                "FROM (SELECT p2.id, " +
                "           p2.netex_id, " +
                "           p2.version " +
                "      FROM parking p2 " +
                "      INNER JOIN stop_place sp " +
                "           ON sp.netex_id = p2.parent_site_ref " +
                "           AND ( Cast(sp.version AS TEXT) = " +
                "                   p2.parent_site_ref_version " +
                "                 OR p2.parent_site_ref_version IS NULL ) " +
                "      WHERE sp.id in (" + StringUtils.join(stopPlaceIds, ',') +
                ')' +
                "   GROUP  BY p2.id) p2 " +
                "JOIN parking p " +
                "ON p2.id = p.id " +
                "WHERE " +
                SQL_MAX_VERSION_OF_PARKING +
                "ORDER BY p.netex_id, p.version";

        return Pair.of(sql, new HashMap<String, Object>(0));
    }

    private Pair<String, Map<String, Object>> getParkings() {
        String sql = "SELECT p.* FROM parking p WHERE " +
                SQL_MAX_VERSION_OF_PARKING +
                "ORDER BY p.netex_id, p.version";
        return Pair.of(sql, new HashMap<String, Object>(0));
    }

    @Override
    public Page<Parking> findNearbyParking(Envelope envelope, String name, ParkingTypeEnumeration parkingTypeEnumeration, String ignoreParkingId, Pageable pageable) {
        Geometry geometryFilter = geometryFactory.toGeometry(envelope);

        String queryString = "SELECT * FROM parking p " +
                "WHERE ST_within(p.centroid, :filter) = true " +
                "AND p.parent_site_ref IS NULL " +
                "AND p.version = (SELECT MAX(pv.version) FROM parking pv WHERE pv.netex_id = p.netex_id) " +
                (name != null ? "AND p.name_value = :name" : "") +
                (parkingTypeEnumeration != null ? " AND p.parking_type = :parkingType" : "") +
                (ignoreParkingId != null ? " AND (p.netex_id != :ignoreParkingId)" : "");


        logger.debug("Finding parking within bounding box with query: {}", queryString);

        final Query query = entityManager.createNativeQuery(queryString, Parking.class);
        query.setParameter("filter", geometryFilter);

        if (name != null) {
            query.setParameter("name", name);
        }
        if (parkingTypeEnumeration != null) {
            query.setParameter("parkingType", parkingTypeEnumeration);
        }
        if (ignoreParkingId != null) {
            query.setParameter("ignoreParkingId", ignoreParkingId);
        }

        query.setFirstResult(Math.toIntExact(pageable.getOffset()));
        query.setMaxResults(pageable.getPageSize());
        List<Parking> parkings = query.getResultList();
        return new PageImpl<>(parkings, pageable, parkings.size());
    }

    @Override
    public String findNearbyParking(Envelope envelope, String name, ParkingTypeEnumeration parkingTypeEnumeration) {
        Geometry geometryFilter = geometryFactory.toGeometry(envelope);

        TypedQuery<String> query = entityManager
                .createQuery("SELECT p.netexId FROM Parking p " +
                                "WHERE within(p.centroid, :filter) = true " +
                                "AND p.version = (SELECT MAX(pv.version) FROM Parking pv WHERE pv.netexId = p.netexId) " +
                                "AND p.name.value = :name " +
                                (parkingTypeEnumeration != null ? "AND p.parkingType = :parkingType" : ""),
                        String.class);

        query.setParameter("filter", geometryFilter);
        query.setParameter("name", name);
        if (parkingTypeEnumeration != null) {
            query.setParameter("parkingType", parkingTypeEnumeration);
        }
        return getOneOrNull(query);
    }

    @Override
    public Page<Parking> findByName(String name, Pageable pageable) {
        String queryString = "SELECT * FROM parking p " +
                "WHERE p.parent_site_ref IS NULL " +
                "AND p.version = (SELECT MAX(pv.version) FROM parking pv WHERE pv.netex_id = p.netex_id) " +
                (name != null ? "AND LOWER(p.name_value) LIKE concat('%', LOWER(:name), '%')" : "");


        logger.debug("Finding parking by similarity name: {}", queryString);

        final Query query = entityManager.createNativeQuery(queryString, Parking.class);

        if (query != null) {
            query.setParameter("name", name);
        }

        query.setFirstResult(Math.toIntExact(pageable.getOffset()));
        query.setMaxResults(pageable.getPageSize());
        List<Parking> parkings = query.getResultList();
        return new PageImpl<>(parkings, pageable, parkings.size());
    }

    @Override
    public Optional<Parking> findByIdLocAndOsm(String idLoc, String idOsm) {
        String queryString = "SELECT * FROM parking p ";

        if (StringUtils.isNotEmpty(idOsm)) {
            queryString = queryString + " INNER JOIN parking_key_values pkv on p.id=pkv.parking_id ";
        }

        if (StringUtils.isNotEmpty(idLoc)) {
            queryString = queryString + " INNER JOIN parking_key_values pkv2 on p.id=pkv2.parking_id ";
        }



        queryString = queryString + " WHERE p.parent_site_ref IS NULL " +
                " AND p.version = (SELECT MAX(pv.version) FROM parking pv WHERE pv.netex_id = p.netex_id) " ;


        if (StringUtils.isNotEmpty(idOsm)) {
            queryString = queryString + " AND pkv.key_values_key = 'id_osm'";
        }

        if (StringUtils.isNotEmpty(idLoc)) {
            queryString = queryString + " AND pkv2.key_values_key = 'id_local'";
        }



        if (StringUtils.isNotEmpty(idOsm)) {
            queryString = queryString + " AND EXISTS ( SELECT 1 FROM value_items vi WHERE vi.items = :idOsm AND pkv.key_values_id = vi.value_id)";
        } else {
            queryString = queryString + " AND NOT EXISTS ( SELECT 1 FROM parking_key_values pkv WHERE pkv.key_values_key = 'id_osm' AND pkv.parking_id = p.id)";
        }

        if (StringUtils.isNotEmpty(idLoc)) {
            queryString = queryString + " AND EXISTS ( SELECT 1 FROM value_items vi2 WHERE vi2.items = :idLoc AND pkv2.key_values_id = vi2.value_id)";
        } else {
            queryString = queryString + " AND NOT EXISTS ( SELECT 1 FROM parking_key_values pkv2 WHERE pkv2.key_values_key = 'id_local' AND pkv2.parking_id = p.id)";
        }

        logger.debug("Finding parking by idloc and idosm: {}", queryString);

        final Query query = entityManager.createNativeQuery(queryString, Parking.class);

        if (query != null) {
            query.setParameter("idLoc", idLoc);
        }

        if (StringUtils.isNotEmpty(idOsm)) {
            query.setParameter("idOsm", idOsm);
        }

        List<Parking> results = query.getResultList();
        if (results.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(results.get(0));

    }


    @Override
    public List<String> findByStopPlaceNetexId(String netexStopPlaceId) {

        String sql = "SELECT p.netex_id " +
                "FROM parking p " +
                "WHERE p.parent_site_ref = :netexStopPlaceId " +
                "AND p.version = (SELECT MAX(pv.version) FROM Parking pv WHERE pv.netex_id = p.netex_id) ";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("netexStopPlaceId", netexStopPlaceId);

        try {
            @SuppressWarnings("unchecked")
            List<String> results = query.getResultList();
            return results;

        } catch (NoResultException noResultException) {
            return new ArrayList<>();
        }
    }

    private <T> T getOneOrNull(TypedQuery<T> query) {
        try {
            List<T> resultList = query.getResultList();
            return resultList.isEmpty() ? null : resultList.get(0);
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Initialize export job table with stop ids that must be exported
     *
     * @param exportJobId id of the export job
     */
    @org.springframework.transaction.annotation.Transactional(propagation = Propagation.REQUIRES_NEW)
    public void initExportJobTable(Long exportJobId) {

        Map<String, Object> parameters = new HashMap<>();

        String queryStr = "INSERT INTO job_id_list \n" +
                " SELECT :exportJobId,req1.parking_id     \n" +
                " FROM ( \n" +
                " SELECT max(p.id)as parking_id,MAX(p.version) as version FROM parking p  WHERE  (p.from_date <= :pointInTime OR  p.from_date IS NULL) \n" +
                " AND (   p.to_date >= :pointInTime  OR p.to_date IS NULL) GROUP BY p.netex_id  ) req1";


        parameters.put("exportJobId", exportJobId);
        parameters.put("pointInTime", Date.from(Instant.now()));

        Session session = entityManager.unwrap(Session.class);
        NativeQuery query = session.createNativeQuery(queryStr);
        searchHelper.addParams(query, parameters);

        query.executeUpdate();

    }

    public List<Parking> getParkingsInitializedForExport(Set<Long> parkingIds) {

        Set<String> parkingIdStrings = parkingIds.stream().map(lvalue -> String.valueOf(lvalue)).collect(Collectors.toSet());

        String joinedParkingIds = String.join(",", parkingIdStrings);
        String sql = "SELECT p FROM Parking p WHERE p.id IN(" + joinedParkingIds +
                ")";


        TypedQuery<Parking> q = entityManager.createQuery(sql, Parking.class);

        List<Parking> results = q.getResultList();

        results.forEach(parking -> {

            Hibernate.initialize(parking.getPlaceEquipments());
            if (parking.getPlaceEquipments() != null) {
                Hibernate.initialize(parking.getPlaceEquipments().getInstalledEquipment());
            }
            Hibernate.initialize(parking.getParkingPaymentProcess());
            Hibernate.initialize(parking.getParkingVehicleTypes());
            Hibernate.initialize(parking.getAccessibilityAssessment());
            Hibernate.initialize(parking.getAlternativeNames());
            Hibernate.initialize(parking.getParkingAreas());
            Hibernate.initialize(parking.getParkingProperties());
            Hibernate.initialize(parking.getPolygon());
            Hibernate.initialize(parking.getTransportTypes());
            Hibernate.initialize(parking.getTypeOfPaymentMethods());
            Hibernate.initialize(parking.getPostalAddress());
            Hibernate.initialize(parking.getAvailabilityConditions());
            Hibernate.initialize(parking.getVehicleEntrances());

            if (parking.getAvailabilityConditions() != null){
                for (AvailabilityCondition availabilityCondition : parking.getAvailabilityConditions()) {
                    Hibernate.initialize(availabilityCondition.getDayTypes());

                    if (availabilityCondition.getDayTypes() != null){
                        for (DayType dayType : availabilityCondition.getDayTypes()) {
                            Hibernate.initialize(dayType.getTimeBand());
                        }
                    }
                }
            }

            if (parking.getParkingProperties() != null) {
                parking.getParkingProperties().forEach(parkProp -> {
                    Hibernate.initialize(parkProp.getSpaces());
                });
            }

            if (parking.getParkingAreas() != null) {
                for (ParkingArea parkingArea : parking.getParkingAreas()) {
                    Hibernate.initialize(parkingArea.getAlternativeNames());
                    Hibernate.initialize(parkingArea.getAccessibilityAssessment());
                    Hibernate.initialize(parkingArea.getKeyValues());
                    Hibernate.initialize(parkingArea.getBays());

                    parkingArea.getBays().forEach(bay -> {
                        bay.getKeyValues().values().forEach(value -> Hibernate.initialize(value.getItems()));
                        Hibernate.initialize(bay.getAlternativeNames());
                    });

                    parkingArea.getKeyValues().values().forEach(value -> Hibernate.initialize(value.getItems()));
                    Hibernate.initialize(parkingArea.getPolygon());

                    if (parkingArea.getAccessibilityAssessment() != null) {
                        Hibernate.initialize(parkingArea.getAccessibilityAssessment().getLimitations());
                    }
                }

            }

            if (parking.getParkingProperties() != null) {
                parking.getParkingProperties().forEach(parkProp -> {
                    Hibernate.initialize(parkProp.getSpaces());

                });
            }


            Hibernate.initialize(parking.getKeyValues());
            parking.getKeyValues().values().forEach(value -> Hibernate.initialize(value.getItems()));

            if (parking.getAccessibilityAssessment() != null) {
                Hibernate.initialize(parking.getAccessibilityAssessment().getLimitations());
            }

            if (CollectionUtils.isNotEmpty(parking.getTransportTypes())) {
                for (var transportType : parking.getTransportTypes()) {
                    Hibernate.initialize(transportType.getKeyValues());
                    transportType.getKeyValues().values().forEach(value -> Hibernate.initialize(value.getItems()));
                    Hibernate.initialize(transportType.getPassengerCapacity());
                    transportType.getPassengerCapacity().getKeyValues().values().forEach(value -> Hibernate.initialize(value.getItems()));
                }
            }

            if (CollectionUtils.isNotEmpty(parking.getTypeOfPaymentMethods())) {
                for (var typeOfPaymentMethod : parking.getTypeOfPaymentMethods()) {
                    Hibernate.initialize(typeOfPaymentMethod.getKeyValues());
                    typeOfPaymentMethod.getKeyValues().values().forEach(value -> Hibernate.initialize(value.getItems()));
                }
            }

        });
        return results;
    }

    public List<Parking> findAllParkingsLastVersionAndValid() {
        String sql = "SELECT p.* FROM parking p WHERE " +
                SQL_MAX_VERSION_OF_PARKING +
                "ORDER BY p.netex_id, p.version";
        return entityManager.createNativeQuery(sql, Parking.class).getResultList();
    }

    @Override
    public List<DTOClusterMarker> findClusterMarkers(){

        String completeQuery = """
                    SELECT cluster_id,
                           ST_X(ST_Centroid(ST_Collect(centroid))) AS center_lon,
                           ST_Y(ST_Centroid(ST_Collect(centroid))) AS center_lat,
                           count(1)
                    FROM (SELECT ST_ClusterDBSCAN(centroid,:maxDistanceDegrees , 1) OVER () AS cluster_id, * 
                          FROM (SELECT p.*
                                FROM parking p
                                WHERE p.version = (select max(pv.version) from parking pv where pv.netex_id = p.netex_id)
                                ORDER BY p.netex_id, p.version) single_poi) pois_with_clusters
                    group by cluster_id
            """;


        Query query = entityManager.createNativeQuery(completeQuery);
        query.setParameter("maxDistanceDegrees", GeometryTransformer.convertMetersToLatitudeDegrees(maximumDistance));


        List<Object[]> result = query.getResultList();

        List<DTOClusterMarker> clusterList = result.stream()
                .filter(tab -> tab[0] != null && tab[1]!= null && tab[3] != null)
                .map(DTOClusterMarker::new)
                .collect(Collectors.toList());

        return clusterList;
    }

    @Override
    public Integer findByIdLocForOtherParking(String idLoc, String netexId) {
        String queryString = "SELECT" +
                "                COUNT(*)" +
                "            FROM" +
                "                parking p" +
                "            WHERE EXISTS (" +
                "                SELECT 1" +
                "                FROM parking_key_values pkv" +
                "                JOIN value_items vi ON vi.value_id = pkv.key_values_id" +
                "                WHERE" +
                "                    pkv.parking_id = p.id" +
                "                    AND pkv.key_values_key = 'id_local'" +
                "                    AND vi.items = LOWER(:name)" +
                (netexId != null ? "AND LOWER(p.netex_id) != LOWER(:id)" : "") +
                "            );";

        logger.debug("Finding parking count by idloc: {}", idLoc);

        final Query query = entityManager.createNativeQuery(queryString);
        query.setParameter("name", idLoc.toLowerCase());

        if (netexId != null) {
            query.setParameter("id", netexId.toLowerCase());
        }

        Object result = query.getSingleResult();
        return ((Number) result).intValue();
    }

    @Override
    public List<Parking> getAllParkingsWithoutInsee() {
        String sql = "SELECT p.* FROM parking p " +
                "WHERE p.insee IS NULL OR p.insee = '';";

        Query q = entityManager.createNativeQuery(sql, Parking.class);

        return (List<Parking>) q.getResultList();
    }
}
