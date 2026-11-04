ALTER TABLE parking
    ADD COLUMN IF NOT EXISTS url character varying(255);

CREATE TABLE IF NOT EXISTS parking_cards_accepted
(
    parking_id     bigint NOT NULL,
    cards_accepted character varying(255)
);

ALTER TABLE ONLY parking_cards_accepted
    ADD CONSTRAINT parking_cards_accepted_fk FOREIGN KEY (parking_id) REFERENCES parking (id);

CREATE TABLE IF NOT EXISTS day_type_days_of_week
(
    day_type_id  bigint NOT NULL,
    days_of_week character varying(255)
);

ALTER TABLE ONLY day_type_days_of_week
    ADD CONSTRAINT day_type_days_of_week_fk FOREIGN KEY (day_type_id) REFERENCES day_type (id);

CREATE TABLE IF NOT EXISTS day_type_holiday_types
(
    day_type_id   bigint NOT NULL,
    holiday_types character varying(255)
);

ALTER TABLE ONLY day_type_holiday_types
    ADD CONSTRAINT day_type_holiday_types_fk FOREIGN KEY (day_type_id) REFERENCES day_type (id);

INSERT INTO day_type_days_of_week (day_type_id, days_of_week)
SELECT id, day_of_week
FROM day_type
WHERE day_of_week IS NOT NULL;

ALTER TABLE day_type
    DROP COLUMN day_of_week;
