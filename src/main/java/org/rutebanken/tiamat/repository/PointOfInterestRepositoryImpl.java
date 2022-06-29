package org.rutebanken.tiamat.repository;

import org.hibernate.Hibernate;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.rutebanken.tiamat.model.ParkingArea;
import org.rutebanken.tiamat.model.PointOfInterest;
import org.rutebanken.tiamat.repository.iterator.ScrollableResultIterator;
import org.rutebanken.tiamat.repository.search.SearchHelper;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@Transactional
public class PointOfInterestRepositoryImpl implements PointOfInterestRepositoryCustom{

    protected static final String SQL_MAX_VERSION_OF_POI = "p.version = (select max(pv.version) from point_of_interest pv where pv.netex_id = p.netex_id) ";

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private SearchHelper searchHelper;

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
            Hibernate.initialize(pointOfInterest.getKeyValues());
            pointOfInterest.getKeyValues().values().forEach(value -> Hibernate.initialize(value.getItems()));
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

}
