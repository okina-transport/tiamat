package org.rutebanken.tiamat.repository;


import org.rutebanken.tiamat.model.MultilingualStringEntity;
import org.rutebanken.tiamat.model.PointOfInterestClassification;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Set;

@Repository
@Transactional
public class PointOfInterestClassificationRepositoryImpl implements PointOfInterestClassificationRepositoryCustom{

    @PersistenceContext
    private EntityManager entityManager;

    public PointOfInterestClassification getOrCreateClassification(String classificationName){
        Query query = entityManager.createNativeQuery("SELECT p " +
                "FROM point_of_interest_classification p " +
                " WHERE p.name = :className ");

        query.setParameter("className", classificationName);

        try {
            @SuppressWarnings("unchecked")
            List<PointOfInterestClassification> results = query.getResultList();
            if (results.isEmpty()) {
                return createNewClassification(classificationName, null);
            } else {
                return results.get(0);
            }
        } catch (NoResultException noResultException) {
            return null;
        }
    }

    private PointOfInterestClassification createNewClassification(String classificationName, Long parentId){
        PointOfInterestClassification newClass = new PointOfInterestClassification();
        newClass.setName(new MultilingualStringEntity(classificationName));

        if (parentId != null){
            newClass.setParentId(parentId);
        }

        entityManager.persist(newClass);
        return newClass;
    }

    public PointOfInterestClassification getOrCreateClassification(String classificationName, String parentClassificationName){

        PointOfInterestClassification parentClassification = getOrCreateClassification(parentClassificationName);
        Long parentId = parentClassification.getId();

        Query query = entityManager.createNativeQuery("SELECT p " +
                "FROM point_of_interest_classification p " +
                " WHERE p.name = :className AND p.parent_id = :parentId");

        query.setParameter("className", classificationName);
        query.setParameter("parentId", parentId);

        try {
            @SuppressWarnings("unchecked")
            List<PointOfInterestClassification> results = query.getResultList();
            if (results.isEmpty()) {
                return createNewClassification(classificationName, parentId);
            } else {
                return results.get(0);
            }
        } catch (NoResultException noResultException) {
            return null;
        }
        
    }



}
