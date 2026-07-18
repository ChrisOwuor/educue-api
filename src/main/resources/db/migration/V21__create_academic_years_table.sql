CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE academic_years (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    code VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    start_year INTEGER NOT NULL,
    end_date DATE NOT NULL,
    current BOOLEAN NOT NULL DEFAULT FALSE,
    closed BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_academic_years_uuid UNIQUE (uuid),
    CONSTRAINT uk_academic_years_code UNIQUE (code),
    CONSTRAINT chk_academic_year_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_academic_year_start_year CHECK (start_year > 0),
    CONSTRAINT chk_academic_year_code_not_blank CHECK (btrim(code) <> '')
);

CREATE INDEX idx_academic_years_start_year
    ON academic_years (start_year);

CREATE INDEX idx_academic_years_current
    ON academic_years (current);

CREATE INDEX idx_academic_years_active
    ON academic_years (active);

-- There can be only one current academic year.
CREATE UNIQUE INDEX uk_academic_years_one_current
    ON academic_years (current)
    WHERE current = TRUE;
