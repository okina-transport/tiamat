CREATE TABLE IF NOT EXISTS point_of_interest (
                                   id bigint NOT NULL,
                                   netex_id character varying(255),
                                   changed timestamp without time zone,
                                   created timestamp without time zone,
                                   from_date timestamp without time zone,
                                   to_date timestamp without time zone,
                                   version bigint NOT NULL,
                                   version_comment character varying(255),
                                   description_lang character varying(5),
                                   description_value character varying(4000),
                                   name_lang character varying(5),
                                   name_value character varying(255),
                                   short_name_lang character varying(5),
                                   short_name_value character varying(255),
                                   centroid geometry,
                                   zip_code character varying(5),
                                   address character varying(255),
                                   city character varying(255),
                                   postal_code character varying(5),
                                   topographic_place_id bigint,
                                   stop_place_id bigint,
                                   point_of_interest_facility_set_id bigint
);

ALTER TABLE point_of_interest OWNER TO tiamat;

CREATE SEQUENCE IF NOT EXISTS point_of_interest_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER TABLE point_of_interest_seq OWNER TO tiamat;

CREATE TABLE IF NOT EXISTS point_of_interest_key_values (
  point_of_interest_id bigint NOT NULL,
  key_values_id bigint NOT NULL,
  key_values_key character varying(255) NOT NULL
);

ALTER TABLE point_of_interest_key_values OWNER TO tiamat;

CREATE TABLE IF NOT EXISTS point_of_interest_classification (
    id bigint NOT NULL,
    netex_id character varying(255),
    name character varying(255),
    short_name character varying(255),
    osm boolean,
    active boolean,
    parent_id bigint
);

ALTER TABLE point_of_interest_classification OWNER TO tiamat;

CREATE TABLE IF NOT EXISTS point_of_interest_facility_set (
    id bigint NOT NULL,
    netex_id character varying(255),
    ticketing_facility character varying(255),
    ticketing_service_facility character varying(255)
);

ALTER TABLE point_of_interest_facility_set OWNER TO tiamat;

CREATE SEQUENCE IF NOT EXISTS point_of_interest_facility_set_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER TABLE point_of_interest_facility_set_seq OWNER TO tiamat;

ALTER TABLE point_of_interest_facility_set
    ADD CONSTRAINT point_of_interest_facility_set_pkey PRIMARY KEY (id);

ALTER TABLE point_of_interest
    ADD CONSTRAINT point_of_interest_pkey PRIMARY KEY (id),
    ADD CONSTRAINT point_of_interest_topographic_place_fk FOREIGN KEY (topographic_place_id) REFERENCES topographic_place (id),
    ADD CONSTRAINT point_of_interest_stop_place_fk FOREIGN KEY (stop_place_id) REFERENCES stop_place (id),
    ADD CONSTRAINT point_of_interest_facility_set_fk FOREIGN KEY (point_of_interest_facility_set_id) REFERENCES point_of_interest_facility_set (id);

ALTER TABLE point_of_interest_classification
    ADD CONSTRAINT point_of_interest_classification_pkey PRIMARY KEY (id),
    ADD CONSTRAINT parent_id_fk FOREIGN KEY (parent_id) REFERENCES point_of_interest_classification (id);

ALTER TABLE point_of_interest_key_values
    ADD CONSTRAINT point_of_interest_key_values_pkey PRIMARY KEY (point_of_interest_id, key_values_key),
    ADD CONSTRAINT point_of_interest_id_fk FOREIGN KEY (point_of_interest_id) REFERENCES point_of_interest (id),
    ADD CONSTRAINT key_values_id_fk FOREIGN KEY (key_values_id) REFERENCES value (id);