package org.rutebanken.tiamat.service.delete;

import org.rutebanken.tiamat.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigInteger;

@Service
public class DeleteService {

    private static final Logger logger = LoggerFactory.getLogger(DeleteService.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    PointOfInterestFacilitySetRepository pointOfInterestFacilitySetRepository;

    @Autowired
    PointOfInterestClassificationRepository pointOfInterestClassificationRepository;

    @Autowired
    ParkingRepository parkingRepository;

    @Transactional
    public void deleteAllPoi() {
        try {
            // Deleting value_items from point_of_interest_key_values if exists...
            BigInteger countKeyValuesPoi = (BigInteger) entityManager.createNativeQuery("SELECT COUNT(*) FROM point_of_interest_key_values")
                    .getSingleResult();

            if (countKeyValuesPoi.compareTo(BigInteger.ZERO) > 0) {
                entityManager.createNativeQuery("DELETE FROM value_items WHERE value_id IN (SELECT key_values_id FROM point_of_interest_key_values)")
                        .executeUpdate();
            }

            // Deleting value_items from point_of_interest_classification_key_values if exists...
            BigInteger countKeyValuesClassification = (BigInteger) entityManager.createNativeQuery("SELECT COUNT(*) FROM point_of_interest_classification_key_values")
                    .getSingleResult();

            if (countKeyValuesClassification.compareTo(BigInteger.ZERO) > 0) {
                entityManager.createNativeQuery("DELETE FROM value_items WHERE value_id IN (SELECT key_values_id FROM point_of_interest_classification_key_values)")
                        .executeUpdate();
            }

            // Truncating point_of_interest and related tables...
            entityManager.createNativeQuery("TRUNCATE TABLE point_of_interest CASCADE").executeUpdate();
            logger.info("Point of interests deleted successfully");
        } catch (Exception e) {
            logger.error("Error occurred: ", e);
            throw e;
        }
    }

    public void deleteAllParkings(){
        try {
            parkingRepository.deleteAll();
            logger.info("Parkings deleted successfully");
        } catch (Exception e) {
            logger.error("Error occurred: ", e);
            throw e;
        }
    }
}
