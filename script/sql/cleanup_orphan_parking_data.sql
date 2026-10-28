DELETE FROM parking_capacity WHERE id NOT IN (
    SELECT spaces_id FROM parking_properties_spaces
);

DROP TABLE IF EXISTS temp_day_type_ids;
CREATE TEMP TABLE IF NOT EXISTS temp_day_type_ids AS
SELECT day_types_id FROM availability_condition_day_types
WHERE availability_condition_id IN (
    SELECT id FROM availability_condition AS t
    WHERE NOT EXISTS (
        SELECT availability_conditions_id FROM parking_availability_conditions
        WHERE availability_conditions_id = t.id
    )
);

DELETE FROM availability_condition_day_types
WHERE day_types_id IN (SELECT day_types_id FROM temp_day_type_ids);

DROP TABLE IF EXISTS temp_time_band_ids;
CREATE TEMP TABLE IF NOT EXISTS temp_time_band_ids AS
SELECT id FROM time_band
WHERE id IN (
    SELECT time_band_id FROM day_type_time_band
    WHERE day_type_id IN (SELECT day_types_id FROM temp_day_type_ids)
);

DELETE FROM day_type_time_band
WHERE day_type_id IN (SELECT day_types_id FROM temp_day_type_ids);

DELETE FROM time_band
WHERE id in (SELECT id FROM temp_time_band_ids);

DELETE FROM day_type_days_of_week
WHERE day_type_id in (SELECT day_types_id FROM temp_day_type_ids);

DELETE FROM day_type_holiday_types
WHERE day_type_id in (SELECT day_types_id FROM temp_day_type_ids);

DELETE FROM day_type
WHERE id IN (SELECT day_types_id FROM temp_day_type_ids);

DELETE FROM availability_condition
WHERE id NOT IN (SELECT availability_conditions_id FROM parking_availability_conditions);


DELETE FROM equipment_place t
WHERE
    NOT EXISTS (
        SELECT equipment_place_id FROM equipment_place_key_values
        WHERE equipment_place_id = t.id
    )
  AND
    NOT EXISTS(
        SELECT equipment_places_id FROM access_space_equipment_places
        WHERE equipment_places_id = t.id
    )
  AND
    NOT EXISTS (
        SELECT equipment_places_id FROM quay_equipment_places
        WHERE equipment_places_id = t.id
    )
  AND
    NOT EXISTS (
        SELECT equipment_places_id FROM parking_equipment_places
        WHERE equipment_places_id = t.id
    )
  AND
    NOT EXISTS (
        SELECT equipment_place_id FROM equipment_place_equipment_positions
        WHERE equipment_place_id = t.id
    )
  AND
    NOT EXISTS (
        SELECT equipment_places_id FROM boarding_position_equipment_places
        WHERE equipment_places_id = t.id
    )
  AND
    NOT EXISTS (
        SELECT equipment_places_id FROM parking_area_equipment_places
        WHERE equipment_places_id = t.id
    )
  AND
    NOT EXISTS (
        SELECT equipment_places_id FROM stop_place_equipment_places
        WHERE equipment_places_id = t.id
    )
  AND
    NOT EXISTS (
        SELECT equipment_places_id FROM parking_bay_equipment_places
        WHERE equipment_places_id = t.id
    )
  AND
    NOT EXISTS (
        SELECT equipment_places_id FROM parking_entrance_equipment_places
        WHERE equipment_places_id = t.id
    )
  AND
    NOT EXISTS (
        SELECT equipment_places_id FROM point_of_interest_equipment_places
        WHERE equipment_places_id = t.id
    );

DROP TABLE IF EXISTS temp_assessment_ids;
CREATE TEMP TABLE IF NOT EXISTS temp_assessment_ids AS
SELECT id FROM accessibility_assessment AS t
WHERE
    NOT EXISTS (
        SELECT accessibility_assessment_id FROM access_space
        WHERE accessibility_assessment_id = t.id
    )
  AND
    NOT EXISTS (
        SELECT accessibility_assessment_id FROM boarding_position
        WHERE accessibility_assessment_id = t.id
    )
  AND
    NOT EXISTS (
        SELECT accessibility_assessment_id FROM parking_area
        WHERE accessibility_assessment_id = t.id
    )
  AND
    NOT EXISTS (
        SELECT accessibility_assessment_id FROM parking
        WHERE accessibility_assessment_id = t.id
    )
  AND
    NOT EXISTS (
        SELECT accessibility_assessment_id FROM quay
        WHERE accessibility_assessment_id = t.id
    )
  AND
    NOT EXISTS (
        SELECT accessibility_assessment_id FROM stop_place
        WHERE accessibility_assessment_id = t.id
    )
  AND
    NOT EXISTS (
        SELECT accessibility_assessment_id FROM point_of_interest
        WHERE accessibility_assessment_id = t.id
    )
  AND
    NOT EXISTS (
        SELECT accessibility_assessment_id FROM parking_bay
        WHERE accessibility_assessment_id = t.id
    )
  AND
    NOT EXISTS (
        SELECT accessibility_assessment_id FROM parking_entrance
        WHERE accessibility_assessment_id = t.id
    );

DROP TABLE IF EXISTS temp_limitation_ids;
CREATE TEMP TABLE IF NOT EXISTS temp_limitation_ids AS
SELECT limitations_id FROM accessibility_assessment_limitations
WHERE accessibility_assessment_id IN (SELECT id FROM temp_assessment_ids);

DELETE FROM accessibility_assessment_limitations
WHERE accessibility_assessment_id IN (SELECT id FROM temp_assessment_ids);

DELETE FROM accessibility_limitation
WHERE id IN (SELECT limitations_id FROM temp_limitation_ids);

DELETE FROM accessibility_assessment
WHERE id IN (SELECT id FROM temp_assessment_ids);

DELETE FROM alternative_name t
WHERE
    NOT EXISTS (
        SELECT alternative_names_id FROM boarding_position_alternative_names
        WHERE alternative_names_id=t.id
    )
  AND
    NOT EXISTS (
        SELECT alternative_names_id FROM stop_place_alternative_names
        WHERE alternative_names_id=t.id
    )
  AND
    NOT EXISTS (
        SELECT alternative_names_id FROM quay_alternative_names
        WHERE alternative_names_id=t.id
    )
  AND
    NOT EXISTS (
        SELECT alternative_names_id FROM parking_area_alternative_names
        WHERE alternative_names_id=t.id
    )
  AND
    NOT EXISTS (
        SELECT alternative_names_id FROM access_space_alternative_names
        WHERE alternative_names_id=t.id
    )
  AND
    NOT EXISTS (
        SELECT alternative_names_id FROM parking_alternative_names
        WHERE alternative_names_id=t.id
    )
  AND
    NOT EXISTS (
        SELECT alternative_names_id FROM group_of_stop_places_alternative_names
        WHERE alternative_names_id=t.id
    )
  AND
    NOT EXISTS (
        SELECT alternative_names_id FROM point_of_interest_alternative_names
        WHERE alternative_names_id=t.id
    )
  AND
    NOT EXISTS (
        SELECT alternative_names_id FROM parking_bay_alternative_names
        WHERE alternative_names_id=t.id
    )
  AND
    NOT EXISTS (
        SELECT alternative_names_id FROM parking_entrance_alternative_names
        WHERE alternative_names_id=t.id
    );

DROP TABLE IF EXISTS temp_constraint_ids;
CREATE TEMP TABLE IF NOT EXISTS temp_constraint_ids AS
SELECT id FROM check_constraint
WHERE
    id NOT IN (
        SELECT check_constraints_id FROM quay_check_constraints
    )
  AND
    id NOT IN (
        SELECT check_constraints_id FROM parking_area_check_constraints
    )
  AND
    id NOT IN (
        SELECT check_constraints_id FROM access_space_check_constraints
    )
  AND
    id NOT IN (
        SELECT check_constraints_id FROM boarding_position_check_constraints
    )
  AND
    id NOT IN (
        SELECT check_constraints_id FROM parking_bay_check_constraints
    )
  AND
    id NOT IN (
        SELECT check_constraints_id FROM parking_entrance_check_constraints
    );


DROP TABLE IF EXISTS temp_value_ids;
CREATE TEMP TABLE IF NOT EXISTS temp_value_ids AS
SELECT key_values_id FROM check_constraint_key_values
WHERE check_constraint_id IN (SELECT id FROM temp_constraint_ids);

DELETE FROM check_constraint_key_values
WHERE check_constraint_id IN (SELECT id FROM temp_constraint_ids);

DELETE FROM value_items
WHERE value_id IN (SELECT key_values_id FROM temp_value_ids);

DELETE FROM value
WHERE id IN (SELECT key_values_id FROM temp_value_ids);

DELETE FROM check_constraint
WHERE id IN (SELECT id FROM temp_constraint_ids);

DROP TABLE IF EXISTS temp_value_ids;
DROP TABLE IF EXISTS temp_constraint_ids;
DROP TABLE IF EXISTS temp_day_type_ids;
DROP TABLE IF EXISTS temp_time_band_ids;
DROP TABLE IF EXISTS temp_assessment_ids;
DROP TABLE IF EXISTS temp_limitation_ids;