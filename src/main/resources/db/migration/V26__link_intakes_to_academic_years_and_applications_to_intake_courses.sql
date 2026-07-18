ALTER TABLE intakes
    ADD COLUMN academic_year_id BIGINT;

-- Match each existing intake to the academic year containing its start date.
UPDATE intakes i
SET academic_year_id = ay.id
FROM academic_years ay
WHERE i.academic_year_id IS NULL
  AND i.start_date BETWEEN ay.start_date AND ay.end_date;

-- Legacy intake dates may predate the configured ranges. Associate those
-- with the closest configured academic year rather than discarding data.
UPDATE intakes i
SET academic_year_id = (
    SELECT ay.id
    FROM academic_years ay
    ORDER BY ABS(ay.start_date - i.start_date), ay.start_date
    LIMIT 1
)
WHERE i.academic_year_id IS NULL;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM intakes WHERE academic_year_id IS NULL) THEN
        RAISE EXCEPTION 'Some intakes could not be matched to an academic year by start_date';
    END IF;
END $$;

ALTER TABLE intakes
    ALTER COLUMN academic_year_id SET NOT NULL,
    ADD CONSTRAINT fk_intakes_academic_year
        FOREIGN KEY (academic_year_id) REFERENCES academic_years(id) ON DELETE RESTRICT;

CREATE INDEX idx_intakes_academic_year ON intakes(academic_year_id);

ALTER TABLE applications
    ADD COLUMN application_number VARCHAR(40),
    ADD COLUMN intake_course_id BIGINT;

-- The pair was already protected by intake_courses' unique constraint, so
-- every valid legacy application maps to exactly one offering.
UPDATE applications a
SET intake_course_id = ic.id
FROM intake_courses ic
WHERE ic.intake_id = a.intake_id
  AND ic.course_id = a.course_id;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM applications WHERE intake_course_id IS NULL) THEN
        RAISE EXCEPTION 'Some applications have no matching intake_courses row';
    END IF;
END $$;

UPDATE applications
SET application_number = 'APP-' || LPAD(id::text, 10, '0')
WHERE application_number IS NULL;

ALTER TABLE applications
    ALTER COLUMN application_number SET NOT NULL,
    ALTER COLUMN intake_course_id SET NOT NULL,
    ADD CONSTRAINT uk_applications_application_number UNIQUE (application_number),
    ADD CONSTRAINT fk_applications_intake_course
        FOREIGN KEY (intake_course_id) REFERENCES intake_courses(id) ON DELETE RESTRICT;

DROP INDEX IF EXISTS idx_applications_intake_id;

ALTER TABLE applications
    DROP COLUMN intake_id,
    DROP COLUMN course_id;

CREATE INDEX idx_applications_intake_course ON applications(intake_course_id);
