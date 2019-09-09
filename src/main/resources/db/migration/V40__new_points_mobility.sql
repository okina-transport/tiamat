ALTER TABLE parking_capacity ADD COLUMN number_of_carsharing_spaces numeric(19, 2);

ALTER TABLE parking ADD COLUMN number_of_recharging_places numeric(19, 2);
ALTER TABLE parking ADD COLUMN carpooling_available boolean;
ALTER TABLE parking ADD COLUMN carsharing_available boolean;
