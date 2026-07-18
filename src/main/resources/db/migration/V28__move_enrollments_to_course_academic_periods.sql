ALTER TABLE enrollments
    ADD COLUMN intake_course_id BIGINT,
    ADD COLUMN current_course_academic_period_id BIGINT;

UPDATE enrollments e
SET intake_course_id = a.intake_course_id
FROM students s
JOIN applications a ON a.id = s.application_id
WHERE e.student_id = s.id;

UPDATE enrollments e
SET current_course_academic_period_id = cap.id
FROM semesters sem
JOIN academic_periods ap
  ON ap.period_type = 'SEMESTER'
 AND ap.year_number = sem.year_number
 AND ap.period_number = sem.semester_number
JOIN course_academic_periods cap ON cap.academic_period_id = ap.id
WHERE e.current_semester_id = sem.id
  AND cap.course_id = e.course_id;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM enrollments WHERE intake_course_id IS NULL
               OR current_course_academic_period_id IS NULL) THEN
        RAISE EXCEPTION 'Some enrollments could not be mapped to the new academic-period model';
    END IF;
END $$;

ALTER TABLE enrollments
    ALTER COLUMN intake_course_id SET NOT NULL,
    ALTER COLUMN current_course_academic_period_id SET NOT NULL,
    ALTER COLUMN course_curriculum_id DROP NOT NULL,
    ALTER COLUMN course_id DROP NOT NULL,
    ALTER COLUMN current_semester_id DROP NOT NULL,
    ADD CONSTRAINT fk_enrollment_intake_course FOREIGN KEY (intake_course_id)
        REFERENCES intake_courses(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_enrollment_current_course_period FOREIGN KEY (current_course_academic_period_id)
        REFERENCES course_academic_periods(id) ON DELETE RESTRICT;

CREATE INDEX idx_enrollments_intake_course ON enrollments(intake_course_id);
CREATE INDEX idx_enrollments_current_course_period ON enrollments(current_course_academic_period_id);
