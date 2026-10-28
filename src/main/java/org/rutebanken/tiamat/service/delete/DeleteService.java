package org.rutebanken.tiamat.service.delete;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteService {

    private static final Logger logger = LoggerFactory.getLogger(DeleteService.class);

    @PersistenceContext
    private EntityManager entityManager;

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
            logger.info("Point of interests deleted successfully");
        } catch (Exception e) {
            logger.error("Error occurred: ", e);
            throw e;
        }
    }

    private void deleteLink(String base,String link,String on) {
        String tempTableName = "temp_"+link+"_ids";
        entityManager.createNativeQuery(
                "CREATE TEMP TABLE IF NOT EXISTS "+tempTableName+" AS " +
                        "SELECT "+on+" FROM "+link
        ).executeUpdate();
        logger.info("Captured "+link+" IDs before deletion.");

        entityManager.createNativeQuery("DELETE FROM "+link).executeUpdate();
        logger.info("Executed DELETE ON "+link+".");

        entityManager.createNativeQuery(
                "DELETE FROM "+base+" WHERE id IN "+
                        "(SELECT "+on+" FROM "+tempTableName+")"
        ).executeUpdate();
        logger.info("Executed DELETE on "+base+" for "+link+".");

        entityManager.createNativeQuery("DROP TABLE IF EXISTS "+tempTableName).executeUpdate();
        logger.info("Executed DELETE on "+tempTableName+".");
    }

    private void deleteAlternativeNames(String tableName) {
        deleteLink("alternative_name",tableName,"alternative_names_id");
    }

    private void deleteKeyValues(String tableName) {
        entityManager.createNativeQuery(
                "DELETE FROM value_items WHERE value_id IN (SELECT key_values_id FROM "+tableName+")"
        ).executeUpdate();
        logger.info("Executed DELETE on value_items for "+tableName+".");
        deleteLink("value",tableName,"key_values_id");
    }

    private String[] startDeleteAccessibilityAssessments(String tableName) {
        String tempTableName = "temp_"+tableName+"accessibility_assessment_ids";
        String limitationTableName = "temp_"+tableName+"_limitation_ids";

        entityManager.createNativeQuery(
                "CREATE TEMP TABLE IF NOT EXISTS "+tempTableName+" AS " +
                        "SELECT accessibility_assessment_id FROM "+tableName+" WHERE accessibility_assessment_id IS NOT NULL"
        ).executeUpdate();
        logger.info("Captured "+tempTableName+" IDs for before deletion.");

        entityManager.createNativeQuery(
                "CREATE TEMP TABLE IF NOT EXISTS "+limitationTableName+" AS " +
                        "SELECT limitations_id FROM accessibility_assessment_limitations " +
                        "WHERE accessibility_assessment_id IN (SELECT accessibility_assessment_id FROM "+tempTableName+")"
        ).executeUpdate();
        logger.info("Captured "+limitationTableName+" IDs before deletion.");

        entityManager.createNativeQuery(
                "DELETE FROM accessibility_assessment_limitations " +
                        "WHERE accessibility_assessment_id IN (SELECT accessibility_assessment_id FROM "+tempTableName+")"
        ).executeUpdate();
        logger.info("Executed DELETE on accessibility_assessment_limitations.");
        return new String[]{tempTableName, limitationTableName};
    }

    private void finishDeleteAccessibilityAssessments(String[] names) {
        String assessments = names[0];
        String limitations = names[1];

        entityManager.createNativeQuery(
                "DELETE FROM accessibility_assessment " +
                        "WHERE id IN (SELECT accessibility_assessment_id FROM "+assessments+")"
        ).executeUpdate();
        logger.info("Executed DELETE on accessibility_assessment.");

        entityManager.createNativeQuery(
                "DELETE FROM accessibility_limitation " +
                        "WHERE id IN (SELECT limitations_id FROM "+limitations+") " +
                        "AND NOT EXISTS (SELECT 1 FROM accessibility_assessment_limitations " +
                        "WHERE limitations_id = accessibility_limitation.id)"
        ).executeUpdate();
        logger.info("Executed DELETE on accessibility_limitation.");

        entityManager.createNativeQuery("DROP TABLE IF EXISTS "+assessments).executeUpdate();
        entityManager.createNativeQuery("DROP TABLE IF EXISTS "+limitations).executeUpdate();
        logger.info("executed delete on "+assessments+" and "+limitations+".");
    }

    @Transactional
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

        entityManager.createNativeQuery("DELETE FROM parking_area_check_constraints").executeUpdate();
        logger.info("Executed DELETE on parking_area_check_constraints.");

        entityManager.createNativeQuery("DELETE FROM parking_bay_check_constraints").executeUpdate();
        logger.info("Executed DELETE on parking_bay_check_constraints.");

        deleteAlternativeNames("parking_bay_alternative_names");
        deleteKeyValues("parking_bay_key_values");

        entityManager.createNativeQuery("DELETE FROM parking_bay_equipment_places").executeUpdate();
        logger.info("Executed DELETE on parking_bay_equipment_places.");

        entityManager.createNativeQuery(
                "DELETE FROM persistable_polygon " +
                        "WHERE id IN (SELECT polygon_id FROM parking_bay)"
        ).executeUpdate();
        logger.info("Executed DELETE on persistable_polygon.");

        String[] parkingBayAccess = startDeleteAccessibilityAssessments("parking_bay");

        entityManager.createNativeQuery("DELETE FROM parking_bay").executeUpdate();
        logger.info("Executed DELETE on parking_bay.");

        finishDeleteAccessibilityAssessments(parkingBayAccess);

        deleteAlternativeNames("parking_entrance_alternative_names");
        deleteKeyValues("parking_entrance_key_values");

        entityManager.createNativeQuery("DELETE FROM parking_entrance_check_constraints").executeUpdate();
        logger.info("Executed DELETE on parking_entrance_check_constraints.");

        deleteLink("equipment_place","parking_entrance_equipment_places","equipment_places_id");

        entityManager.createNativeQuery(
                "DELETE FROM installed_equipment_version_structure_installed_equipment " +
                        "WHERE place_equipment_id IN (SELECT place_equipments_id FROM temp_place_equipment_ids)"
        ).executeUpdate();
        logger.info("Executed DELETE on installed_equipment_version_structure_installed_equipment.");

        entityManager.createNativeQuery(
                "DELETE FROM equipment_place "+
                        "WHERE id IN (select place_equipments_id FROM temp_place_equipment_ids)"
        ).executeUpdate();
        logger.info("Executed DELETE on place_equipment.");

        entityManager.createNativeQuery(
                "DELETE FROM installed_equipment_version_structure " +
                        "WHERE id IN (SELECT installed_equipment_id FROM temp_installed_equipment_ids)"
        ).executeUpdate();
        logger.info("Executed DELETE on installed_equipment_version_structure.");


        entityManager.createNativeQuery("DELETE FROM parking_equipment_places").executeUpdate();
        logger.info("Executed DELETE on parking_equipment_places.");

        deleteAlternativeNames("parking_area_alternative_names");
        deleteKeyValues("parking_area_key_values");

        deleteLink("equipment_place","parking_area_equipment_places","equipment_places_id");

        entityManager.createNativeQuery("DELETE FROM parking_parking_areas").executeUpdate();
        logger.info("Executed DELETE on parking_parking_areas.");

        entityManager.createNativeQuery(
                "DELETE FROM persistable_polygon " +
                        "WHERE id IN (SELECT polygon_id FROM parking_area)"
        ).executeUpdate();
        logger.info("Executed DELETE on persistable_polygon.");

        entityManager.createNativeQuery(
                "DELETE FROM persistable_polygon " +
                        "WHERE id IN (SELECT polygon_id FROM parking_entrance)"
        ).executeUpdate();
        logger.info("Executed DELETE on persistable_polygon.");

        String[] parkingAreaAccess = startDeleteAccessibilityAssessments("parking_area");

        entityManager.createNativeQuery("DELETE FROM parking_area").executeUpdate();
        logger.info("Executed DELETE on parking_area.");

        finishDeleteAccessibilityAssessments(parkingAreaAccess);

        deleteAlternativeNames("parking_alternative_names");
        deleteKeyValues("parking_key_values");

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

        entityManager.createNativeQuery("DELETE FROM parking_capacity").executeUpdate();
        logger.info("Executed DELETE on parking_capacity.");

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
                "DELETE FROM availability_condition_day_types " +
                        "WHERE availability_condition_id IN (SELECT id " +
                        "FROM availability_condition ac,parking_availability_conditions pac "+
                        "WHERE ac.id = pac.availability_conditions_id)"
        ).executeUpdate();
        logger.info("Executed DELETE on availability_condition_day_types.");
        deleteLink("availability_condition","parking_availability_conditions","availability_conditions_id");


        entityManager.createNativeQuery(
                "DELETE FROM postal_address " +
                        "WHERE id IN (SELECT postal_address_id FROM parking)"
        );

        entityManager.createNativeQuery(
                "DELETE FROM persistable_polygon " +
                        "WHERE id IN (SELECT polygon_id FROM parking_entrance)"
        ).executeUpdate();
        logger.info("Executed DELETE on persistable_polygon.");

        String[] parkingAccess = startDeleteAccessibilityAssessments("parking");
        String[] parkingEntranceAccess = startDeleteAccessibilityAssessments("parking_entrance");

        entityManager.createNativeQuery("DELETE FROM parking").executeUpdate();
        logger.info("Executed DELETE on parking.");

        finishDeleteAccessibilityAssessments(parkingAccess);
        finishDeleteAccessibilityAssessments(parkingEntranceAccess);

        entityManager.createNativeQuery("DROP TABLE IF EXISTS temp_installed_equipment_ids").executeUpdate();
        entityManager.createNativeQuery("DROP TABLE IF EXISTS temp_place_equipment_ids").executeUpdate();
        logger.info("Dropped temporary tables.");

        logger.info("All parking data successfully deleted.");
    }
}
