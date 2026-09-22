UPDATE parking SET carsharing_available = FALSE WHERE carsharing_available IS NULL;
UPDATE parking SET recharging_available = FALSE WHERE recharging_available IS NULL;
UPDATE parking SET carpooling_available = FALSE WHERE carpooling_available IS NULL;
UPDATE parking SET overnight_parking_permitted = FALSE WHERE overnight_parking_permitted IS NULL;
UPDATE parking SET prohibited_for_hazardous_materials = FALSE WHERE prohibited_for_hazardous_materials IS NULL;
UPDATE parking SET secure = FALSE WHERE secure IS NULL;
UPDATE parking SET real_time_occupancy_available = FALSE WHERE real_time_occupancy_available IS NULL;
UPDATE parking SET free_parking_out_of_hours = FALSE WHERE free_parking_out_of_hours IS NULL;


ALTER TABLE parking
    ALTER COLUMN carsharing_available SET DEFAULT FALSE,
    ALTER COLUMN carsharing_available SET NOT NULL;

ALTER TABLE parking
    ALTER COLUMN recharging_available SET DEFAULT FALSE,
    ALTER COLUMN recharging_available SET NOT NULL;

ALTER TABLE parking
    ALTER COLUMN carpooling_available SET DEFAULT FALSE,
    ALTER COLUMN carpooling_available SET NOT NULL;

ALTER TABLE parking
    ALTER COLUMN overnight_parking_permitted SET DEFAULT FALSE,
    ALTER COLUMN overnight_parking_permitted SET NOT NULL;

ALTER TABLE parking
    ALTER COLUMN prohibited_for_hazardous_materials SET DEFAULT FALSE,
    ALTER COLUMN prohibited_for_hazardous_materials SET NOT NULL;

ALTER TABLE parking
    ALTER COLUMN secure SET DEFAULT FALSE,
    ALTER COLUMN secure SET NOT NULL;

ALTER TABLE parking
    ALTER COLUMN real_time_occupancy_available SET DEFAULT FALSE,
    ALTER COLUMN real_time_occupancy_available SET NOT NULL;

ALTER TABLE parking
    ALTER COLUMN free_parking_out_of_hours SET DEFAULT FALSE,
    ALTER COLUMN free_parking_out_of_hours SET NOT NULL;
