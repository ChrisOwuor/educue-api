CREATE TABLE student_results
(
    id BIGSERIAL PRIMARY KEY,

    uuid UUID NOT NULL UNIQUE,

    version BIGINT,

    student_unit_registration_id BIGINT NOT NULL UNIQUE,

    ca_marks NUMERIC(5,2),

    exam_marks NUMERIC(5,2),

    total_marks NUMERIC(5,2),

    grade VARCHAR(10),

    passed BOOLEAN NOT NULL DEFAULT FALSE,

    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',

    remarks TEXT,

    recorded_by_id BIGINT,

    approved_by_id BIGINT,

    approved_at TIMESTAMP,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_student_results_registration
        FOREIGN KEY (student_unit_registration_id)
            REFERENCES student_unit_registrations (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_student_results_recorded_by
        FOREIGN KEY (recorded_by_id)
            REFERENCES users (id)
            ON DELETE SET NULL,

    CONSTRAINT fk_student_results_approved_by
        FOREIGN KEY (approved_by_id)
            REFERENCES users (id)
            ON DELETE SET NULL,

    CONSTRAINT chk_student_results_ca_marks
        CHECK (ca_marks IS NULL OR (ca_marks >= 0 AND ca_marks <= 100)),

    CONSTRAINT chk_student_results_exam_marks
        CHECK (exam_marks IS NULL OR (exam_marks >= 0 AND exam_marks <= 100)),

    CONSTRAINT chk_student_results_total_marks
        CHECK (total_marks IS NULL OR (total_marks >= 0 AND total_marks <= 100)),

    CONSTRAINT chk_student_results_status
        CHECK (
            status IN (
                       'DRAFT',
                       'SUBMITTED',
                       'APPROVED',
                       'RELEASED',
                       'WITHHELD'
                )
            )
);

CREATE INDEX idx_student_results_status
    ON student_results (status);

CREATE INDEX idx_student_results_recorded_by
    ON student_results (recorded_by_id);

CREATE INDEX idx_student_results_approved_by
    ON student_results (approved_by_id);
