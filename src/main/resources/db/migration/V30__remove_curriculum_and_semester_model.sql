ALTER TABLE trainer_assignments
    ADD COLUMN course_unit_placement_id BIGINT;

-- Materialize placements needed by legacy trainer assignments before the old
-- semester tables are removed. The assignment start year becomes the first
-- effective intake year when no equivalent placement exists yet.
INSERT INTO course_unit_placements (
    uuid, course_academic_period_id, unit_id, unit_type,
    effective_from_intake_year, active, version, created_at, updated_at
)
SELECT DISTINCT
    gen_random_uuid(), cap.id, su.unit_id,
    CASE WHEN su.is_mandatory THEN 'CORE' ELSE 'ELECTIVE' END,
    EXTRACT(YEAR FROM ta.effective_from)::INTEGER,
    TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM trainer_assignments ta
JOIN semester_units su ON su.id = ta.semester_unit_id
JOIN semesters semester ON semester.id = su.semester_id
JOIN course_curriculums curriculum ON curriculum.id = semester.course_curriculum_id
JOIN academic_periods ap
  ON ap.period_type = 'SEMESTER'
 AND ap.year_number = semester.year_number
 AND ap.period_number = semester.semester_number
JOIN course_academic_periods cap
  ON cap.course_id = curriculum.course_id
 AND cap.academic_period_id = ap.id
WHERE NOT EXISTS (
    SELECT 1 FROM course_unit_placements placement
    WHERE placement.course_academic_period_id = cap.id
      AND placement.unit_id = su.unit_id
      AND placement.effective_from_intake_year <= EXTRACT(YEAR FROM ta.effective_from)::INTEGER
      AND (placement.effective_to_intake_year IS NULL
           OR placement.effective_to_intake_year >= EXTRACT(YEAR FROM ta.effective_from)::INTEGER)
)
ON CONFLICT (course_academic_period_id, unit_id, effective_from_intake_year) DO NOTHING;

UPDATE trainer_assignments ta
SET course_unit_placement_id = (
    SELECT placement.id
    FROM semester_units su
    JOIN semesters semester ON semester.id = su.semester_id
    JOIN course_curriculums curriculum ON curriculum.id = semester.course_curriculum_id
    JOIN academic_periods ap
      ON ap.period_type = 'SEMESTER'
     AND ap.year_number = semester.year_number
     AND ap.period_number = semester.semester_number
    JOIN course_academic_periods cap
      ON cap.course_id = curriculum.course_id
     AND cap.academic_period_id = ap.id
    JOIN course_unit_placements placement
      ON placement.course_academic_period_id = cap.id
     AND placement.unit_id = su.unit_id
    WHERE su.id = ta.semester_unit_id
    ORDER BY placement.effective_from_intake_year DESC
    LIMIT 1
);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM trainer_assignments WHERE course_unit_placement_id IS NULL) THEN
        RAISE EXCEPTION 'Some trainer assignments could not be mapped to course unit placements';
    END IF;
END $$;

ALTER TABLE trainer_assignments
    ALTER COLUMN course_unit_placement_id SET NOT NULL,
    ADD CONSTRAINT fk_trainer_assignment_placement
        FOREIGN KEY (course_unit_placement_id) REFERENCES course_unit_placements(id) ON DELETE RESTRICT;

CREATE INDEX idx_trainer_assignment_placement
    ON trainer_assignments(course_unit_placement_id);

ALTER TABLE trainer_assignments DROP COLUMN semester_unit_id;
ALTER TABLE student_unit_registrations DROP COLUMN semester_unit_id;
ALTER TABLE enrollments
    DROP COLUMN course_curriculum_id,
    DROP COLUMN course_id,
    DROP COLUMN current_semester_id;

DROP TABLE semester_units;
ALTER TABLE course_curriculums DROP CONSTRAINT IF EXISTS fk_curriculum_first_semester;
DROP TABLE semesters;
DROP TABLE course_curriculums;

DROP TYPE IF EXISTS semester_unit_category;
