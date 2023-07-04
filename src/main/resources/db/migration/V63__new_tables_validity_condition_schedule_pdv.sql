ALTER TABLE point_of_interest
    ADD COLUMN IF NOT EXISTS point_of_interest_opening_hours_id bigint;

CREATE TABLE IF NOT EXISTS point_of_interest_opening_hours (
    id bigint NOT NULL,
    netex_id character varying(255),
    version bigint NOT NULL,
    from_date timestamp without time zone,
    to_date timestamp without time zone,
    changed timestamp without time zone,
    created timestamp without time zone
);

ALTER TABLE point_of_interest_opening_hours OWNER TO tiamat;

CREATE TABLE IF NOT EXISTS day_type (
    id bigint NOT NULL,
    netex_id character varying(255),
    day_of_week character varying(255),
    version bigint NOT NULL,
    from_date timestamp without time zone,
    to_date timestamp without time zone,
    changed timestamp without time zone,
    created timestamp without time zone
    );

ALTER TABLE day_type OWNER TO tiamat;

CREATE TABLE IF NOT EXISTS time_band (
    id bigint NOT NULL,
    netex_id character varying(255),
    start_time timestamp without time zone,
    end_time timestamp without time zone,
    version bigint NOT NULL,
    from_date timestamp without time zone,
    to_date timestamp without time zone,
    changed timestamp without time zone,
    created timestamp without time zone
    );

ALTER TABLE time_band OWNER TO tiamat;

CREATE TABLE IF NOT EXISTS point_of_interest_opening_hours_day_type (
     point_of_interest_opening_hours_id bigint NOT NULL,
     day_type_id bigint NOT NULL
    );

ALTER TABLE point_of_interest_opening_hours_day_type OWNER TO tiamat;

CREATE TABLE IF NOT EXISTS day_type_time_band (
     day_type_id bigint NOT NULL,
     time_band_id bigint NOT NULL
);
ALTER TABLE day_type_time_band OWNER TO tiamat;

ALTER TABLE point_of_interest_opening_hours
    ADD CONSTRAINT point_of_interest_opening_hours_pkey PRIMARY KEY (id);

ALTER TABLE day_type
    ADD CONSTRAINT day_type_pkey PRIMARY KEY (id);

ALTER TABLE time_band
    ADD CONSTRAINT time_band_pkey PRIMARY KEY (id);

ALTER TABLE point_of_interest_opening_hours_day_type
    ADD CONSTRAINT point_of_interest_opening_hours_day_type_pkey  PRIMARY KEY (point_of_interest_opening_hours_id, day_type_id);

ALTER TABLE point_of_interest_opening_hours_day_type
    ADD CONSTRAINT point_of_interest_opening_hours_fk FOREIGN KEY (point_of_interest_opening_hours_id) REFERENCES point_of_interest_opening_hours (id);

ALTER TABLE point_of_interest_opening_hours_day_type
    ADD CONSTRAINT day_type_fk FOREIGN KEY (day_type_id) REFERENCES day_type (id);

ALTER TABLE day_type_time_band
    ADD CONSTRAINT day_type_time_band_pkey  PRIMARY KEY (day_type_id, time_band_id);

ALTER TABLE day_type_time_band
    ADD CONSTRAINT day_type_time_fk FOREIGN KEY (day_type_id) REFERENCES day_type (id);

ALTER TABLE day_type_time_band
    ADD CONSTRAINT time_band_fk FOREIGN KEY (time_band_id) REFERENCES time_band (id);

ALTER TABLE point_of_interest
    ADD CONSTRAINT point_of_interest_opening_hours_fk FOREIGN KEY (point_of_interest_opening_hours_id) REFERENCES point_of_interest_opening_hours (id);

CREATE SEQUENCE point_of_interest_opening_hours_seq
    START WITH 1
    INCREMENT BY 10
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE day_type_seq
    START WITH 1
    INCREMENT BY 10
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE time_band_seq
    START WITH 1
    INCREMENT BY 10
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;