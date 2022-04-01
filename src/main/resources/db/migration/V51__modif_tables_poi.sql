
ALTER TABLE point_of_interest ADD COLUMN IF NOT EXISTS changed_by character varying(255);
ALTER TABLE point_of_interest ADD COLUMN IF NOT EXISTS private_code_type character varying(255);
ALTER TABLE point_of_interest ADD COLUMN IF NOT EXISTS private_code_value character varying(255);
ALTER TABLE point_of_interest ADD COLUMN IF NOT EXISTS polygon_id bigint;
ALTER TABLE point_of_interest ADD COLUMN IF NOT EXISTS accessibility_assessment_id bigint;
ALTER TABLE point_of_interest ADD COLUMN IF NOT EXISTS all_areas_wheelchair_accessible boolean;
ALTER TABLE point_of_interest ADD COLUMN IF NOT EXISTS covered integer;
ALTER TABLE point_of_interest ADD COLUMN IF NOT EXISTS parent_site_ref character varying(255);
ALTER TABLE point_of_interest ADD COLUMN IF NOT EXISTS parent_site_ref_version character varying(255);
ALTER TABLE point_of_interest ADD COLUMN IF NOT EXISTS place_equipments_id bigint;

DROP SEQUENCE point_of_interest_seq;
CREATE SEQUENCE point_of_interest_seq
    START WITH 1
    INCREMENT BY 10
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

DROP SEQUENCE point_of_interest_classification_seq;
CREATE SEQUENCE point_of_interest_classification_seq
    START WITH 1
    INCREMENT BY 10
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER TABLE ONLY point_of_interest_classification DROP column short_name;
ALTER TABLE ONLY point_of_interest_classification DROP column name;

ALTER TABLE point_of_interest_classification ADD COLUMN IF NOT EXISTS short_name_value character varying(255);
ALTER TABLE point_of_interest_classification ADD COLUMN IF NOT EXISTS short_name_lang character varying(255);
ALTER TABLE point_of_interest_classification ADD COLUMN IF NOT EXISTS name_value character varying(255);
ALTER TABLE point_of_interest_classification ADD COLUMN IF NOT EXISTS name_lang character varying(255);
ALTER TABLE point_of_interest_classification ADD COLUMN IF NOT EXISTS version_comment character varying(255);
ALTER TABLE point_of_interest_classification ADD COLUMN IF NOT EXISTS changed_by character varying(255);
ALTER TABLE point_of_interest_classification ADD COLUMN IF NOT EXISTS version bigint;
ALTER TABLE point_of_interest_classification ADD COLUMN IF NOT EXISTS from_date  timestamp without time zone;
ALTER TABLE point_of_interest_classification ADD COLUMN IF NOT EXISTS to_date  timestamp without time zone;
ALTER TABLE point_of_interest_classification ADD COLUMN IF NOT EXISTS created  timestamp without time zone;
ALTER TABLE point_of_interest_classification ADD COLUMN IF NOT EXISTS changed  timestamp without time zone;


CREATE TABLE point_of_interest_classifications (
                                                   point_of_interest_id bigint NOT NULL,
                                                   classifications_id bigint NOT NULL
);


ALTER TABLE ONLY point_of_interest_classifications
    ADD CONSTRAINT poi_class_pkey PRIMARY KEY (point_of_interest_id, classifications_id);


ALTER TABLE ONLY point_of_interest_classifications
    ADD CONSTRAINT fk_poi_class_poi_id FOREIGN KEY (point_of_interest_id) REFERENCES point_of_interest(id);

ALTER TABLE ONLY point_of_interest_classifications
    ADD CONSTRAINT fk_poi_class_poiclass_id FOREIGN KEY (classifications_id) REFERENCES point_of_interest_classification(id);