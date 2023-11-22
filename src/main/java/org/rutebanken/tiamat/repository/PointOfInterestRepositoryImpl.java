package org.rutebanken.tiamat.repository;

import org.hibernate.Hibernate;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.rutebanken.tiamat.model.*;
import org.rutebanken.tiamat.repository.iterator.ScrollableResultIterator;
import org.rutebanken.tiamat.repository.search.SearchHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.math.BigInteger;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@Transactional
public class PointOfInterestRepositoryImpl implements PointOfInterestRepositoryCustom{

    protected static final String SQL_MAX_VERSION_OF_POI = "p.version = (select max(pv.version) from point_of_interest pv where pv.netex_id = p.netex_id) ";

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private GeometryFactory geometryFactory;

    @Autowired
    private SearchHelper searchHelper;

    private static final Logger logger = LoggerFactory.getLogger(ParkingRepositoryImpl.class);

    private Pair<String, Map<String, Object>> getPointsOfInterest() {
        String sql = "SELECT p.* FROM point_of_interest p WHERE " +
                SQL_MAX_VERSION_OF_POI +
                "ORDER BY p.netex_id, p.version";
        return Pair.of(sql, new HashMap<String, Object>(0));
    }

    @Override
    public int countResult() {
        return countResult(getPointsOfInterest());
    }

    private int countResult(Pair<String, Map<String, Object>> sqlWithParams) {
        Session session = entityManager.unwrap(Session.class);
        NativeQuery query = session.createNativeQuery("SELECT COUNT(*) from (" + sqlWithParams.getFirst() + ") as numberOfPointsOfInterest");
        searchHelper.addParams(query, sqlWithParams.getSecond());
        return ((BigInteger) query.uniqueResult()).intValue();
    }

    @Override
    public Iterator<PointOfInterest> scrollPointsOfInterest() {
        return scrollPointsOfInterest(getPointsOfInterest());
    }

    private Iterator<PointOfInterest> scrollPointsOfInterest(Pair<String, Map<String, Object>> sqlWithParams) {
        final int fetchSize = 800;

        Session session = entityManager.unwrap(Session.class);
        NativeQuery query = session.createNativeQuery(sqlWithParams.getFirst());
        searchHelper.addParams(query, sqlWithParams.getSecond());

        query.addEntity(PointOfInterest.class);
        query.setReadOnly(true);
        query.setFetchSize(fetchSize);
        query.setCacheable(false);
        ScrollableResults results = query.scroll(ScrollMode.FORWARD_ONLY);

        ScrollableResultIterator<PointOfInterest> pointOfInterestEntityIterator = new ScrollableResultIterator<>(results, fetchSize, session);

        return pointOfInterestEntityIterator;
    }

    @Override
    public String findFirstByKeyValues(String key, Set<String> values) {
        Query query = entityManager.createNativeQuery("SELECT p.netex_id " +
                "FROM point_of_interest p " +
                "INNER JOIN point_of_interest_key_values pkv " +
                "ON pkv.point_of_interest_id = p.id " +
                "INNER JOIN value_items v " +
                "ON pkv.key_values_id = v.value_id " +
                "WHERE pkv.key_values_key = :key " +
                "AND v.items IN ( :values ) " +
                "AND p.version = (SELECT MAX(pv.version) FROM point_of_interest pv WHERE pv.netex_id = p.netex_id)");

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


    public void clearAllPois(){
        entityManager.createNativeQuery("TRUNCATE TABLE point_of_interest_facility_set CASCADE").executeUpdate();
    }

    @Override
    public void clearPOIForClassification(String classificationName) {

        Query query = entityManager.createNativeQuery("SELECT delete_poi_for_classification(:className)");
        query.setParameter("className", classificationName);
        query.getSingleResult();
    }

    @Override
    public void clearPOIExceptClassification(String classificationName) {
        Query query = entityManager.createNativeQuery("SELECT delete_poi_except_classification(:className)");
        query.setParameter("className", classificationName);
        query.getSingleResult();
    }

    @Override
    public List<PointOfInterest> findAllAndInitialize() {

        Query query = entityManager.createNativeQuery("SELECT p.* " +
                "FROM point_of_interest p ", PointOfInterest.class);

        List<PointOfInterest> resultList = query.getResultList();

        for (PointOfInterest poi : resultList) {
            Hibernate.initialize(poi.getClassifications());
        }

        return resultList;
    }

    /**
     * Initialize export job table with points of interest ids that must be exported
     *
     * @param exportJobId id of the export job
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void initExportJobTable(Long exportJobId) {

        Map<String, Object> parameters = new HashMap<>();

        String queryStr = "INSERT INTO export_job_id_list \n" +
                " SELECT :exportJobId, req1.poi_id     \n" +
                " FROM ( \n" +
                " SELECT MAX(poi.id) AS poi_id, MAX(poi.version) AS version FROM point_of_interest poi WHERE (poi.from_date <= :pointInTime OR poi.from_date IS NULL) \n" +
                " AND (poi.to_date >= :pointInTime OR poi.to_date IS NULL) GROUP BY poi.netex_id) req1";


        parameters.put("exportJobId", exportJobId);
        parameters.put("pointInTime", Date.from(Instant.now()));

        Session session = entityManager.unwrap(Session.class);
        NativeQuery query = session.createNativeQuery(queryStr);
        searchHelper.addParams(query, parameters);

        query.executeUpdate();
    }

    @Override
    public PointOfInterest findFirstByNetexIdOrderByVersionDescAndInitialize(String netexId){
        String sql = "SELECT poi.* FROM point_of_interest poi WHERE poi.netex_id = :netexId AND poi.version = (SELECT max(poi2.version) FROM point_of_interest poi2 WHERE poi2.netex_id = :netexId)";

        Query pointOfInterestTypedQuery = entityManager.createNativeQuery(sql, PointOfInterest.class);

        pointOfInterestTypedQuery.setParameter("netexId",netexId);

        List<PointOfInterest> results = pointOfInterestTypedQuery.getResultList();

        results.forEach(pointOfInterest -> {
            Hibernate.initialize(pointOfInterest.getAlternativeNames());
            Hibernate.initialize(pointOfInterest.getClassifications());
            Hibernate.initialize(pointOfInterest.getEquipmentPlaces());
            Hibernate.initialize(pointOfInterest.getPolygon());
            Hibernate.initialize(pointOfInterest.getAccessibilityAssessment());
            if (pointOfInterest.getAccessibilityAssessment() != null){
                Hibernate.initialize(pointOfInterest.getAccessibilityAssessment().getLimitations());
            }

            pointOfInterest.getClassifications().forEach(this::initializeRecursivelyClassification);

            Hibernate.initialize(pointOfInterest.getKeyValues());
            pointOfInterest.getKeyValues().values().forEach(value -> Hibernate.initialize(value.getItems()));
        });
        return results.isEmpty() ? null : results.get(0);
    }

    private void initializeRecursivelyClassification(PointOfInterestClassification classification){
        Hibernate.initialize(classification.getKeyValues());
        classification.getKeyValues().values().forEach(value -> Hibernate.initialize(value.getItems()));
        Hibernate.initialize(classification.getParent());

        if (classification.getParent() != null){
            initializeRecursivelyClassification(classification.getParent());
        }

    }

    @Override
    public int countPOIInExport(Long exportJobId) {
        String queryString = "select count(*) FROM export_job_id_list WHERE job_id = :exportJobId";
        return ((Number)entityManager.createNativeQuery(queryString).setParameter("exportJobId", exportJobId).getSingleResult()
        ).intValue();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteProcessedIds(Long exportJobId, Set<Long> processedPoi) {
        Session session = entityManager.unwrap(Session.class);
        String queryStr = "DELETE FROM export_job_id_list WHERE job_id = :exportJobId AND exported_object_id IN :poiIdList";
        NativeQuery query = session.createNativeQuery(queryStr);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("exportJobId", exportJobId);
        parameters.put("poiIdList", processedPoi);
        searchHelper.addParams(query, parameters);
        query.executeUpdate();
    }

    public List<PointOfInterest> getPOIInitializedForExport(Set<Long> poiIds) {

        Set<String> poiIdStrings = poiIds.stream().map(String::valueOf).collect(Collectors.toSet());

        String joinedPoiIds = String.join(",", poiIdStrings);
        String sql = "SELECT poi FROM PointOfInterest poi WHERE poi.id IN(" + joinedPoiIds + ")";

        TypedQuery<PointOfInterest> pointOfInterestTypedQuery = entityManager.createQuery(sql, PointOfInterest.class);

        List<PointOfInterest> results = pointOfInterestTypedQuery.getResultList();

        results.forEach(pointOfInterest -> {
            Hibernate.initialize(pointOfInterest.getAlternativeNames());
            Hibernate.initialize(pointOfInterest.getClassifications());
            Hibernate.initialize(pointOfInterest.getEquipmentPlaces());
            if (pointOfInterest.getPlaceEquipments() != null) {
                Hibernate.initialize(pointOfInterest.getPlaceEquipments().getInstalledEquipment());
            }
            Hibernate.initialize(pointOfInterest.getPolygon());
            Hibernate.initialize(pointOfInterest.getAccessibilityAssessment());
            if (pointOfInterest.getAccessibilityAssessment() != null){
                Hibernate.initialize(pointOfInterest.getAccessibilityAssessment().getLimitations());
            }

            pointOfInterest.getClassifications().forEach(this::initializeRecursivelyClassification);

            Hibernate.initialize(pointOfInterest.getKeyValues());
            pointOfInterest.getKeyValues().values().forEach(value -> Hibernate.initialize(value.getItems()));

            if (pointOfInterest.getPointOfInterestOpeningHours() != null){
                Hibernate.initialize(pointOfInterest.getPointOfInterestOpeningHours());
                if (pointOfInterest.getPointOfInterestOpeningHours().getDaysType() != null) {
                    Hibernate.initialize(pointOfInterest.getPointOfInterestOpeningHours().getDaysType());
                    pointOfInterest.getPointOfInterestOpeningHours().getDaysType().forEach(dayType -> {
                        Hibernate.initialize(dayType.getDays());
                        if (dayType.getTimeBand() != null){
                            Hibernate.initialize(dayType.getTimeBand());
                            dayType.getTimeBand().forEach(timeBand -> {
                                Hibernate.initialize(timeBand.getEndTime());
                                Hibernate.initialize(timeBand.getStartTime());
                            });
                        }
                    });
                }
            }
        });

        return results;
    }

    /**
     * Get a batch of object to process
     * @param exportJobId
     * @return
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Set<Long> getNextBatchToProcess(Long exportJobId){
        Session session = entityManager.unwrap(Session.class);
        NativeQuery query = session.createNativeQuery("SELECT exported_object_id FROM export_job_id_list WHERE job_id  = :exportJobId LIMIT 1000");

        query.setParameter("exportJobId", exportJobId);

        Set<Long> result = new HashSet<>();
        for(Object object : query.list()) {
            BigInteger bigInteger = (BigInteger) object;
            result.add(bigInteger.longValue());
        }
        return result;
    }

    @Override
    public Page<PointOfInterest> findByName(String name, Pageable pageable){
        String queryString = "SELECT * FROM point_of_interest p " +
                "WHERE p.parent_site_ref IS NULL " +
                "AND p.version = (SELECT MAX(pv.version) FROM point_of_interest pv WHERE pv.netex_id = p.netex_id) " +
                (name != null ? "AND LOWER(p.name_value) LIKE concat('%', LOWER(:name), '%')":"");


        logger.debug("Finding point of interest by similarity name: {}", queryString);

        final Query query = entityManager.createNativeQuery(queryString, PointOfInterest.class);

        if(query != null){
            query.setParameter("name", name);
        }

        query.setFirstResult(Math.toIntExact(pageable.getOffset()));
        query.setMaxResults(pageable.getPageSize());
        List<PointOfInterest> pointsOfInterest = query.getResultList();
        return new PageImpl<>(pointsOfInterest, pageable, pointsOfInterest.size());
    }

    @Override
    public Page<PointOfInterest> findByClassifications(List<String> classifications, Pageable pageable) {
        String queryString = "SELECT * FROM point_of_interest p WHERE id IN (" +
                                "SELECT point_of_interest_id FROM point_of_interest_classifications poic2 WHERE classifications_id IN (" +
                                    "SELECT id FROM point_of_interest_classification poic WHERE LOWER(name_value) LIKE concat('%', LOWER(:classification), '%')));";

        logger.debug("Finding poi by classification: {}", queryString);
        final Query query = entityManager.createNativeQuery(queryString, PointOfInterest.class);

        List<PointOfInterest> pointsOfInterest = new ArrayList<>();
        for (String classification : classifications) {
            if (query != null) {
                query.setParameter("classification", classification);
            }
            query.setFirstResult(Math.toIntExact(pageable.getOffset()));
            query.setMaxResults(pageable.getPageSize());
            pointsOfInterest.addAll(query.getResultList());
        }

        return new PageImpl<>(pointsOfInterest, pageable, pointsOfInterest.size());
    }

    @Override
    public Page<PointOfInterest> findNearbyPOI(Envelope envelope, String name, String ignorePointOfInterestId, Pageable pageable) {
        Geometry geometryFilter = geometryFactory.toGeometry(envelope);

        String queryString = "SELECT * FROM point_of_interest p " +
                "WHERE ST_within(p.centroid, :filter) = true " +
                "AND p.parent_site_ref IS NULL " +
                "AND p.version = (SELECT MAX(pv.version) FROM point_of_interest pv WHERE pv.netex_id = p.netex_id) " +
                (name != null ? "AND p.name_value = :name":"") +
                (ignorePointOfInterestId != null ? " AND (p.netex_id != :ignorePointOfInterestId)":"");


        logger.debug("Finding point of interest within bounding box with query: {}", queryString);

        final Query query = entityManager.createNativeQuery(queryString, PointOfInterest.class);
        query.setParameter("filter", geometryFilter);

        if(name != null){
            query.setParameter("name", name);
        }

        if(ignorePointOfInterestId != null) {
            query.setParameter("ignorePointOfInterestId", ignorePointOfInterestId);
        }

        query.setFirstResult(Math.toIntExact(pageable.getOffset()));
        query.setMaxResults(pageable.getPageSize());
        List<PointOfInterest> pointsOfInterest = query.getResultList();
        return new PageImpl<>(pointsOfInterest, pageable, pointsOfInterest.size());
    }

    @Override
    public String findNearbyPOI(Envelope envelope, String name) {
        Geometry geometryFilter = geometryFactory.toGeometry(envelope);

        TypedQuery<String> query = entityManager
                .createQuery("SELECT p.netexId FROM PointOfInterest p " +
                                "WHERE within(p.centroid, :filter) = true " +
                                "AND p.version = (SELECT MAX(pv.version) FROM PointOfInterest pv WHERE pv.netexId = p.netexId) " +
                                "AND p.name.value = :name ", String.class);

        query.setParameter("filter", geometryFilter);
        query.setParameter("name", name);

        return getOneOrNull(query);
    }

    private <T> T getOneOrNull(TypedQuery<T> query) {
        try {
            List<T> resultList = query.getResultList();
            return resultList.isEmpty() ? null : resultList.get(0);
        } catch (NoResultException e) {
            return null;
        }
    }

}
