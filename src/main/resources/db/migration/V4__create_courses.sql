-- V2__create_courses.sql

CREATE TABLE courses (
                         id BIGSERIAL PRIMARY KEY,

                         uuid UUID NOT NULL UNIQUE,

                         department_id BIGINT NOT NULL REFERENCES departments(id),

                         code VARCHAR(20) NOT NULL UNIQUE,

                         name VARCHAR(150) NOT NULL,

                         duration_value INT,
                         duration_unit VARCHAR(10), -- MONTHS | YEARS | WEEKS

                         total_semesters INT NOT NULL,

                         active BOOLEAN NOT NULL DEFAULT TRUE,

                         created_at TIMESTAMP NOT NULL DEFAULT now(),
                         updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_courses_department_id ON courses(department_id);
CREATE INDEX idx_courses_code ON courses(code);
