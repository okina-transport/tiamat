ALTER TABLE stop_place DROP CONSTRAINT fk_provider_id;
ALTER TABLE stop_place DROP COLUMN provider_id;

ALTER TABLE stop_place ADD COLUMN provider CHARACTER VARYING(255);