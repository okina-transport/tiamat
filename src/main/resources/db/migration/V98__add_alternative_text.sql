CREATE TABLE IF NOT EXISTS alternative_text
(
    id                      BIGINT PRIMARY KEY,
    netex_id                VARCHAR(255),
    version                 BIGINT NOT NULL,
    version_comment         VARCHAR(255),
    created                 TIMESTAMP WITHOUT TIME ZONE,
    changed                 TIMESTAMP WITHOUT TIME ZONE,
    changed_by              VARCHAR(255),
    from_date               TIMESTAMP WITHOUT TIME ZONE,
    to_date                 TIMESTAMP WITHOUT TIME ZONE,
    attribute_name          VARCHAR(255),
    use_for_language        VARCHAR(255),
    text_value              VARCHAR(4000),
    text_lang               VARCHAR(255),
    data_managed_object_ref BYTEA
);

CREATE SEQUENCE IF NOT EXISTS alternative_text_seq
    START WITH 1
    INCREMENT BY 10
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE IF NOT EXISTS stop_place_alternative_texts
(
    stop_place_id        BIGINT NOT NULL,
    alternative_texts_id BIGINT NOT NULL
);

ALTER TABLE stop_place_alternative_texts
    ADD CONSTRAINT uk_stop_place_alternative_texts_id UNIQUE (alternative_texts_id);

ALTER TABLE stop_place_alternative_texts
    ADD CONSTRAINT fk_stop_place_alternative_texts_stop_place FOREIGN KEY (stop_place_id) REFERENCES stop_place (id);

ALTER TABLE stop_place_alternative_texts
    ADD CONSTRAINT fk_stop_place_alternative_texts_alternative_text FOREIGN KEY (alternative_texts_id) REFERENCES alternative_text (id);

CREATE TABLE IF NOT EXISTS quay_alternative_texts
(
    quay_id              BIGINT NOT NULL,
    alternative_texts_id BIGINT NOT NULL
);

ALTER TABLE quay_alternative_texts
    ADD CONSTRAINT uk_quay_alternative_texts_id UNIQUE (alternative_texts_id);

ALTER TABLE quay_alternative_texts
    ADD CONSTRAINT fk_quay_alternative_texts_quay FOREIGN KEY (quay_id) REFERENCES quay (id);

ALTER TABLE quay_alternative_texts
    ADD CONSTRAINT fk_quay_alternative_texts_alternative_text FOREIGN KEY (alternative_texts_id) REFERENCES alternative_text (id);
