-- Course duration is stored as a single value. Its interpretation is defined
-- by the institution rather than by a separate unit or semester count.
ALTER TABLE courses
    DROP COLUMN duration_unit,
    DROP COLUMN total_semesters;


-- Existing courses predate required study-mode selection.
UPDATE courses
SET study_mode = 'FULL_TIME'
WHERE study_mode IS NULL;

ALTER TABLE courses
    ALTER COLUMN study_mode SET NOT NULL;
