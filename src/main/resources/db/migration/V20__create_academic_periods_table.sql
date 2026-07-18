-- Provides gen_random_uuid().
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE academic_periods (
                                  id BIGSERIAL PRIMARY KEY,

                                  uuid UUID NOT NULL DEFAULT gen_random_uuid(),

                                  code VARCHAR(20) NOT NULL,

                                  name VARCHAR(100) NOT NULL,

                                  period_type VARCHAR(20) NOT NULL,

                                  year_number INTEGER NOT NULL,

                                  period_number INTEGER NOT NULL,

                                  sequence_number INTEGER NOT NULL,

                                  active BOOLEAN NOT NULL DEFAULT TRUE,

                                  version BIGINT NOT NULL DEFAULT 0,

                                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                  CONSTRAINT uk_academic_periods_uuid
                                      UNIQUE (uuid),

                                  CONSTRAINT uk_academic_periods_code
                                      UNIQUE (code),

                                  CONSTRAINT uk_academic_period_type_year_period
                                      UNIQUE (
                                              period_type,
                                              year_number,
                                              period_number
                                          ),

                                  CONSTRAINT chk_academic_period_year_number
                                      CHECK (year_number > 0),

                                  CONSTRAINT chk_academic_period_period_number
                                      CHECK (period_number > 0),

                                  CONSTRAINT chk_academic_period_sequence_number
                                      CHECK (sequence_number > 0),

                                  CONSTRAINT chk_academic_period_type
                                      CHECK (
                                          period_type IN (
                                                          'SEMESTER',
                                                          'TERM',
                                                          'TRIMESTER',
                                                          'MODULE',
                                                          'QUARTER',
                                                          'BLOCK'
                                              )
                                          )
);

CREATE INDEX idx_academic_periods_type
    ON academic_periods (period_type);

CREATE INDEX idx_academic_periods_type_sequence
    ON academic_periods (
                         period_type,
                         sequence_number
        );

CREATE INDEX idx_academic_periods_active
    ON academic_periods (active);

-- V19 creates course_unit_placements first, so add this dependency only after
-- academic_periods exists.
ALTER TABLE course_unit_placements
    ADD CONSTRAINT fk_course_unit_placement_period
        FOREIGN KEY (academic_period_id)
            REFERENCES academic_periods (id)
            ON DELETE RESTRICT;
