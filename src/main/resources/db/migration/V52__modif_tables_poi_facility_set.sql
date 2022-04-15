ALTER TABLE point_of_interest_facility_set ADD COLUMN IF NOT EXISTS changed  timestamp without time zone;
ALTER TABLE point_of_interest_facility_set ADD COLUMN IF NOT EXISTS created  timestamp without time zone;
ALTER TABLE point_of_interest_facility_set ADD COLUMN IF NOT EXISTS version bigint;
ALTER TABLE point_of_interest_facility_set ADD COLUMN IF NOT EXISTS from_date  timestamp without time zone;
ALTER TABLE point_of_interest_facility_set ADD COLUMN IF NOT EXISTS to_date  timestamp without time zone;


DROP SEQUENCE point_of_interest_facility_set_seq;
CREATE SEQUENCE point_of_interest_facility_set_seq
    START WITH 1
    INCREMENT BY 10
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;