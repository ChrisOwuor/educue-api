-- Add the new columns without breaking existing records
ALTER TABLE courses
    ADD COLUMN qualification_type VARCHAR(50),
    ADD COLUMN study_mode VARCHAR(50),
    ADD COLUMN total_credits INTEGER,
    ADD COLUMN version BIGINT DEFAULT 0,
    ADD COLUMN award_title VARCHAR(150);

-- Backfill optimistic-locking version for existing records
UPDATE courses
SET version = 0
WHERE version IS NULL;

-- Replace BACHELOR with a valid value from your QualificationType enum
UPDATE courses
SET qualification_type = 'BACHELOR'
WHERE qualification_type IS NULL;

-- Match the entity's nullable = false requirement
ALTER TABLE courses
    ALTER COLUMN qualification_type SET NOT NULL,
ALTER COLUMN version SET NOT NULL;
