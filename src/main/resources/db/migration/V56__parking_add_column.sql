ALTER TABLE parking ADD COLUMN insee varchar(255);
ALTER TABLE parking ADD COLUMN siret varchar(255);

ALTER TABLE parking_area ADD COLUMN maximum_height numeric;
ALTER TABLE parking_area ADD COLUMN specific_parking_area_usage varchar(255);