-- Step 1: Add the column back allowing NULLs temporarily
ALTER TABLE courses ADD COLUMN duration_unit VARCHAR(50);

-- Step 2: (Optional) Populate default values for existing rows so they are not empty
UPDATE courses SET duration_unit = 'YEARS' WHERE duration_unit IS NULL;
