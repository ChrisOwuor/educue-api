ALTER TABLE student_unit_registrations
    ADD COLUMN course_unit_placement_id BIGINT;

-- V23 intentionally did not guess legacy placements. For registrations that
-- still point at the old semester model, create only the exact placement
-- needed by that student's course period and intake year.
INSERT INTO course_unit_placements (
    uuid, course_academic_period_id, unit_id, unit_type,
    effective_from_intake_year, active, version, created_at, updated_at
)
SELECT DISTINCT
    gen_random_uuid(),
    enrollment.current_course_academic_period_id,
    semester_unit.unit_id,
    CASE WHEN semester_unit.is_mandatory THEN 'CORE' ELSE 'ELECTIVE' END,
    EXTRACT(YEAR FROM intake.start_date)::INTEGER,
    TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM student_unit_registrations registration
JOIN enrollments enrollment ON enrollment.id = registration.enrollment_id
JOIN intake_courses intake_course ON intake_course.id = enrollment.intake_course_id
JOIN intakes intake ON intake.id = intake_course.intake_id
JOIN semester_units semester_unit ON semester_unit.id = registration.semester_unit_id
WHERE NOT EXISTS (
    SELECT 1
    FROM course_unit_placements placement
    WHERE placement.course_academic_period_id = enrollment.current_course_academic_period_id
      AND placement.unit_id = semester_unit.unit_id
      AND placement.effective_from_intake_year <= EXTRACT(YEAR FROM intake.start_date)::INTEGER
      AND (placement.effective_to_intake_year IS NULL
           OR placement.effective_to_intake_year >= EXTRACT(YEAR FROM intake.start_date)::INTEGER)
)
ON CONFLICT (course_academic_period_id, unit_id, effective_from_intake_year) DO NOTHING;

-- Preserve existing registrations by finding the placement for the same unit
-- in the enrollment's already-migrated course academic period. The intake
-- year selects the correct effective version when a unit placement changed.
UPDATE student_unit_registrations registration
SET course_unit_placement_id = (
    SELECT placement.id AS placement_id
    FROM enrollments enrollment
    JOIN intake_courses intake_course ON intake_course.id = enrollment.intake_course_id
    JOIN intakes intake ON intake.id = intake_course.intake_id
    JOIN semester_units semester_unit ON semester_unit.id = registration.semester_unit_id
    JOIN course_unit_placements placement
      ON placement.course_academic_period_id = enrollment.current_course_academic_period_id
     AND placement.unit_id = semester_unit.unit_id
     AND placement.effective_from_intake_year <= EXTRACT(YEAR FROM intake.start_date)::INTEGER
     AND (placement.effective_to_intake_year IS NULL
          OR placement.effective_to_intake_year >= EXTRACT(YEAR FROM intake.start_date)::INTEGER)
    WHERE enrollment.id = registration.enrollment_id
    ORDER BY placement.effective_from_intake_year DESC
    LIMIT 1
);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM student_unit_registrations
        WHERE course_unit_placement_id IS NULL
    ) THEN
        RAISE EXCEPTION 'Some student unit registrations could not be mapped to course unit placements';
    END IF;
END $$;

ALTER TABLE student_unit_registrations
    ALTER COLUMN course_unit_placement_id SET NOT NULL,
    ALTER COLUMN semester_unit_id DROP NOT NULL,
    ADD CONSTRAINT fk_student_registration_placement
        FOREIGN KEY (course_unit_placement_id)
        REFERENCES course_unit_placements(id) ON DELETE RESTRICT;

ALTER TABLE student_unit_registrations
    DROP CONSTRAINT IF EXISTS uq_student_unit_registration,
    ADD CONSTRAINT uq_student_unit_registration_placement
        UNIQUE (enrollment_id, course_unit_placement_id);

CREATE INDEX idx_student_registration_placement
    ON student_unit_registrations(course_unit_placement_id);
