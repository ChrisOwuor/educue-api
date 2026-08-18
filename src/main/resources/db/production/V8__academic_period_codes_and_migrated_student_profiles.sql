ALTER TABLE academic_periods DROP CONSTRAINT IF EXISTS uk_academic_period_code;
ALTER TABLE academic_periods ADD CONSTRAINT uk_academic_period_type_code UNIQUE (period_type, code);

ALTER TABLE students ADD COLUMN IF NOT EXISTS profile_completion_required BOOLEAN NOT NULL DEFAULT FALSE;
