package org.rutebanken.tiamat.service.delete;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.rutebanken.tiamat.importer.mdm.MdmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteService {

    private static final Logger logger = LoggerFactory.getLogger(DeleteService.class);

    @PersistenceContext
    private EntityManager entityManager;

    private final MdmService mdmService;

    public DeleteService(MdmService mdmService) {
        this.mdmService = mdmService;
    }

    @Transactional
    public void deleteAllPoi() {
        try {
            // Deleting value_items from point_of_interest_key_values if exists...
            Long countKeyValuesPoi = ((Long) entityManager.createNativeQuery("SELECT COUNT(*) FROM point_of_interest_key_values")
                    .getSingleResult());

            if (countKeyValuesPoi > 0) {
                entityManager.createNativeQuery("DELETE FROM value_items WHERE value_id IN (SELECT key_values_id FROM point_of_interest_key_values)")
                        .executeUpdate();
            }

            // Deleting value_items from point_of_interest_classification_key_values if exists...
            Long countKeyValuesClassification = ((Long) entityManager.createNativeQuery("SELECT COUNT(*) FROM point_of_interest_classification_key_values")
                    .getSingleResult());

            if (countKeyValuesClassification > 0) {
                entityManager.createNativeQuery("DELETE FROM value_items WHERE value_id IN (SELECT key_values_id FROM point_of_interest_classification_key_values)")
                        .executeUpdate();
            }

            // Truncating point_of_interest and related tables...
            entityManager.createNativeQuery("TRUNCATE TABLE point_of_interest CASCADE").executeUpdate();
            mdmService.deleteAllPoiIds();
            logger.info("Point of interests deleted successfully");
        } catch (Exception e) {
            logger.error("Error occurred: ", e);
            throw e;
        }
    }

    @Transactional
    @Async
    public void deleteAllParkings() {
        logger.info("All parking data deleting.");

        entityManager.createNativeQuery(
                "CREATE TEMP TABLE IF NOT EXISTS temp_place_equipment_ids AS " +
                        "SELECT place_equipments_id FROM parking WHERE place_equipments_id IS NOT NULL"
        ).executeUpdate();

        entityManager.createNativeQuery(
                "CREATE TEMP TABLE IF NOT EXISTS temp_installed_equipment_ids AS " +
                        "SELECT installed_equipment_id " +
                        "FROM installed_equipment_version_structure_installed_equipment " +
                        "WHERE place_equipment_id IN (SELECT place_equipments_id FROM temp_place_equipment_ids)"
        ).executeUpdate();
        logger.info("Captured equipment IDs before deletion.");

        entityManager.createNativeQuery("DELETE FROM parking_area_check_constraints").executeUpdate();
        logger.info("Executed DELETE on parking_area_check_constraints.");

        entityManager.createNativeQuery("DELETE FROM parking_bay_check_constraints").executeUpdate();
        logger.info("Executed DELETE on parking_bay_check_constraints.");

        entityManager.createNativeQuery(
                "DELETE FROM installed_equipment_version_structure_installed_equipment " +
                        "WHERE place_equipment_id IN (SELECT place_equipments_id FROM temp_place_equipment_ids)"
        ).executeUpdate();
        logger.info("Executed DELETE on installed_equipment_version_structure_installed_equipment.");

        entityManager.createNativeQuery(
                "DELETE FROM installed_equipment_version_structure " +
                        "WHERE id IN (SELECT installed_equipment_id FROM temp_installed_equipment_ids)"
        ).executeUpdate();
        logger.info("Executed DELETE on installed_equipment_version_structure.");

        entityManager.createNativeQuery("DELETE FROM parking_equipment_places").executeUpdate();
        logger.info("Executed DELETE on parking_equipment_places.");

        entityManager.createNativeQuery(
                "DELETE FROM value_items WHERE value_id IN (SELECT key_values_id FROM parking_area_key_values)"
        ).executeUpdate();
        logger.info("Executed DELETE on value_items for parking_area_key_values.");

        entityManager.createNativeQuery("DELETE FROM parking_area_key_values").executeUpdate();
        logger.info("Executed DELETE on parking_area_key_values.");

        entityManager.createNativeQuery("DELETE FROM parking_parking_areas").executeUpdate();
        logger.info("Executed DELETE on parking_parking_areas.");

        entityManager.createNativeQuery("DELETE FROM parking_area").executeUpdate();
        logger.info("Executed DELETE on parking_area.");

        entityManager.createNativeQuery(
                "DELETE FROM value_items WHERE value_id IN (SELECT key_values_id FROM parking_key_values)"
        ).executeUpdate();
        logger.info("Executed DELETE on value_items for parking_key_values.");

        entityManager.createNativeQuery("DELETE FROM parking_key_values").executeUpdate();
        logger.info("Executed DELETE on parking_key_values.");

        entityManager.createNativeQuery(
                "DELETE FROM alternative_name WHERE id IN (SELECT alternative_names_id FROM parking_alternative_names)"
        ).executeUpdate();
        logger.info("Executed DELETE on alternative_name.");

        entityManager.createNativeQuery("DELETE FROM parking_alternative_names").executeUpdate();
        logger.info("Executed DELETE on parking_alternative_names.");

        entityManager.createNativeQuery("DELETE FROM parking_adjacent_sites").executeUpdate();
        logger.info("Executed DELETE on parking_adjacent_sites.");

        entityManager.createNativeQuery("DELETE FROM parking_parking_payment_methods").executeUpdate();
        logger.info("Executed DELETE on parking_parking_payment_methods.");

        entityManager.createNativeQuery("DELETE FROM parking_parking_payment_process").executeUpdate();
        logger.info("Executed DELETE on parking_parking_payment_process.");

        entityManager.createNativeQuery("DELETE FROM parking_type_of_payment_methods").executeUpdate();
        logger.info("Executed DELETE on parking_type_of_payment_methods.");

        entityManager.createNativeQuery("DELETE FROM parking_parking_properties").executeUpdate();
        logger.info("Executed DELETE on parking_parking_properties.");

        entityManager.createNativeQuery("DELETE FROM parking_properties_spaces").executeUpdate();
        logger.info("Executed DELETE on parking_properties_spaces.");

        entityManager.createNativeQuery("DELETE FROM parking_properties_parking_vehicle_types").executeUpdate();
        logger.info("Executed DELETE on parking_properties_parking_vehicle_types.");

        entityManager.createNativeQuery("DELETE FROM parking_properties_parking_user_types").executeUpdate();
        logger.info("Executed DELETE on parking_properties_parking_user_types.");

        entityManager.createNativeQuery("DELETE FROM parking_properties").executeUpdate();
        logger.info("Executed DELETE on parking_properties.");

        entityManager.createNativeQuery("DELETE FROM parking_parking_vehicle_types").executeUpdate();
        logger.info("Executed DELETE on parking_parking_vehicle_types.");

        entityManager.createNativeQuery("DELETE FROM parking_transport_types").executeUpdate();
        logger.info("Executed DELETE on parking_transport_types.");

        entityManager.createNativeQuery(
                "DELETE FROM accessibility_limitation " +
                        "WHERE id IN (SELECT limitations_id FROM accessibility_assessment_limitations " +
                        "WHERE accessibility_assessment_id IN (SELECT accessibility_assessment_id FROM parking))"
        ).executeUpdate();
        logger.info("Executed DELETE on accessibility_limitation.");

        entityManager.createNativeQuery(
                "DELETE FROM accessibility_assessment_limitations " +
                        "WHERE accessibility_assessment_id IN (SELECT accessibility_assessment_id FROM parking)"
        ).executeUpdate();
        logger.info("Executed DELETE on accessibility_assessment_limitations.");

        entityManager.createNativeQuery(
                "DELETE FROM accessibility_assessment WHERE id IN (SELECT accessibility_assessment_id FROM parking)"
        ).executeUpdate();
        logger.info("Executed DELETE on accessibility_assessment.");

        entityManager.createNativeQuery("DELETE FROM parking").executeUpdate();
        logger.info("Executed DELETE on parking.");

        entityManager.createNativeQuery("DROP TABLE IF EXISTS temp_installed_equipment_ids").executeUpdate();
        entityManager.createNativeQuery("DROP TABLE IF EXISTS temp_place_equipment_ids").executeUpdate();
        logger.info("Dropped temporary tables.");

        mdmService.deleteAllParkingIds();
        logger.info("All parking data successfully deleted.");
    }
}
