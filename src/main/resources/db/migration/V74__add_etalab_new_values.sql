ALTER TABLE parking ADD COLUMN IF NOT EXISTS address character varying(255);

ALTER TABLE parking_capacity ADD COLUMN IF NOT EXISTS number_of_bike_spaces integer;
ALTER TABLE parking_capacity ADD COLUMN IF NOT EXISTS number_of_electric_bikes_with_recharge_point integer;
ALTER TABLE parking_capacity ADD COLUMN IF NOT EXISTS number_of_two_wheeled_vehicle integer;
