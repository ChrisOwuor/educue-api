CREATE TABLE course_academic_periods (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    course_id BIGINT NOT NULL,
    academic_period_id BIGINT NOT NULL,
    position INTEGER NOT NULL,
    next_period_id BIGINT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_course_academic_periods_uuid UNIQUE (uuid),
    CONSTRAINT uk_course_academic_period UNIQUE (course_id, academic_period_id),
    CONSTRAINT uk_course_period_position UNIQUE (course_id, position),
    CONSTRAINT uk_course_period_next UNIQUE (next_period_id),
    CONSTRAINT chk_course_period_position CHECK (position > 0),
    CONSTRAINT fk_course_period_course FOREIGN KEY (course_id)
        REFERENCES courses (id) ON DELETE RESTRICT,
    CONSTRAINT fk_course_period_academic_period FOREIGN KEY (academic_period_id)
        REFERENCES academic_periods (id) ON DELETE RESTRICT,
    CONSTRAINT fk_course_period_next FOREIGN KEY (next_period_id)
        REFERENCES course_academic_periods (id) ON DELETE RESTRICT
);

CREATE INDEX idx_course_period_course ON course_academic_periods (course_id);
CREATE INDEX idx_course_period_next ON course_academic_periods (next_period_id);

-- Deliberately no backfill: existing placement data is not guessed or silently remapped.
-- The legacy columns stay nullable during this transition. The application only writes
-- course_academic_period_id for new rows; a later cleanup migration can remove the old
-- columns after legacy placements have been handled explicitly.
ALTER TABLE course_unit_placements
    ADD COLUMN course_academic_period_id BIGINT;

ALTER TABLE course_unit_placements
    ALTER COLUMN course_id DROP NOT NULL,
    ALTER COLUMN academic_period_id DROP NOT NULL,
    DROP CONSTRAINT uk_course_unit_placement_start,
    ADD CONSTRAINT fk_course_unit_placement_course_period
        FOREIGN KEY (course_academic_period_id)
        REFERENCES course_academic_periods (id) ON DELETE RESTRICT,
    ADD CONSTRAINT uk_course_unit_placement_start
        UNIQUE (course_academic_period_id, unit_id, effective_from_intake_year);

CREATE INDEX idx_course_unit_placements_course_academic_period
    ON course_unit_placements (course_academic_period_id);

CREATE INDEX idx_course_unit_placements_course_period_effective_range
    ON course_unit_placements (
        course_academic_period_id,
        effective_from_intake_year,
        effective_to_intake_year
    );
