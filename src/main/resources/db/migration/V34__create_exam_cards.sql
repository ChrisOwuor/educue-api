CREATE TABLE exam_cards (
    id BIGSERIAL PRIMARY KEY,
    verification_code UUID NOT NULL UNIQUE,
    student_id BIGINT NOT NULL REFERENCES students(id),
    course_academic_period_id BIGINT NOT NULL REFERENCES course_academic_periods(id),
    issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_exam_card_student_period UNIQUE (student_id, course_academic_period_id)
);

CREATE INDEX idx_exam_card_verification_code ON exam_cards (verification_code);
