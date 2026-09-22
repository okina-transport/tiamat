ALTER TABLE job ADD COLUMN IF NOT EXISTS total_count integer;
ALTER TABLE job ADD COLUMN IF NOT EXISTS remaining_count integer;
ALTER TABLE job ADD COLUMN IF NOT EXISTS provider text;
ALTER TABLE job ADD COLUMN IF NOT EXISTS merge_mode text;
