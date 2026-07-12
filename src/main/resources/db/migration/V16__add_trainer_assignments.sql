-- ============================================================
-- V16__create_trainer_assignments.sql
-- ============================================================

CREATE TABLE trainer_assignments
(
    id BIGSERIAL PRIMARY KEY,

    trainer_id BIGINT NOT NULL,
    semester_unit_id BIGINT NOT NULL,

    effective_from DATE NOT NULL DEFAULT CURRENT_DATE,
    effective_to DATE,

    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    assigned_by BIGINT,

    CONSTRAINT fk_trainer_assignments_trainer
        FOREIGN KEY (trainer_id)
            REFERENCES users(id),

    CONSTRAINT fk_trainer_assignments_semester_unit
        FOREIGN KEY (semester_unit_id)
            REFERENCES semester_units(id),

    CONSTRAINT fk_trainer_assignments_assigned_by
        FOREIGN KEY (assigned_by)
            REFERENCES users(id),

    CONSTRAINT chk_trainer_assignment_dates
        CHECK (
            effective_to IS NULL
                OR effective_to >= effective_from
            )
);

-- ============================================================
-- Indexes
-- ============================================================

CREATE INDEX idx_trainer_assignment_trainer
    ON trainer_assignments(trainer_id);

CREATE INDEX idx_trainer_assignment_semester_unit
    ON trainer_assignments(semester_unit_id);

CREATE INDEX idx_trainer_assignment_effective_from
    ON trainer_assignments(effective_from);

CREATE INDEX idx_trainer_assignment_effective_to
    ON trainer_assignments(effective_to);

CREATE INDEX idx_trainer_assignment_assigned_by
    ON trainer_assignments(assigned_by);

-- Helps efficiently find the current active assignment for a semester unit.
CREATE INDEX idx_trainer_assignment_active
    ON trainer_assignments(semester_unit_id, effective_to);
