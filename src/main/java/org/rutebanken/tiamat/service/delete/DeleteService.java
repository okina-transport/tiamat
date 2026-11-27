package org.rutebanken.tiamat.service.delete;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

            entityManager.createNativeQuery(
                    "CREATE TEMP TABLE IF NOT EXISTS temp_poi_accessibility_assessment_ids AS " +
                            "SELECT accessibility_assessment_id FROM point_of_interest WHERE accessibility_assessment_id IS NOT NULL"
            ).executeUpdate();
            entityManager.createNativeQuery(
                    "CREATE TEMP TABLE IF NOT EXISTS temp_poi_limitation_ids AS " +
                            "SELECT limitations_id FROM accessibility_assessment_limitations " +
                            "WHERE accessibility_assessment_id IN (SELECT accessibility_assessment_id FROM temp_poi_accessibility_assessment_ids)"
            ).executeUpdate();

            entityManager.createNativeQuery(
                    "CREATE TEMP TABLE IF NOT EXISTS temp_poi_place_equipment_ids AS " +
                            "SELECT place_equipments_id FROM point_of_interest WHERE place_equipments_id IS NOT NULL"
            ).executeUpdate();
            entityManager.createNativeQuery(
                    "CREATE TEMP TABLE IF NOT EXISTS temp_poi_installed_equipment_ids AS " +
                            "SELECT installed_equipment_id FROM installed_equipment_version_structure_installed_equipment " +
                            "WHERE place_equipment_id IN (SELECT place_equipments_id FROM temp_poi_place_equipment_ids)"
            ).executeUpdate();

            entityManager.createNativeQuery(
                    "CREATE TEMP TABLE IF NOT EXISTS temp_poi_value_ids AS " +
                            "SELECT key_values_id FROM point_of_interest_key_values"
            ).executeUpdate();
            entityManager.createNativeQuery(
                    "CREATE TEMP TABLE IF NOT EXISTS temp_poi_alternative_name_ids AS " +
                            "SELECT alternative_names_id FROM point_of_interest_alternative_names"
            ).executeUpdate();
            entityManager.createNativeQuery(
                    "CREATE TEMP TABLE IF NOT EXISTS temp_poi_polygon_ids AS " +
                            "SELECT polygon_id FROM point_of_interest WHERE polygon_id IS NOT NULL"
            ).executeUpdate();
            entityManager.createNativeQuery(
                    "CREATE TEMP TABLE IF NOT EXISTS temp_poi_opening_hours_ids AS " +
                            "SELECT point_of_interest_opening_hours_id FROM point_of_interest WHERE point_of_interest_opening_hours_id IS NOT NULL"
            ).executeUpdate();
            entityManager.createNativeQuery(
                    "CREATE TEMP TABLE IF NOT EXISTS temp_poi_day_type_ids AS " +
                            "SELECT day_type_id FROM point_of_interest_opening_hours_day_type " +
                            "WHERE point_of_interest_opening_hours_id IN (SELECT point_of_interest_opening_hours_id FROM temp_poi_opening_hours_ids)"
            ).executeUpdate();
            entityManager.createNativeQuery(
                    "CREATE TEMP TABLE IF NOT EXISTS temp_poi_time_band_ids AS " +
                            "SELECT time_band_id FROM day_type_time_band WHERE day_type_id IN (SELECT day_type_id FROM temp_poi_day_type_ids)"
            ).executeUpdate();
            entityManager.createNativeQuery(
                    "CREATE TEMP TABLE IF NOT EXISTS temp_poi_equipment_place_ids AS " +
                            "SELECT equipment_places_id FROM point_of_interest_equipment_places"
            ).executeUpdate();

            // Truncating point_of_interest and related tables...
            entityManager.createNativeQuery("TRUNCATE TABLE point_of_interest CASCADE").executeUpdate();
            logger.info("Point of interests deleted successfully");

            entityManager.createNativeQuery(
                    "DELETE FROM accessibility_assessment_limitations " +
                            "WHERE accessibility_assessment_id IN (SELECT accessibility_assessment_id FROM temp_poi_accessibility_assessment_ids)"
            ).executeUpdate();
            entityManager.createNativeQuery(
                    "DELETE FROM accessibility_assessment " +
                            "WHERE id IN (SELECT accessibility_assessment_id FROM temp_poi_accessibility_assessment_ids)"
            ).executeUpdate();
            entityManager.createNativeQuery(
                    "DELETE FROM accessibility_limitation " +
                            "WHERE id IN (SELECT limitations_id FROM temp_poi_limitation_ids) " +
                            "AND NOT EXISTS (SELECT 1 FROM accessibility_assessment_limitations " +
                            "WHERE limitations_id = accessibility_limitation.id)"
            ).executeUpdate();
            entityManager.createNativeQuery(
                    "DELETE FROM installed_equipment_version_structure_installed_equipment " +
                            "WHERE place_equipment_id IN (SELECT place_equipments_id FROM temp_poi_place_equipment_ids)"
            ).executeUpdate();
            entityManager.createNativeQuery(
                    "DELETE FROM installed_equipment_version_structure ievs " +
                            "WHERE ievs.id IN (SELECT installed_equipment_id FROM temp_poi_installed_equipment_ids) " +
                            "AND NOT EXISTS (SELECT 1 FROM installed_equipment_version_structure_installed_equipment ie WHERE ie.installed_equipment_id = ievs.id)"
            ).executeUpdate();
            entityManager.createNativeQuery(
                    "DELETE FROM installed_equipment_version_structure ievs " +
                            "WHERE ievs.id IN (SELECT place_equipments_id FROM temp_poi_place_equipment_ids) " +
                            "AND NOT EXISTS (SELECT 1 FROM installed_equipment_version_structure_installed_equipment ie WHERE ie.place_equipment_id = ievs.id) " +
                            "AND NOT EXISTS (SELECT 1 FROM stop_place sp WHERE sp.place_equipments_id = ievs.id) " +
                            "AND NOT EXISTS (SELECT 1 FROM quay q WHERE q.place_equipments_id = ievs.id) " +
                            "AND NOT EXISTS (SELECT 1 FROM access_space a WHERE a.place_equipments_id = ievs.id) " +
                            "AND NOT EXISTS (SELECT 1 FROM boarding_position b WHERE b.place_equipments_id = ievs.id) " +
                            "AND NOT EXISTS (SELECT 1 FROM parking p WHERE p.place_equipments_id = ievs.id) " +
                            "AND NOT EXISTS (SELECT 1 FROM parking_area pa WHERE pa.place_equipments_id = ievs.id) " +
                            "AND NOT EXISTS (SELECT 1 FROM parking_bay pb WHERE pb.place_equipments_id = ievs.id) " +
                            "AND NOT EXISTS (SELECT 1 FROM parking_entrance pe WHERE pe.place_equipments_id = ievs.id)"
            ).executeUpdate();

            entityManager.createNativeQuery("DELETE FROM point_of_interest_adjacent_sites").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM point_of_interest_equipment_places").executeUpdate();
            entityManager.createNativeQuery(
                    "DELETE FROM equipment_place WHERE id IN (SELECT equipment_places_id FROM temp_poi_equipment_place_ids)"
            ).executeUpdate();

            entityManager.createNativeQuery(
                    "DELETE FROM value WHERE id IN (SELECT key_values_id FROM temp_poi_value_ids)"
            ).executeUpdate();
            entityManager.createNativeQuery(
                    "DELETE FROM alternative_name WHERE id IN (SELECT alternative_names_id FROM temp_poi_alternative_name_ids)"
            ).executeUpdate();
            entityManager.createNativeQuery(
                    "DELETE FROM persistable_polygon WHERE id IN (SELECT polygon_id FROM temp_poi_polygon_ids)"
            ).executeUpdate();

            entityManager.createNativeQuery(
                    "DELETE FROM point_of_interest_opening_hours_day_type " +
                            "WHERE point_of_interest_opening_hours_id IN (SELECT point_of_interest_opening_hours_id FROM temp_poi_opening_hours_ids)"
            ).executeUpdate();
            entityManager.createNativeQuery(
                    "DELETE FROM point_of_interest_opening_hours " +
                            "WHERE id IN (SELECT point_of_interest_opening_hours_id FROM temp_poi_opening_hours_ids)"
            ).executeUpdate();
            entityManager.createNativeQuery(
                    "CREATE TEMP TABLE IF NOT EXISTS temp_poi_unused_day_type_ids AS " +
                            "SELECT day_type_id FROM temp_poi_day_type_ids dt " +
                            "WHERE NOT EXISTS (SELECT 1 FROM point_of_interest_opening_hours_day_type o WHERE o.day_type_id = dt.day_type_id) " +
                            "AND NOT EXISTS (SELECT 1 FROM availability_condition_day_types a WHERE a.day_types_id = dt.day_type_id)"
            ).executeUpdate();
            entityManager.createNativeQuery(
                    "DELETE FROM day_type_time_band WHERE day_type_id IN (SELECT day_type_id FROM temp_poi_unused_day_type_ids)"
            ).executeUpdate();
            entityManager.createNativeQuery(
                    "DELETE FROM day_type_days_of_week WHERE day_type_id IN (SELECT day_type_id FROM temp_poi_unused_day_type_ids)"
            ).executeUpdate();
            entityManager.createNativeQuery(
                    "DELETE FROM day_type_holiday_types WHERE day_type_id IN (SELECT day_type_id FROM temp_poi_unused_day_type_ids)"
            ).executeUpdate();
            entityManager.createNativeQuery(
                    "DELETE FROM day_type WHERE id IN (SELECT day_type_id FROM temp_poi_unused_day_type_ids)"
            ).executeUpdate();
            entityManager.createNativeQuery(
                    "DELETE FROM time_band tb WHERE tb.id IN (SELECT time_band_id FROM temp_poi_time_band_ids) " +
                            "AND NOT EXISTS (SELECT 1 FROM day_type_time_band dttb WHERE dttb.time_band_id = tb.id)"
            ).executeUpdate();
            logger.info("Point of interests opening hours, alternative names, polygons and key values deleted successfully");

            for (String tempTable : List.of("temp_poi_value_ids", "temp_poi_alternative_name_ids", "temp_poi_polygon_ids",
                    "temp_poi_opening_hours_ids", "temp_poi_day_type_ids", "temp_poi_unused_day_type_ids",
                    "temp_poi_time_band_ids", "temp_poi_equipment_place_ids")) {
                entityManager.createNativeQuery("DROP TABLE IF EXISTS " + tempTable).executeUpdate();
            }
            entityManager.createNativeQuery("DROP TABLE IF EXISTS temp_poi_installed_equipment_ids").executeUpdate();
            entityManager.createNativeQuery("DROP TABLE IF EXISTS temp_poi_place_equipment_ids").executeUpdate();
            entityManager.createNativeQuery("DROP TABLE IF EXISTS temp_poi_limitation_ids").executeUpdate();
            entityManager.createNativeQuery("DROP TABLE IF EXISTS temp_poi_accessibility_assessment_ids").executeUpdate();
            logger.info("Point of interests accessibility assessments deleted successfully");
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
                        "SELECT place_equipments_id FROM parking WHERE place_equipments_id IS NOT NULL " +
                        "UNION SELECT place_equipments_id FROM parking_area WHERE place_equipments_id IS NOT NULL " +
                        "UNION SELECT place_equipments_id FROM parking_bay WHERE place_equipments_id IS NOT NULL " +
                        "UNION SELECT place_equipments_id FROM parking_entrance WHERE place_equipments_id IS NOT NULL"
        ).executeUpdate();

        entityManager.createNativeQuery(
                "CREATE TEMP TABLE IF NOT EXISTS temp_installed_equipment_ids AS " +
                        "SELECT installed_equipment_id " +
                        "FROM installed_equipment_version_structure_installed_equipment " +
                        "WHERE place_equipment_id IN (SELECT place_equipments_id FROM temp_place_equipment_ids)"
        ).executeUpdate();

        entityManager.createNativeQuery(
                "CREATE TEMP TABLE IF NOT EXISTS temp_accessibility_assessment_ids AS " +
                        "SELECT accessibility_assessment_id FROM parking WHERE accessibility_assessment_id IS NOT NULL " +
                        "UNION SELECT accessibility_assessment_id FROM parking_area WHERE accessibility_assessment_id IS NOT NULL " +
                        "UNION SELECT accessibility_assessment_id FROM parking_bay WHERE accessibility_assessment_id IS NOT NULL " +
                        "UNION SELECT accessibility_assessment_id FROM parking_entrance WHERE accessibility_assessment_id IS NOT NULL"
        ).executeUpdate();
        entityManager.createNativeQuery(
                "CREATE TEMP TABLE IF NOT EXISTS temp_availability_condition_ids AS " +
                        "SELECT availability_conditions_id FROM parking_availability_conditions"
        ).executeUpdate();

        entityManager.createNativeQuery(
                "CREATE TEMP TABLE IF NOT EXISTS temp_postal_address_ids AS " +
                        "SELECT postal_address_id FROM parking WHERE postal_address_id IS NOT NULL"
        ).executeUpdate();
        logger.info("Captured equipment, accessibility assessment, availability condition and postal address IDs before deletion.");

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

        entityManager.createNativeQuery(
                "DELETE FROM value_items WHERE value_id IN (SELECT key_values_id FROM parking_bay_key_values)"
        ).executeUpdate();
        logger.info("Executed DELETE on value_items for parking_bay_key_values.");

        entityManager.createNativeQuery("DELETE FROM parking_bay_key_values").executeUpdate();
        logger.info("Executed DELETE on parking_bay_key_values.");

        entityManager.createNativeQuery(
                "CREATE TEMP TABLE IF NOT EXISTS temp_alternative_name_ids AS " +
                        "SELECT alternative_names_id FROM parking_bay_alternative_names " +
                        "UNION SELECT alternative_names_id FROM parking_area_alternative_names " +
                        "UNION SELECT alternative_names_id FROM parking_entrance_alternative_names"
        ).executeUpdate();

        entityManager.createNativeQuery("DELETE FROM parking_bay_alternative_names").executeUpdate();
        logger.info("Executed DELETE on parking_bay_alternative_names.");

        entityManager.createNativeQuery("DELETE FROM parking_bay_equipment_places").executeUpdate();
        logger.info("Executed DELETE on parking_bay_equipment_places.");

        entityManager.createNativeQuery("DELETE FROM parking_bay").executeUpdate();
        logger.info("Executed DELETE on parking_bay.");

        entityManager.createNativeQuery("DELETE FROM parking_area_alternative_names").executeUpdate();
        logger.info("Executed DELETE on parking_area_alternative_names.");

        entityManager.createNativeQuery("DELETE FROM parking_area_equipment_places").executeUpdate();
        logger.info("Executed DELETE on parking_area_equipment_places.");

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

        entityManager.createNativeQuery("DELETE FROM parking_cards_accepted").executeUpdate();
        logger.info("Executed DELETE on parking_cards_accepted.");

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
                "CREATE TEMP TABLE IF NOT EXISTS temp_limitation_ids AS " +
                        "SELECT limitations_id FROM accessibility_assessment_limitations " +
                        "WHERE accessibility_assessment_id IN (SELECT accessibility_assessment_id FROM temp_accessibility_assessment_ids)"
        ).executeUpdate();
        logger.info("Captured limitation IDs before deletion.");

        entityManager.createNativeQuery(
                "DELETE FROM accessibility_assessment_limitations " +
                        "WHERE accessibility_assessment_id IN (SELECT accessibility_assessment_id FROM temp_accessibility_assessment_ids)"
        ).executeUpdate();
        logger.info("Executed DELETE on accessibility_assessment_limitations.");

        entityManager.createNativeQuery(
                "DELETE FROM value_items WHERE value_id IN (SELECT key_values_id FROM parking_entrance_key_values)"
        ).executeUpdate();
        logger.info("Executed DELETE on value_items for parking_entrance_key_values.");

        entityManager.createNativeQuery("DELETE FROM parking_entrance_key_values").executeUpdate();
        logger.info("Executed DELETE on parking_entrance_key_values.");

        entityManager.createNativeQuery("DELETE FROM parking_entrance_alternative_names").executeUpdate();
        logger.info("Executed DELETE on parking_entrance_alternative_names.");

        entityManager.createNativeQuery("DELETE FROM parking_entrance_check_constraints").executeUpdate();
        logger.info("Executed DELETE on parking_entrance_check_constraints.");

        entityManager.createNativeQuery("DELETE FROM parking_entrance_equipment_places").executeUpdate();
        logger.info("Executed DELETE on parking_entrance_equipment_places.");

        entityManager.createNativeQuery("DELETE FROM parking_entrance").executeUpdate();
        logger.info("Executed DELETE on parking_entrance.");

        entityManager.createNativeQuery(
                "DELETE FROM alternative_name WHERE id IN (SELECT alternative_names_id FROM temp_alternative_name_ids)"
        ).executeUpdate();
        logger.info("Executed DELETE on alternative_name for parking areas, bays and entrances.");

        entityManager.createNativeQuery("DELETE FROM parking_availability_conditions").executeUpdate();
        logger.info("Executed DELETE on parking_availability_conditions.");

        entityManager.createNativeQuery(
                "DELETE FROM availability_condition_day_types " +
                        "WHERE availability_condition_id IN (SELECT availability_conditions_id FROM temp_availability_condition_ids)"
        ).executeUpdate();
        logger.info("Executed DELETE on availability_condition_day_types.");

        entityManager.createNativeQuery(
                "DELETE FROM availability_condition " +
                        "WHERE id IN (SELECT availability_conditions_id FROM temp_availability_condition_ids)"
        ).executeUpdate();
        logger.info("Executed DELETE on availability_condition.");

        entityManager.createNativeQuery("DELETE FROM parking").executeUpdate();
        logger.info("Executed DELETE on parking.");

        entityManager.createNativeQuery(
                "DELETE FROM postal_address WHERE id IN (SELECT postal_address_id FROM temp_postal_address_ids)"
        ).executeUpdate();
        logger.info("Executed DELETE on postal_address.");

        entityManager.createNativeQuery(
                "DELETE FROM installed_equipment_version_structure ievs " +
                        "WHERE ievs.id IN (SELECT place_equipments_id FROM temp_place_equipment_ids) " +
                        "AND NOT EXISTS (SELECT 1 FROM installed_equipment_version_structure_installed_equipment ie WHERE ie.place_equipment_id = ievs.id) " +
                        "AND NOT EXISTS (SELECT 1 FROM stop_place sp WHERE sp.place_equipments_id = ievs.id) " +
                        "AND NOT EXISTS (SELECT 1 FROM quay q WHERE q.place_equipments_id = ievs.id) " +
                        "AND NOT EXISTS (SELECT 1 FROM access_space a WHERE a.place_equipments_id = ievs.id) " +
                        "AND NOT EXISTS (SELECT 1 FROM boarding_position b WHERE b.place_equipments_id = ievs.id) " +
                        "AND NOT EXISTS (SELECT 1 FROM point_of_interest poi WHERE poi.place_equipments_id = ievs.id)"
        ).executeUpdate();
        logger.info("Executed DELETE on installed_equipment_version_structure for place equipments.");

        entityManager.createNativeQuery(
                "DELETE FROM accessibility_assessment " +
                        "WHERE id IN (SELECT accessibility_assessment_id FROM temp_accessibility_assessment_ids)"
        ).executeUpdate();
        logger.info("Executed DELETE on accessibility_assessment.");

        entityManager.createNativeQuery(
                "DELETE FROM accessibility_limitation " +
                        "WHERE id IN (SELECT limitations_id FROM temp_limitation_ids) " +
                        "AND NOT EXISTS (SELECT 1 FROM accessibility_assessment_limitations " +
                        "WHERE limitations_id = accessibility_limitation.id)"
        ).executeUpdate();
        logger.info("Executed DELETE on accessibility_limitation.");

        entityManager.createNativeQuery("DROP TABLE IF EXISTS temp_installed_equipment_ids").executeUpdate();
        entityManager.createNativeQuery("DROP TABLE IF EXISTS temp_place_equipment_ids").executeUpdate();
        entityManager.createNativeQuery("DROP TABLE IF EXISTS temp_limitation_ids").executeUpdate();
        entityManager.createNativeQuery("DROP TABLE IF EXISTS temp_accessibility_assessment_ids").executeUpdate();
        entityManager.createNativeQuery("DROP TABLE IF EXISTS temp_availability_condition_ids").executeUpdate();
        entityManager.createNativeQuery("DROP TABLE IF EXISTS temp_postal_address_ids").executeUpdate();
        entityManager.createNativeQuery("DROP TABLE IF EXISTS temp_alternative_name_ids").executeUpdate();
        logger.info("Dropped temporary tables.");

        logger.info("All parking data successfully deleted.");
    }
}
