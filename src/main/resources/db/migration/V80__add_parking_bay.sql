CREATE TABLE parking_bay
(
    id                              bigint PRIMARY KEY,
    netex_id                        character varying(255),
    version                         bigint,
    changed                         timestamp without time zone,
    created                         timestamp without time zone,
    from_date                       timestamp without time zone,
    to_date                         timestamp without time zone,
    parking_vehicle_type            character varying(255),
    length                          numeric,
    width                           numeric,
    height                          numeric,
    weight                          numeric,
    maximum_length                  numeric,
    maximum_width                   numeric,
    maximum_height                  numeric,
    maximum_weight                  numeric,
    recharging_available            boolean,
    parking_area_id                 BIGINT,
    changed_by                      character varying(255),
    version_comment                 character varying(255),
    description_lang                character varying(5),
    description_value               character varying(4000),
    name_lang                       character varying(5),
    name_value                      character varying(255),
    private_code_type               character varying(255),
    private_code_value              character varying(255),
    parent_site_ref                 character varying(255),
    parent_site_ref_version         character varying(255),
    level_ref                       character varying(255),
    level_ref_version               character varying(255),
    site_ref                        character varying(255),
    site_ref_version                character varying(255),
    centroid                        geometry,
    all_areas_wheelchair_accessible boolean,
    covered                         integer,
    place_equipments_id             BIGINT,
    accessibility_assessment_id     BIGINT,
    polygon_id                      BIGINT
);

ALTER TABLE parking_bay
    ADD CONSTRAINT fk_parking_area
        FOREIGN KEY (parking_area_id)
            REFERENCES parking_area (id);

CREATE TABLE parking_bay_key_values
(
    parking_bay_id bigint                 NOT NULL,
    key_values_id  bigint                 NOT NULL,
    key_values_key character varying(255) NOT NULL
);
ALTER TABLE parking_bay_key_values
    ADD CONSTRAINT fk_key_values_id FOREIGN KEY (key_values_id) REFERENCES value (id);

ALTER TABLE parking_bay_key_values
    ADD CONSTRAINT fk_parking_bay_id FOREIGN KEY (parking_bay_id) REFERENCES parking_bay (id);

create table parking_bay_alternative_names
(
    parking_bay_id       bigint not null,
    alternative_names_id bigint not null
);
ALTER TABLE parking_bay_alternative_names
    ADD CONSTRAINT fk_alternative_names_id FOREIGN KEY (alternative_names_id) REFERENCES alternative_name (id);

ALTER TABLE parking_bay_alternative_names
    ADD CONSTRAINT fk_parking_bay_id FOREIGN KEY (parking_bay_id) REFERENCES parking_bay (id);

CREATE SEQUENCE parking_bay_seq
    START WITH 1
    INCREMENT BY 10
    NO MINVALUE
    NO MAXVALUE CACHE 1;

CREATE TABLE parking_bay_check_constraints
(
    parking_bay_id       bigint NOT NULL,
    check_constraints_id bigint NOT NULL
);

ALTER TABLE parking_bay_check_constraints
    ADD CONSTRAINT fk_parking_bay_id FOREIGN KEY (parking_bay_id) REFERENCES parking_bay (id);

ALTER TABLE parking_bay_check_constraints
    ADD CONSTRAINT fk_check_constraints_id FOREIGN KEY (check_constraints_id) REFERENCES check_constraint (id);


CREATE TABLE parking_bay_equipment_places
(
    parking_bay_id      bigint NOT NULL,
    equipment_places_id bigint NOT NULL
);

ALTER TABLE parking_bay_equipment_places
    ADD CONSTRAINT fk_parking_bay_id FOREIGN KEY (parking_bay_id) REFERENCES parking_bay (id);

ALTER TABLE parking_bay_equipment_places
    ADD CONSTRAINT fk_equipment_places_id FOREIGN KEY (equipment_places_id) REFERENCES equipment_place (id);
