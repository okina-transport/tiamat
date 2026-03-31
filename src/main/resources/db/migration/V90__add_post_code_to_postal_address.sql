ALTER TABLE postal_address
    ADD COLUMN IF NOT EXISTS post_code character varying(255);