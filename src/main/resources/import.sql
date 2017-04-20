CREATE INDEX stop_place_centroid_index ON stop_place USING GIST ( centroid );

CREATE INDEX persistable_polygon_index ON persistable_polygon USING GIST (polygon);

DROP TABLE IF EXISTS id_generator;
CREATE TABLE id_generator(table_name text, id_value bigint, CONSTRAINT id_constraint UNIQUE (table_name, id_value));

CREATE index table_name_index ON id_generator(table_name);
CREATE index id_value_index ON id_generator(id_value);

CREATE index value_id_index ON value_items (value_id);

CREATE INDEX trgm_idx ON stop_place USING GIST (name_value gist_trgm_ops);