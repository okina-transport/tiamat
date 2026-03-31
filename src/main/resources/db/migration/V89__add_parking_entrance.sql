CREATE TABLE IF NOT EXISTS parking_entrance (
    id BIGINT PRIMARY KEY,
    netex_id VARCHAR(255) NOT NULL,
    version VARCHAR(255),
    version_comment VARCHAR(255),
    created TIMESTAMP WITHOUT TIME ZONE,
    changed TIMESTAMP WITHOUT TIME ZONE,
    changed_by VARCHAR(255),
    from_date TIMESTAMP WITHOUT TIME ZONE,
    to_date TIMESTAMP WITHOUT TIME ZONE,
    name_value VARCHAR(255),
    name_lang VARCHAR(5),
    description_value VARCHAR(4000),
    description_lang VARCHAR(5),
    private_code_value VARCHAR(255),
    private_code_type VARCHAR(255),
    longitude NUMERIC(10, 7),
    latitude NUMERIC(10, 7),
    centroid GEOMETRY,
    polygon_id BIGINT,
    all_areas_wheelchair_accessible BOOLEAN,
    covered INTEGER,
    public_use character varying(255),
    site_ref VARCHAR(255),
    site_ref_version VARCHAR(255),
    level_ref VARCHAR(255),
    level_ref_version VARCHAR(255),
    parking_id BIGINT,
    place_equipments_id BIGINT,
    accessibility_assessment_id BIGINT,
    CONSTRAINT fk_parking_entrance_parking
    FOREIGN KEY (parking_id)
    REFERENCES parking (id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_parking_entrance_parking_id ON parking_entrance(parking_id);

CREATE SEQUENCE IF NOT EXISTS parking_entrance_seq
    START WITH 1
    INCREMENT BY 10
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE IF NOT EXISTS parking_entrance_alternative_names
(
    parking_entrance_id       bigint not null,
    alternative_names_id bigint not null
);
ALTER TABLE parking_entrance_alternative_names
    ADD CONSTRAINT fk_alternative_names_id FOREIGN KEY (alternative_names_id) REFERENCES alternative_name (id);

ALTER TABLE parking_entrance_alternative_names
    ADD CONSTRAINT fk_parking_entrance_id FOREIGN KEY (parking_entrance_id) REFERENCES parking_entrance (id);

CREATE TABLE IF NOT EXISTS parking_entrance_check_constraints
(
    parking_entrance_id       bigint NOT NULL,
    check_constraints_id bigint NOT NULL
);

ALTER TABLE parking_entrance_check_constraints
    ADD CONSTRAINT fk_parking_entrance_id FOREIGN KEY (parking_entrance_id) REFERENCES parking_entrance (id);

ALTER TABLE parking_entrance_check_constraints
    ADD CONSTRAINT fk_check_constraints_id FOREIGN KEY (check_constraints_id) REFERENCES check_constraint (id);


CREATE TABLE IF NOT EXISTS parking_entrance_equipment_places
(
    parking_entrance_id      bigint NOT NULL,
    equipment_places_id bigint NOT NULL
);

ALTER TABLE parking_entrance_equipment_places
    ADD CONSTRAINT fk_parking_entrance_id FOREIGN KEY (parking_entrance_id) REFERENCES parking_entrance (id);

ALTER TABLE parking_entrance_equipment_places
    ADD CONSTRAINT fk_equipment_places_id FOREIGN KEY (equipment_places_id) REFERENCES equipment_place (id);

CREATE TABLE IF NOT EXISTS parking_entrance_key_values
(
    parking_entrance_id bigint                 NOT NULL,
    key_values_id  bigint                 NOT NULL,
    key_values_key character varying(255) NOT NULL
);
ALTER TABLE parking_entrance_key_values
    ADD CONSTRAINT fk_key_values_id FOREIGN KEY (key_values_id) REFERENCES value (id);

ALTER TABLE parking_entrance_key_values
    ADD CONSTRAINT fk_parking_entrance_id FOREIGN KEY (parking_entrance_id) REFERENCES parking_entrance (id);