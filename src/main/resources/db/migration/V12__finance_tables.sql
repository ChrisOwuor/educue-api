-- ==========================================
-- FEE STRUCTURES
-- ==========================================

CREATE TABLE fee_structures (

                                id BIGSERIAL PRIMARY KEY,

                                intake_id BIGINT NOT NULL
                                    REFERENCES intakes(id),

                                course_id BIGINT NOT NULL
                                    REFERENCES courses(id),

                                semester_id BIGINT NOT NULL
                                    REFERENCES semesters(id),

                                active BOOLEAN NOT NULL DEFAULT TRUE,

                                created_at TIMESTAMP NOT NULL DEFAULT now(),

                                CONSTRAINT uk_fee_structure
                                    UNIQUE (
                                            intake_id,
                                            course_id,
                                            semester_id
                                        )
);

CREATE TABLE fee_structure_items (

                                     id BIGSERIAL PRIMARY KEY,

                                     fee_structure_id BIGINT NOT NULL
                                         REFERENCES fee_structures(id)
                                             ON DELETE CASCADE,

                                     name VARCHAR(150) NOT NULL,

                                     amount NUMERIC(12,2) NOT NULL
);

-- ==========================================
-- PAYMENTS
-- ==========================================

CREATE TABLE payments (

                          id BIGSERIAL PRIMARY KEY,

                          student_id BIGINT NOT NULL
                              REFERENCES students(id),

                          amount NUMERIC(12,2) NOT NULL,

                          reference VARCHAR(100) NOT NULL,

                          payment_method VARCHAR(30) NOT NULL,

                          paid_at TIMESTAMP NOT NULL,

                          recorded_by BIGINT
                              REFERENCES users(id),

                          confirmed BOOLEAN NOT NULL DEFAULT TRUE,

                          remarks VARCHAR(255),

                          CONSTRAINT uk_payment_reference
                              UNIQUE(reference)
);

-- ==========================================
-- STUDENT LEDGER
-- ==========================================

CREATE TABLE student_ledger (

                                id BIGSERIAL PRIMARY KEY,

                                student_id BIGINT NOT NULL
                                    REFERENCES students(id),

                                payment_id BIGINT
                                    REFERENCES payments(id),

                                debit NUMERIC(12,2) NOT NULL DEFAULT 0,

                                credit NUMERIC(12,2) NOT NULL DEFAULT 0,

                                description VARCHAR(255) NOT NULL,

                                created_by BIGINT
                                    REFERENCES users(id),

                                created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_payment_student
    ON payments(student_id);

CREATE INDEX idx_ledger_student
    ON student_ledger(student_id);
