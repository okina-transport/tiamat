ALTER TABLE point_of_interest ADD CONSTRAINT point_of_interest_netex_id_version_constraint UNIQUE (netex_id, version);
