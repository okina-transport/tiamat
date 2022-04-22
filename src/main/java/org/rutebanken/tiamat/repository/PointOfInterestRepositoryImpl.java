package org.rutebanken.tiamat.repository;

import org.hibernate.Hibernate;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.rutebanken.tiamat.model.PointOfInterest;
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
                "FROM point_of_interest p ", PointOfInterest.class );

        List<PointOfInterest> resultList = query.getResultList();

        for (PointOfInterest poi : resultList) {
            Hibernate.initialize(poi.getClassifications());
        }

        return resultList;
    }
}
