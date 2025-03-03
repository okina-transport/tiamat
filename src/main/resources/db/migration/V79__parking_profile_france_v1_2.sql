CREATE TABLE parking_properties_parking_vehicle_types
(
    parking_properties_id bigint NOT NULL,
    parking_vehicle_types character varying(255)
);

ALTER TABLE parking_properties_parking_vehicle_types
    OWNER TO tiamat;

ALTER TABLE parking_properties_parking_vehicle_types
    ADD CONSTRAINT fk_pppvt_pp_id FOREIGN KEY (parking_properties_id) REFERENCES parking_properties (id);

CREATE TABLE passenger_capacity
(
    id                        bigint PRIMARY KEY,
    netex_id                  character varying(255),
    changed                   timestamp without time zone,
    created                   timestamp without time zone,
    from_date                 timestamp without time zone,
    to_date                   timestamp without time zone,
    version                   bigint NOT NULL,
    version_comment           character varying(255),
    description_lang          character varying(5),
    description_value         character varying(4000),
    name_lang                 character varying(5),
    name_value                character varying(255),
    short_name_lang           character varying(5),
    short_name_value          character varying(255),
    changed_by                character varying(255),
    fare_class                character varying(20),
    total_capacity            bigint,
    seating_capacity          bigint,
    standing_capacity         bigint,
    special_place_capacity    bigint,
    pushchair_capacity        bigint,
    wheelchair_place_capacity bigint
);

ALTER TABLE passenger_capacity
    OWNER TO tiamat;

CREATE SEQUENCE passenger_capacity_seq
    START WITH 1
    INCREMENT BY 10
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE passenger_capacity_key_values
(
    passenger_capacity_id bigint                 NOT NULL,
    key_values_id         bigint                 NOT NULL,
    key_values_key        character varying(255) NOT NULL
);

ALTER TABLE passenger_capacity_key_values
    OWNER TO tiamat;

ALTER TABLE passenger_capacity_key_values
    ADD CONSTRAINT passenger_capacity_key_values_id_key UNIQUE (key_values_id);

ALTER TABLE passenger_capacity_key_values
    ADD CONSTRAINT fk_pckv_v_id FOREIGN KEY (key_values_id) REFERENCES value;

ALTER TABLE passenger_capacity_key_values
    ADD CONSTRAINT fk_pckv_pc_id FOREIGN KEY (passenger_capacity_id) REFERENCES passenger_capacity;

CREATE TABLE transport_type
(
    id                    bigint PRIMARY KEY,
    netex_id              character varying(255),
    changed               timestamp without time zone,
    created               timestamp without time zone,
    from_date             timestamp without time zone,
    to_date               timestamp without time zone,
    version               bigint NOT NULL,
    version_comment       character varying(255),
    description_lang      character varying(5),
    description_value     character varying(4000),
    name_lang             character varying(5),
    name_value            character varying(255),
    short_name_lang       character varying(5),
    short_name_value      character varying(255),
    changed_by            character varying(255),
    euro_class            character varying(255),
    reversing_direction   boolean,
    self_propelled        boolean,
    propulsion_type       character varying(255),
    fuel_type             character varying(255),
    type_of_fuel          character varying(255),
    maximum_range         bigint,
    transport_mode        character varying(255),
    passenger_capacity_id bigint
);

ALTER TABLE transport_type
    OWNER TO tiamat;

CREATE SEQUENCE transport_type_seq
    START WITH 1
    INCREMENT BY 10
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER TABLE transport_type
    ADD CONSTRAINT fk_tt_pc_id FOREIGN KEY (passenger_capacity_id) REFERENCES passenger_capacity;

CREATE TABLE transport_type_key_values
(
    transport_type_id bigint                 NOT NULL,
    key_values_id     bigint                 NOT NULL,
    key_values_key    character varying(255) NOT NULL
);

ALTER TABLE transport_type
    OWNER TO tiamat;

ALTER TABLE transport_type_key_values
    ADD CONSTRAINT transport_type_key_values_id_key unique (key_values_id);

ALTER TABLE transport_type_key_values
    ADD CONSTRAINT fk_ttkv_v_id FOREIGN KEY (key_values_id) REFERENCES value;

ALTER TABLE transport_type_key_values
    ADD CONSTRAINT fk_ttkv_tt_id FOREIGN KEY (transport_type_id) REFERENCES transport_type;

CREATE TABLE parking_transport_types
(
    parking_id         bigint NOT NULL,
    transport_types_id bigint NOT NULL
);

ALTER TABLE parking_transport_types
    OWNER TO tiamat;

ALTER TABLE parking_transport_types
    ADD CONSTRAINT fk_pvt_parking_id FOREIGN KEY (parking_id) REFERENCES parking;

ALTER TABLE parking_transport_types
    ADD CONSTRAINT fk_pvt_vt_id FOREIGN KEY (transport_types_id) REFERENCES transport_type;

CREATE TABLE type_of_payment_method
(
    id                bigint PRIMARY KEY,
    netex_id          character varying(255),
    changed           timestamp without time zone,
    created           timestamp without time zone,
    from_date         timestamp without time zone,
    to_date           timestamp without time zone,
    version           bigint NOT NULL,
    version_comment   character varying(255),
    description_lang  character varying(5),
    description_value character varying(4000),
    name_lang         character varying(5),
    name_value        character varying(255),
    short_name_lang   character varying(5),
    short_name_value  character varying(255),
    changed_by        character varying(255),
    automated_use     boolean,
    payment_method    character varying(255)
);

ALTER TABLE type_of_payment_method
    OWNER TO tiamat;

CREATE SEQUENCE type_of_payment_method_seq
    START WITH 1
    INCREMENT BY 10
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE type_of_payment_method_key_values
(
    type_of_payment_method_id bigint                 NOT NULL,
    key_values_id             bigint                 NOT NULL,
    key_values_key            character varying(255) NOT NULL
);

ALTER TABLE type_of_payment_method_key_values
    OWNER TO tiamat;

ALTER TABLE type_of_payment_method_key_values
    ADD CONSTRAINT type_of_payment_method_key_values_id_key unique (key_values_id);

ALTER TABLE type_of_payment_method_key_values
    ADD CONSTRAINT fk_topmkv_v_id FOREIGN KEY (key_values_id) REFERENCES value;

ALTER TABLE type_of_payment_method_key_values
    ADD CONSTRAINT fk_topmkv_topm_id FOREIGN KEY (type_of_payment_method_id) REFERENCES type_of_payment_method;

CREATE TABLE parking_type_of_payment_methods
(
    parking_id                 bigint NOT NULL,
    type_of_payment_methods_id bigint NOT NULL
);

ALTER TABLE parking_type_of_payment_methods
    OWNER TO tiamat;

ALTER TABLE parking_type_of_payment_methods
    ADD CONSTRAINT fk_ptopm_parking_id FOREIGN KEY (parking_id) REFERENCES parking;

ALTER TABLE parking_type_of_payment_methods
    ADD CONSTRAINT fk_ptopm_topm_id FOREIGN KEY (type_of_payment_methods_id) REFERENCES type_of_payment_method;

ALTER TABLE parking_capacity
    ADD COLUMN transport_type_ref character varying(255);