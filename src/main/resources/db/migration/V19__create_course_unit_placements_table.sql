CREATE TABLE course_unit_placements (
                                        id BIGSERIAL PRIMARY KEY,

                                        uuid UUID NOT NULL DEFAULT gen_random_uuid(),

                                        course_id BIGINT NOT NULL,

                                        unit_id BIGINT NOT NULL,

                                        academic_period_id BIGINT NOT NULL,

                                        unit_type VARCHAR(20) NOT NULL,

                                        effective_from_intake_year INTEGER NOT NULL,

                                        effective_to_intake_year INTEGER,

                                        active BOOLEAN NOT NULL DEFAULT TRUE,

                                        version BIGINT NOT NULL DEFAULT 0,

                                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                        CONSTRAINT uk_course_unit_placements_uuid
                                            UNIQUE (uuid),

    /*
     * The same course-unit combination can have several historical
     * placements, but cannot start twice in the same intake year.
     */
                                        CONSTRAINT uk_course_unit_placement_start
                                            UNIQUE (
                                                    course_id,
                                                    unit_id,
                                                    effective_from_intake_year
                                                ),

                                        CONSTRAINT fk_course_unit_placement_course
                                            FOREIGN KEY (course_id)
                                                REFERENCES courses (id)
                                                ON DELETE RESTRICT,

                                        CONSTRAINT fk_course_unit_placement_unit
                                            FOREIGN KEY (unit_id)
                                                REFERENCES units (id)
                                                ON DELETE RESTRICT,

                                        CONSTRAINT chk_course_unit_placement_type
                                            CHECK (
                                                unit_type IN (
                                                              'CORE',
                                                              'ELECTIVE',
                                                              'OPTIONAL'
                                                    )
                                                ),

                                        CONSTRAINT chk_course_unit_placement_from_year
                                            CHECK (effective_from_intake_year > 0),

                                        CONSTRAINT chk_course_unit_placement_year_range
                                            CHECK (
                                                effective_to_intake_year IS NULL
                                                    OR effective_to_intake_year
                                                    >= effective_from_intake_year
                                                )
);

CREATE INDEX idx_course_unit_placements_course
    ON course_unit_placements (course_id);

CREATE INDEX idx_course_unit_placements_unit
    ON course_unit_placements (unit_id);

CREATE INDEX idx_course_unit_placements_period
    ON course_unit_placements (academic_period_id);

CREATE INDEX idx_course_unit_placements_course_period
    ON course_unit_placements (
                               course_id,
                               academic_period_id
        );

CREATE INDEX idx_course_unit_placements_effective_range
    ON course_unit_placements (
                               course_id,
                               effective_from_intake_year,
                               effective_to_intake_year
        );

CREATE INDEX idx_course_unit_placements_active
    ON course_unit_placements (active);
