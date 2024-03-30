package org.rutebanken.tiamat.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;


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

}
