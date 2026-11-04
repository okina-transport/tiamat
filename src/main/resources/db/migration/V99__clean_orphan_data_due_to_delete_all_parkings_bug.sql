DELETE FROM parking_capacity WHERE id NOT IN (
    SELECT spaces_id FROM parking_properties_spaces
);

DROP TABLE IF EXISTS temp_day_type_ids;
CREATE TEMP TABLE IF NOT EXISTS temp_day_type_ids AS
SELECT day_types_id FROM availability_condition_day_types
WHERE availability_condition_id IN (
    SELECT id FROM availability_condition
    WHERE id NOT IN (
        SELECT availability_conditions_id FROM parking_availability_conditions
    )
);

DELETE FROM availability_condition_day_types
WHERE day_types_id IN (SELECT day_types_id FROM temp_day_type_ids);

DELETE FROM day_type
WHERE id IN (SELECT day_types_id FROM temp_day_type_ids);

DELETE FROM availability_condition
WHERE id NOT IN (SELECT availability_conditions_id FROM parking_availability_conditions);


DELETE FROM equipment_place
WHERE
    id NOT IN (
        SELECT equipment_place_id FROM equipment_place_key_values
    )
  AND
    id NOT IN (
        SELECT equipment_places_id FROM access_space_equipment_places
    )
  AND
    id NOT IN (
        SELECT equipment_places_id FROM quay_equipment_places
    )
  AND
    id NOT IN (
        SELECT equipment_places_id FROM parking_equipment_places
    )
  AND
    id NOT IN (
        SELECT equipment_place_id FROM equipment_place_equipment_positions
    )
  AND
    id NOT IN (
        SELECT equipment_places_id FROM boarding_position_equipment_places
    )
  AND
    id NOT IN (
        SELECT equipment_places_id FROM parking_area_equipment_places
    )
  AND
    id NOT IN (
        SELECT equipment_places_id FROM stop_place_equipment_places
    )
  AND
    id NOT IN (
        SELECT equipment_places_id FROM parking_bay_equipment_places
    )
  AND
    id NOT IN (
        SELECT equipment_places_id FROM parking_bay_equipment_places
    );

DROP TABLE IF EXISTS temp_assessment_ids;
CREATE TEMP TABLE IF NOT EXISTS temp_assessment_ids AS
SELECT id FROM accessibility_assessment
WHERE
    id NOT IN (
        SELECT accessibility_assessment_id FROM access_space
    )
  AND
    id NOT IN (
        SELECT accessibility_assessment_id FROM boarding_position
    )
  AND
    id NOT IN (
        SELECT accessibility_assessment_id FROM parking_area
    )
  AND
    id NOT IN (
        SELECT accessibility_assessment_id FROM parking
    )
  AND
    id NOT IN (
        SELECT accessibility_assessment_id FROM quay
    )
  AND
    id NOT IN (
        SELECT accessibility_assessment_id FROM stop_place
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

DELETE FROM alternative_name
WHERE
    id NOT IN (
        SELECT alternative_names_id FROM boarding_position_alternative_names
    )
  AND
    id NOT IN (
        SELECT alternative_names_id FROM stop_place_alternative_names
    )
  AND
    id NOT IN (
        SELECT alternative_names_id FROM quay_alternative_names
    )
  AND
    id NOT IN (
        SELECT alternative_names_id FROM parking_area_alternative_names
    )
  AND
    id NOT IN (
        SELECT alternative_names_id FROM access_space_alternative_names
    )
  AND
    id NOT IN (
        SELECT alternative_names_id FROM parking_alternative_names
    )
  AND
    id NOT IN (
        SELECT alternative_names_id FROM group_of_stop_places_alternative_names
    )
  AND
    id NOT IN (
        SELECT alternative_names_id FROM point_of_interest_alternative_names
    )
  AND
    id NOT IN (
        SELECT alternative_names_id FROM parking_bay_alternative_names
    )
  AND
    id NOT IN (
        SELECT alternative_names_id FROM parking_entrance_alternative_names
    );

DELETE FROM check_constraint
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


DROP TABLE IF EXISTS temp_day_type_ids;
DROP TABLE IF EXISTS temp_assessment_ids;
DROP TABLE IF EXISTS temp_limitation_ids;