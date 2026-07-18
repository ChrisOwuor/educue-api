-- Add replacement columns as nullable so databases containing legacy rows can migrate.
ALTER TABLE fee_structures
    DROP CONSTRAINT uk_fee_structure,
    ADD COLUMN intake_course_id BIGINT,
    ADD COLUMN course_academic_period_id BIGINT;

-- Ensure legacy semester-based courses have the corresponding course period.
INSERT INTO course_academic_periods (
    course_id, academic_period_id, position, version, created_at
)
SELECT DISTINCT
    fs.course_id,
    ap.id,
    ap.sequence_number,
    0,
    CURRENT_TIMESTAMP
FROM fee_structures fs
JOIN semesters s ON s.id = fs.semester_id
JOIN academic_periods ap
  ON ap.period_type = 'SEMESTER'
 AND ap.year_number = s.year_number
 AND ap.period_number = s.semester_number
WHERE NOT EXISTS (
    SELECT 1
    FROM course_academic_periods cap
    WHERE cap.course_id = fs.course_id
      AND cap.academic_period_id = ap.id
)
AND NOT EXISTS (
    SELECT 1
    FROM course_academic_periods cap
    WHERE cap.course_id = fs.course_id
      AND cap.position = ap.sequence_number
);

UPDATE fee_structures fs
SET intake_course_id = ic.id
FROM intake_courses ic
WHERE ic.intake_id = fs.intake_id
  AND ic.course_id = fs.course_id;

UPDATE fee_structures fs
SET course_academic_period_id = cap.id
FROM semesters s
JOIN academic_periods ap
  ON ap.period_type = 'SEMESTER'
 AND ap.year_number = s.year_number
 AND ap.period_number = s.semester_number
JOIN course_academic_periods cap ON cap.academic_period_id = ap.id
WHERE s.id = fs.semester_id
  AND cap.course_id = fs.course_id;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM fee_structures
        WHERE intake_course_id IS NULL OR course_academic_period_id IS NULL
    ) THEN
        RAISE EXCEPTION 'Some fee structures could not be mapped to an intake course and course academic period';
    END IF;
END $$;

ALTER TABLE fee_structures
    ALTER COLUMN intake_course_id SET NOT NULL,
    ALTER COLUMN course_academic_period_id SET NOT NULL,
    ADD CONSTRAINT fk_fee_structure_intake_course
        FOREIGN KEY (intake_course_id) REFERENCES intake_courses(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_fee_structure_course_period
        FOREIGN KEY (course_academic_period_id) REFERENCES course_academic_periods(id) ON DELETE RESTRICT,
    DROP COLUMN intake_id,
    DROP COLUMN course_id,
    DROP COLUMN semester_id,
    ADD CONSTRAINT uk_fee_structure UNIQUE (intake_course_id, course_academic_period_id);

CREATE INDEX idx_fee_structures_intake_course ON fee_structures(intake_course_id);
CREATE INDEX idx_fee_structures_course_period ON fee_structures(course_academic_period_id);

ALTER TABLE payments
    DROP CONSTRAINT fk_payment_semester,
    ADD COLUMN applied_course_academic_period_id BIGINT;

UPDATE payments p
SET applied_course_academic_period_id = cap.id
FROM semesters s
JOIN course_curriculums cc ON cc.id = s.course_curriculum_id
JOIN academic_periods ap
  ON ap.period_type = 'SEMESTER'
 AND ap.year_number = s.year_number
 AND ap.period_number = s.semester_number
JOIN course_academic_periods cap
  ON cap.course_id = cc.course_id
 AND cap.academic_period_id = ap.id
WHERE p.applied_semester_id = s.id;

ALTER TABLE payments
    ADD CONSTRAINT fk_payment_course_period
        FOREIGN KEY (applied_course_academic_period_id)
        REFERENCES course_academic_periods(id) ON DELETE RESTRICT,
    DROP COLUMN applied_semester_id;

ALTER TABLE fee_ledger
    DROP CONSTRAINT fk_fee_ledger_semester,
    ADD COLUMN course_academic_period_id BIGINT;

-- Billing rows map directly through their fee structure.
UPDATE fee_ledger fl
SET course_academic_period_id = fs.course_academic_period_id
FROM fee_structures fs
WHERE fl.fee_structure_id = fs.id;

-- Payment ledger rows map through their payment when one was specified.
UPDATE fee_ledger fl
SET course_academic_period_id = p.applied_course_academic_period_id
FROM payments p
WHERE fl.course_academic_period_id IS NULL
  AND fl.payment_id = p.id;

ALTER TABLE fee_ledger
    ADD CONSTRAINT fk_fee_ledger_course_period
        FOREIGN KEY (course_academic_period_id)
        REFERENCES course_academic_periods(id) ON DELETE RESTRICT,
    DROP COLUMN semester_id;

CREATE INDEX idx_payments_course_period ON payments(applied_course_academic_period_id);
CREATE INDEX idx_fee_ledger_course_period ON fee_ledger(course_academic_period_id);
