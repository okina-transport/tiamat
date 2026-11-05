DELETE FROM point_of_interest_facility_set
WHERE id NOT IN (SELECT point_of_interest_facility_set_id FROM point_of_interest WHERE point_of_interest_facility_set_id IS NOT NULL );

DELETE FROM point_of_interest_key_values
WHERE point_of_interest_id NOT IN (SELECT id FROM point_of_interest );

DELETE FROM point_of_interest_equipment_places
WHERE point_of_interest_id NOT IN (SELECT id FROM point_of_interest );

DROP TABLE IF EXISTS temp_day_type_ids;
CREATE TEMP TABLE IF NOT EXISTS temp_day_type_ids AS
SELECT day_type_id FROM point_of_interest_opening_hours_day_type
WHERE point_of_interest_opening_hours_id IN (
    SELECT id FROM point_of_interest_opening_hours
    WHERE id NOT IN (
        SELECT point_of_interest_opening_hours_id FROM point_of_interest
    )
);

DROP TABLE IF EXISTS temp_time_band_ids;
CREATE TEMP TABLE IF NOT EXISTS temp_time_band_ids AS
SELECT id FROM time_band
WHERE id IN (
    SELECT time_band_id FROM day_type_time_band
    WHERE day_type_id IN (SELECT day_type_id FROM temp_day_type_ids)
);

DELETE FROM day_type_time_band
WHERE day_type_id IN (SELECT day_type_id FROM temp_day_type_ids);

DELETE FROM time_band
WHERE id in (SELECT id FROM temp_time_band_ids);

DELETE FROM day_type
WHERE id IN (SELECT day_type_id FROM temp_day_type_ids);

DELETE FROM point_of_interest_opening_hours_day_type
WHERE point_of_interest_opening_hours_id NOT IN (SELECT point_of_interest_opening_hours_id FROM point_of_interest);

DELETE FROM point_of_interest_opening_hours
WHERE id NOT IN (SELECT point_of_interest_opening_hours_id FROM point_of_interest);

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
        SELECT equipment_places_id FROM parking_entrance_equipment_places
    );

DROP TABLE IF EXISTS temp_assessment_ids;
CREATE TEMP TABLE IF NOT EXISTS temp_assessment_ids AS
SELECT id FROM accessibility_assessment
WHERE
    id NOT IN (
        SELECT accessibility_assessment_id FROM access_space
        WHERE accessibility_assessment_id IS NOT NULL
    )
  AND
    id NOT IN (
        SELECT accessibility_assessment_id FROM boarding_position
        WHERE accessibility_assessment_id IS NOT NULL
    )
  AND
    id NOT IN (
        SELECT accessibility_assessment_id FROM parking_area
        WHERE accessibility_assessment_id IS NOT NULL
    )
  AND
    id NOT IN (
        SELECT accessibility_assessment_id FROM parking
        WHERE accessibility_assessment_id IS NOT NULL
    )
  AND
    id NOT IN (
        SELECT accessibility_assessment_id FROM quay
        WHERE accessibility_assessment_id IS NOT NULL
    )
  AND
    id NOT IN (
        SELECT accessibility_assessment_id FROM stop_place
        WHERE accessibility_assessment_id IS NOT NULL
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

DROP TABLE IF EXISTS temp_day_type_ids;
DROP TABLE IF EXISTS temp_time_band_ids;
DROP TABLE IF EXISTS temp_assessment_ids;
DROP TABLE IF EXISTS temp_limitation_ids;