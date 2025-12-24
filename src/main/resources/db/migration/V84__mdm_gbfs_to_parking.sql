CREATE TABLE IF NOT EXISTS organisation_key_values (
        organisation_id bigint NOT NULL REFERENCES organisation(id),
        key_values_id bigint NOT NULL REFERENCES value(id),
        key_values_key character varying(255) NOT NULL UNIQUE,
        PRIMARY KEY (organisation_id, key_values_key)
);

ALTER TABLE organisation ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 1;
ALTER TABLE organisation ADD COLUMN IF NOT EXISTS from_date timestamp without time zone;
ALTER TABLE organisation ADD COLUMN IF NOT EXISTS to_date timestamp without time zone;
ALTER TABLE organisation ADD COLUMN IF NOT EXISTS changed_by character varying(255);
ALTER TABLE organisation ADD COLUMN IF NOT EXISTS version_comment character varying(255);
