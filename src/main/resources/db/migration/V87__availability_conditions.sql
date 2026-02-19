CREATE TABLE IF NOT EXISTS availability_condition (
                               id BIGINT NOT NULL,
                               netex_id character varying(255),
                               is_available boolean,
                                changed timestamp without time zone,
                                created timestamp without time zone,
                                from_date timestamp without time zone,
                                to_date timestamp without time zone,
                                version bigint NOT NULL,
                                CONSTRAINT availability_condition_pkey PRIMARY KEY (id)
);



CREATE TABLE IF NOT EXISTS postal_address (
                                                      id BIGINT NOT NULL,
                                                      netex_id character varying(255),
                                                      street character varying(255),
                                                      town character varying(255),
                                                      postal_region character varying(255),
                                                        changed timestamp without time zone,
                                                        created timestamp without time zone,
                                                        from_date timestamp without time zone,
                                                        to_date timestamp without time zone,
                                                        version bigint NOT NULL,
                                                      CONSTRAINT postal_address_pkey PRIMARY KEY (id)
    );


ALTER TABLE parking ADD COLUMN IF NOT EXISTS postal_address_id BIGINT;
ALTER TABLE parking DROP CONSTRAINT IF EXISTS parking_postal_address_id_fkey;
ALTER TABLE parking ADD CONSTRAINT parking_postal_address_id_fkey foreign key (postal_address_id) references postal_address(id);


CREATE TABLE IF NOT EXISTS availability_condition_day_types (
                            availability_condition_id bigint NOT NULL,
                            day_types_id bigint NOT NULL,
                            CONSTRAINT avail_cond_day_type_pkey PRIMARY KEY (availability_condition_id, day_types_id),
                            CONSTRAINT avcd_avail_fk FOREIGN KEY (availability_condition_id) REFERENCES availability_condition(id),
                            CONSTRAINT avcd_day_type_fk FOREIGN KEY (day_types_id) REFERENCES day_type(id)
);


CREATE TABLE IF NOT EXISTS parking_availability_conditions (
                                                                parking_id bigint NOT NULL,
                                                                availability_conditions_id bigint NOT NULL,
                                                                CONSTRAINT parking_avail_cond_pkey PRIMARY KEY (parking_id, availability_conditions_id),
                                                                CONSTRAINT parking_availcond_parking_fk FOREIGN KEY (parking_id) REFERENCES parking(id),
                                                                CONSTRAINT parking_availcond_avail_fk FOREIGN KEY (availability_conditions_id) REFERENCES availability_condition(id)
);

ALTER TABLE time_band ADD COLUMN IF NOT EXISTS day_offset integer;
ALTER TABLE parking_area ADD COLUMN IF NOT EXISTS public_use character varying(255);
ALTER TABLE parking_bay ADD COLUMN IF NOT EXISTS public_use character varying(255);
ALTER TABLE parking ADD COLUMN IF NOT EXISTS public_use character varying(255);
ALTER TABLE quay ADD COLUMN IF NOT EXISTS public_use character varying(255);
ALTER TABLE stop_place ADD COLUMN IF NOT EXISTS public_use character varying(255);
ALTER TABLE point_of_interest ADD COLUMN IF NOT EXISTS public_use character varying(255);
ALTER TABLE access_space ADD COLUMN IF NOT EXISTS public_use character varying(255);
ALTER TABLE boarding_position ADD COLUMN IF NOT EXISTS public_use character varying(255);

ALTER TABLE parking_area ADD COLUMN IF NOT EXISTS label_value character varying(255);
ALTER TABLE parking_area ADD COLUMN IF NOT EXISTS label_lang character varying(5);

ALTER TABLE parking_bay ADD COLUMN IF NOT EXISTS label_value character varying(255);
ALTER TABLE parking_bay ADD COLUMN IF NOT EXISTS label_lang character varying(5);
ALTER TABLE parking_area ADD COLUMN IF NOT EXISTS nb_bays_with_recharging bigint;








ALTER TABLE time_band ALTER COLUMN start_time TYPE TIME USING start_time::time;
ALTER TABLE time_band ALTER COLUMN end_time TYPE TIME USING end_time::time;


CREATE SEQUENCE IF NOT EXISTS availability_condition_seq
    START WITH 1
    INCREMENT BY 10
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS postal_address_seq
    START WITH 1
    INCREMENT BY 10
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;






