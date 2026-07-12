-- V8__remove_course_id_from_intakes.sql

ALTER TABLE intakes
DROP CONSTRAINT IF EXISTS fk_intakes_course,
DROP CONSTRAINT IF EXISTS fk_intakes_course_id,
DROP CONSTRAINT IF EXISTS intakes_course_id_fkey; -- Common default JPA name

ALTER TABLE intakes
DROP COLUMN IF EXISTS course_id;
