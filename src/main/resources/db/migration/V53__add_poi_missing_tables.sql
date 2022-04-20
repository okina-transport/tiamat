CREATE TABLE point_of_interest_adjacent_sites (point_of_interest_id bigint NOT NULL, ref VARCHAR(255), version VARCHAR(255));

CREATE TABLE point_of_interest_alternative_names (point_of_interest_id bigint NOT NULL, alternative_names_id bigint NOT NULL);
ALTER TABLE point_of_interest_alternative_names ADD CONSTRAINT point_of_interest_alternative_names_id_key UNIQUE (alternative_names_id);
ALTER TABLE point_of_interest_alternative_names ADD CONSTRAINT point_of_interest_alternative_names_alternative_names_id_fkey FOREIGN KEY (alternative_names_id) REFERENCES alternative_name;
ALTER TABLE point_of_interest_alternative_names ADD CONSTRAINT point_of_interest_alternative_names_point_of_interest_id_fkey FOREIGN KEY (point_of_interest_id) REFERENCES point_of_interest;

CREATE TABLE point_of_interest_classification_key_values (point_of_interest_classification_id bigint NOT NULL, key_values_id bigint NOT NULL, key_values_key character varying(255) NOT NULL);
