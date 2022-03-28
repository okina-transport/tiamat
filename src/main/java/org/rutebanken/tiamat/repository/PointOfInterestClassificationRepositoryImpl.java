package org.rutebanken.tiamat.repository;


import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;


@Repository
@Transactional
public class PointOfInterestClassificationRepositoryImpl implements PointOfInterestClassificationRepositoryCustom{

    @PersistenceContext
    private EntityManager entityManager;



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

}
