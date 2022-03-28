package org.rutebanken.tiamat.repository;


import org.rutebanken.tiamat.model.PointOfInterestFacilitySet;
import org.rutebanken.tiamat.model.TicketingFacilityEnumeration;
import org.rutebanken.tiamat.model.TicketingServiceFacilityEnumeration;
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
public class PointOfInterestRepositoryImpl implements PointOfInterestRepositoryCustom{

    @PersistenceContext
    private EntityManager entityManager;


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

    @Override
    public PointOfInterestFacilitySet getOrCreateFacilitySet(TicketingFacilityEnumeration ticketingFacility, TicketingServiceFacilityEnumeration ticketingServiceFacility) {
        Query query = entityManager.createNativeQuery("SELECT fs.* " +
                "FROM point_of_interest_facility_set fs " +
                "WHERE fs.ticketing_facility = :ticketing_facility " +
                "AND fs.ticketing_service_facility = :ticketing_service_facility " ,PointOfInterestFacilitySet.class );

        query.setParameter("ticketing_facility", ticketingFacility.toString());
        query.setParameter("ticketing_service_facility", ticketingServiceFacility.toString());




        try {
            @SuppressWarnings("unchecked")
            List<PointOfInterestFacilitySet> results = query.getResultList();
            if (results.isEmpty()) {
                return createFacilitySet(ticketingFacility, ticketingServiceFacility);
            } else {
                return results.get(0);
            }
        } catch (NoResultException noResultException) {
            return null;
        }
    }


    private PointOfInterestFacilitySet createFacilitySet(TicketingFacilityEnumeration ticketingFacility, TicketingServiceFacilityEnumeration ticketingServiceFacility){
        PointOfInterestFacilitySet newFacilitySet = new PointOfInterestFacilitySet();
        newFacilitySet.setTicketingFacility(ticketingFacility);
        newFacilitySet.setTicketingServiceFacility(ticketingServiceFacility);
        entityManager.persist(newFacilitySet);
        return newFacilitySet;
    }

    public void clearAllPois(){
        entityManager.createNativeQuery("TRUNCATE TABLE point_of_interest CASCADE").executeUpdate();
    }
}
