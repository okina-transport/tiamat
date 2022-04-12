package org.rutebanken.tiamat.repository;


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

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.Transactional;
import java.math.BigInteger;
import java.util.*;


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
}
