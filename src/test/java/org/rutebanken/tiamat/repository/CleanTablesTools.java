package org.rutebanken.tiamat.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;


@Component
public class CleanTablesTools {

    @Autowired
    protected EntityManagerFactory entityManagerFactory;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cleanInstalledEquipments(){
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();

        Query query = entityManager.createNativeQuery("DELETE FROM installed_equipment_version_structure");

        query.executeUpdate();
        transaction.commit();
        entityManager.close();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cleanStopPlacesAndQuays(){
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();

        Query query = entityManager.createNativeQuery("DELETE FROM stop_place_quays");
        query.executeUpdate();
        Query query2 = entityManager.createNativeQuery("DELETE FROM quay");
        query2.executeUpdate();
        Query query3 = entityManager.createNativeQuery("DELETE FROM stop_place");
        query3.executeUpdate();


        transaction.commit();
        entityManager.close();
    }

}
