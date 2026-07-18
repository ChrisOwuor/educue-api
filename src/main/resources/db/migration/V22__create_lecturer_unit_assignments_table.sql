CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE lecturer_unit_assignments (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    course_unit_placement_id BIGINT NOT NULL,
    lecturer_id BIGINT NOT NULL,
    effective_from_academic_year_id BIGINT NOT NULL,
    effective_to_academic_year_id BIGINT,
    start_year INTEGER NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by BIGINT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_lecturer_unit_assignments_uuid UNIQUE (uuid),
    CONSTRAINT fk_lecturer_assignment_placement
        FOREIGN KEY (course_unit_placement_id)
            REFERENCES course_unit_placements (id)
            ON DELETE RESTRICT,
    CONSTRAINT fk_lecturer_assignment_lecturer
        FOREIGN KEY (lecturer_id)
            REFERENCES users (id)
            ON DELETE RESTRICT,
    CONSTRAINT fk_lecturer_assignment_from_year
        FOREIGN KEY (effective_from_academic_year_id)
            REFERENCES academic_years (id)
            ON DELETE RESTRICT,
    CONSTRAINT fk_lecturer_assignment_to_year
        FOREIGN KEY (effective_to_academic_year_id)
            REFERENCES academic_years (id)
            ON DELETE RESTRICT,
    CONSTRAINT fk_lecturer_assignment_assigned_by
        FOREIGN KEY (assigned_by)
            REFERENCES users (id)
            ON DELETE SET NULL,
    CONSTRAINT chk_lecturer_assignment_start_year CHECK (start_year > 0)
);

CREATE INDEX idx_lecturer_assignment_placement
    ON lecturer_unit_assignments (course_unit_placement_id);

CREATE INDEX idx_lecturer_assignment_lecturer
    ON lecturer_unit_assignments (lecturer_id);

CREATE INDEX idx_lecturer_assignment_effective_years
    ON lecturer_unit_assignments (
        effective_from_academic_year_id,
        effective_to_academic_year_id
    );

CREATE INDEX idx_lecturer_assignment_assigned_by
    ON lecturer_unit_assignments (assigned_by);

CREATE INDEX idx_lecturer_assignment_enabled
    ON lecturer_unit_assignments (enabled);
