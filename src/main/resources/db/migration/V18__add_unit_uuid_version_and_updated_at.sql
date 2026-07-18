-- Required for gen_random_uuid().
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Step 1: Add columns as nullable so existing rows are not rejected.
ALTER TABLE units
    ADD COLUMN uuid UUID,
    ADD COLUMN version BIGINT DEFAULT 0,
    ADD COLUMN updated_at TIMESTAMP;

-- Step 2: Backfill existing unit records.
UPDATE units
SET uuid = gen_random_uuid()
WHERE uuid IS NULL;

UPDATE units
SET version = 0
WHERE version IS NULL;

UPDATE units
SET updated_at = COALESCE(created_at, CURRENT_TIMESTAMP)
WHERE updated_at IS NULL;

-- Step 3: Make the columns required after existing rows are populated.
ALTER TABLE units
    ALTER COLUMN uuid SET NOT NULL,
ALTER COLUMN version SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

-- Step 4: Ensure every public UUID remains unique.
ALTER TABLE units
    ADD CONSTRAINT uk_units_uuid UNIQUE (uuid);

ALTER TABLE units
    ALTER COLUMN version DROP DEFAULT;
