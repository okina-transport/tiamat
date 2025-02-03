CREATE TABLE organisation (
    id bigint NOT NULL primary key,
    netex_id character varying(255),
    changed timestamp without time zone,
    created timestamp without time zone,
    name character varying(255),
    short_name character varying(255),
    type character varying(10),
    operator character varying(255),
    organisation_url character varying(255),
    purchase_url character varying(255),
    phone_number character varying(50),
    email character varying(255),
    android_store_uri character varying(255),
    android_discovery_uri character varying(255),
    ios_store_uri character varying(255),
    ios_discovery_uri character varying(255),
    language character varying(255),
    timezone character varying(255)
);

alter table parking ADD COLUMN organisation_id bigint,
ADD CONSTRAINT fk_organisation
FOREIGN KEY (organisation_id) REFERENCES organisation(id);