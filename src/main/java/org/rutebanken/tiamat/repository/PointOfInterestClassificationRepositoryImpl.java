package org.rutebanken.tiamat.repository;


import org.hibernate.Hibernate;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.rutebanken.tiamat.model.PointOfInterestClassification;
import org.rutebanken.tiamat.repository.iterator.ScrollableResultIterator;
import org.rutebanken.tiamat.repository.search.SearchHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import java.math.BigInteger;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;


@Repository
@Transactional
public class PointOfInterestClassificationRepositoryImpl implements PointOfInterestClassificationRepositoryCustom{

    protected static final String SQL_MAX_VERSION_OF_POIC = "pc.version = (SELECT MAX(pcv.version) FROM point_of_interest_classification pcv WHERE pcv.netex_id = pc.netex_id) ";

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private SearchHelper searchHelper;

    private Pair<String, Map<String, Object>> getPointOfInterestClassifications() {
        String sql = "SELECT pc.* FROM point_of_interest_classification pc WHERE " +
                SQL_MAX_VERSION_OF_POIC +
                "ORDER BY pc.netex_id, pc.version";
        return Pair.of(sql, new HashMap<String, Object>(0));
    }

    public Optional<String> getClassification(String classificationName, Long parentId){

        String queryStr = "SELECT p.netex_id " +
                "FROM point_of_interest_classification p " +
                " WHERE p.name_value = :className ";


        if (parentId != null){
            queryStr = queryStr + " AND p.parent_id = :parentId";
        }



        Query query = entityManager.createNativeQuery(queryStr);
        query.setParameter("className", classificationName);

        if (parentId != null){
            query.setParameter("parentId", parentId);
        }


        try {
            @SuppressWarnings("unchecked")
            List<String> results = query.getResultList();
            if (results.isEmpty()) {
                return Optional.empty();
            } else {
                return Optional.of(results.get(0));
            }
        } catch (NoResultException noResultException) {
            return Optional.empty();
        }
    }

    @Override
    public String findFirstByKeyValues(String key, Set<String> originalIds) {
        Query query = entityManager.createNativeQuery("SELECT pc.netex_id " +
                "FROM point_of_interest_classification pc " +
                "INNER JOIN point_of_interest_classification_key_values pckv " +
                "ON pckv.point_of_interest_classification_id = pc.id " +
                "INNER JOIN value_items v " +
                "ON pckv.key_values_id = v.value_id " +
                "WHERE pckv.key_values_key = :key " +
                "AND v.items IN ( :values ) " +
                "AND pc.version = (SELECT MAX(pcv.version) FROM point_of_interest pcv WHERE pcv.netex_id = pc.netex_id)");

        query.setParameter("key", key);
        query.setParameter("values", originalIds);

        try {
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
    public int countResult() {
        return countResult(getPointOfInterestClassifications());
    }

    private int countResult(Pair<String, Map<String, Object>> sqlWithParams) {
        Session session = entityManager.unwrap(Session.class);
        NativeQuery query = session.createNativeQuery("SELECT COUNT(*) FROM (" + sqlWithParams.getFirst() + ") AS numberOfPointsOfInterest");
        searchHelper.addParams(query, sqlWithParams.getSecond());
        return ((BigInteger) query.uniqueResult()).intValue();
    }

    @Override
    public Iterator<PointOfInterestClassification> scrollPointOfInterestClassifications() {
        return scrollPointOfInterestClassifications(getPointOfInterestClassifications());
    }

    private Iterator<PointOfInterestClassification> scrollPointOfInterestClassifications(Pair<String, Map<String, Object>> sqlWithParams) {
        final int fetchSize = 800;

        Session session = entityManager.unwrap(Session.class);
        NativeQuery query = session.createNativeQuery(sqlWithParams.getFirst());
        searchHelper.addParams(query, sqlWithParams.getSecond());

        query.addEntity(PointOfInterestClassification.class);
        query.setReadOnly(true);
        query.setFetchSize(fetchSize);
        query.setCacheable(false);
        ScrollableResults results = query.scroll(ScrollMode.FORWARD_ONLY);

        ScrollableResultIterator<PointOfInterestClassification> pointOfInterestClassificationEntityIterator = new ScrollableResultIterator<>(results, fetchSize, session);

        return pointOfInterestClassificationEntityIterator;
    }

    /**
     * Get a batch of object to process
     * @param exportJobId
     * @return
     */
    @org.springframework.transaction.annotation.Transactional(propagation = Propagation.REQUIRES_NEW)
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

    @org.springframework.transaction.annotation.Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteProcessedIds(Long exportJobId, Set<Long> processedPoiClassification) {
        Session session = entityManager.unwrap(Session.class);
        String queryStr = "DELETE FROM export_job_id_list WHERE job_id = :exportJobId AND exported_object_id IN :poiClassificationIdList";
        NativeQuery query = session.createNativeQuery(queryStr);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("exportJobId", exportJobId);
        parameters.put("poiClassificationIdList", processedPoiClassification);
        searchHelper.addParams(query, parameters);
        query.executeUpdate();
    }

    public List<PointOfInterestClassification> getPOIClassificationInitializedForExport(Set<Long> poiIds) {

        Set<String> poiIdStrings = poiIds.stream().map(String::valueOf).collect(Collectors.toSet());

        String joinedPoiIds = String.join(",", poiIdStrings);
        String sql = "SELECT poiclassification FROM PointOfInterestClassification poiclassification WHERE poiclassification.id IN(" + joinedPoiIds + ")";

        TypedQuery<PointOfInterestClassification> pointOfInterestClassificationTypedQuery = entityManager.createQuery(sql, PointOfInterestClassification.class);

        List<PointOfInterestClassification> results = pointOfInterestClassificationTypedQuery.getResultList();

        results.forEach(pointOfInterestClassification -> {
            Hibernate.initialize(pointOfInterestClassification.getParent());
            Hibernate.initialize(pointOfInterestClassification.getKeyValues());
            pointOfInterestClassification.getKeyValues().values().forEach(value -> Hibernate.initialize(value.getItems()));
        });

        return results;
    }

    /**
     * Initialize export job table with points of interest classification ids that must be exported
     *
     * @param exportJobId id of the export job
     */
    @org.springframework.transaction.annotation.Transactional(propagation = Propagation.REQUIRES_NEW)
    public void initExportJobTable(Long exportJobId) {

        Map<String, Object> parameters = new HashMap<>();

        String queryStr = "INSERT INTO export_job_id_list \n" +
                " SELECT :exportJobId, req1.poic_id     \n" +
                " FROM ( \n" +
                " SELECT MAX(poic.id) AS poic_id, MAX(poic.version) AS version FROM point_of_interest_classification poic WHERE (poic.from_date <= :pointInTime OR poic.from_date IS NULL) \n" +
                " AND (poic.to_date >= :pointInTime OR poic.to_date IS NULL) GROUP BY poic.netex_id) req1";


        parameters.put("exportJobId", exportJobId);
        parameters.put("pointInTime", Date.from(Instant.now()));

        Session session = entityManager.unwrap(Session.class);
        NativeQuery query = session.createNativeQuery(queryStr);
        searchHelper.addParams(query, parameters);

        query.executeUpdate();
    }
}
