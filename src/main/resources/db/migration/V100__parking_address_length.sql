-- Some GBFS feeds (ex: Citiz) put free information text in station address, longer than 255 characters
ALTER TABLE parking ALTER COLUMN address TYPE character varying(1000);
