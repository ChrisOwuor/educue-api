-- V5__academic_core_structure.sql

-- =========================
-- COURSE CURRICULUMS
-- =========================
CREATE TABLE course_curriculums (
                                    id BIGSERIAL PRIMARY KEY,
                                    course_id BIGINT NOT NULL REFERENCES courses(id),

                                    name VARCHAR(150) NOT NULL,

                                    is_active BOOLEAN NOT NULL DEFAULT TRUE,
                                    is_default_for_admission BOOLEAN NOT NULL DEFAULT FALSE,

                                    effective_from DATE NOT NULL,
                                    effective_to DATE,

                                    created_at TIMESTAMP NOT NULL DEFAULT now(),
                                    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_course_curriculums_course_id ON course_curriculums(course_id);
CREATE INDEX idx_course_curriculums_active ON course_curriculums(is_active);


-- =========================
-- SEMESTERS (CURRICULUM-BOUND)
-- =========================
CREATE TABLE semesters (
                           id BIGSERIAL PRIMARY KEY,
                           course_curriculum_id BIGINT NOT NULL REFERENCES course_curriculums(id),

                           year_number INT NOT NULL,
                           semester_number INT NOT NULL,
                           name VARCHAR(50),

                           CONSTRAINT uq_semester_per_curriculum UNIQUE (course_curriculum_id, year_number, semester_number)
);

CREATE INDEX idx_semesters_curriculum ON semesters(course_curriculum_id);


-- =========================
-- UNITS (GLOBAL CATALOG)
-- =========================
CREATE TABLE units (
                       id BIGSERIAL PRIMARY KEY,
                       department_id BIGINT NOT NULL REFERENCES departments(id),

                       code VARCHAR(30) NOT NULL UNIQUE,
                       name VARCHAR(150) NOT NULL,

                       credit_hours INT,
                       description VARCHAR(255),

                       active BOOLEAN NOT NULL DEFAULT TRUE,

                       created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_units_department_id ON units(department_id);
CREATE INDEX idx_units_code ON units(code);


-- =========================
-- SEMESTER UNITS (CURRICULUM STRUCTURE)
-- =========================
CREATE TABLE semester_units (
                                id BIGSERIAL PRIMARY KEY,
                                semester_id BIGINT NOT NULL REFERENCES semesters(id),
                                unit_id BIGINT NOT NULL REFERENCES units(id),

                                category VARCHAR(20) NOT NULL,
                                is_mandatory BOOLEAN NOT NULL DEFAULT TRUE,

                                CONSTRAINT uq_semester_unit UNIQUE (semester_id, unit_id)
);

CREATE INDEX idx_semester_units_semester_id ON semester_units(semester_id);
CREATE INDEX idx_semester_units_unit_id ON semester_units(unit_id);


-- =========================
-- STUDENTS
-- =========================
CREATE TABLE students (
                          id BIGSERIAL PRIMARY KEY,

                          admission_number VARCHAR(30) NOT NULL UNIQUE,
                          full_name VARCHAR(150) NOT NULL,

                          email VARCHAR(150),
                          phone VARCHAR(20),

                          created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_students_admission_number ON students(admission_number);


-- =========================
-- STUDENT UNIT REGISTRATIONS
-- =========================
CREATE TABLE student_unit_registrations (
                                            id BIGSERIAL PRIMARY KEY,

                                            enrollment_id BIGINT NOT NULL,
                                            semester_unit_id BIGINT NOT NULL REFERENCES semester_units(id),

                                            attempt_type VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
                                            status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

                                            registered_at TIMESTAMP NOT NULL DEFAULT now(),

    -- prevents duplicate ACTIVE registrations for same unit in same enrollment
                                            CONSTRAINT uq_student_unit_registration UNIQUE (enrollment_id, semester_unit_id)
);

CREATE INDEX idx_sureg_enrollment ON student_unit_registrations(enrollment_id);
CREATE INDEX idx_sureg_semester_unit ON student_unit_registrations(semester_unit_id);
CREATE INDEX idx_sureg_status ON student_unit_registrations(status);
