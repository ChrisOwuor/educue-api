-- V9__application_audit_and_student_origin.sql

-- =====================================================
-- APPLICATION AUDIT + OPTIMISTIC LOCKING
-- =====================================================

ALTER TABLE applications
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE applications
    ADD COLUMN approved_at TIMESTAMP;

ALTER TABLE applications
    ADD COLUMN approved_by_user_id BIGINT;

ALTER TABLE applications
    ADD CONSTRAINT fk_applications_approved_by
        FOREIGN KEY (approved_by_user_id)
            REFERENCES users(id);


-- =====================================================
-- STUDENT -> SOURCE APPLICATION
-- One application can create only one student.
-- =====================================================

ALTER TABLE students
    ADD COLUMN application_id BIGINT NOT NULL;

ALTER TABLE students
    ADD CONSTRAINT uk_students_application
        UNIQUE (application_id);

ALTER TABLE students
    ADD CONSTRAINT fk_students_application
        FOREIGN KEY (application_id)
            REFERENCES applications(id);

-- =====================================================
-- ENROLLMENT -> COURSE
-- Denormalized reference for simpler reporting/querying.
-- =====================================================

ALTER TABLE enrollments
    ADD COLUMN course_id BIGINT NOT NULL;

ALTER TABLE enrollments
    ADD CONSTRAINT fk_enrollments_course
        FOREIGN KEY (course_id)
            REFERENCES courses(id);

CREATE INDEX idx_enrollments_course
    ON enrollments(course_id);

-- =====================================================
-- USER SECURITY
-- Force first password change for newly admitted students.
-- =====================================================

ALTER TABLE users
    ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT TRUE;

-- Existing users should not be forced to change passwords.
UPDATE users
SET must_change_password = FALSE;

-- =====================================================
-- CURRICULUM INTEGRITY
-- Only one default curriculum per course.
-- =====================================================

CREATE UNIQUE INDEX uq_course_curriculum_default_admission
    ON course_curriculums(course_id)
    WHERE is_default_for_admission = TRUE;



ALTER TABLE students
    ADD COLUMN user_id BIGINT NOT NULL;

ALTER TABLE students
    ADD CONSTRAINT uk_students_user
        UNIQUE (user_id);

ALTER TABLE students
    ADD CONSTRAINT fk_students_user
        FOREIGN KEY (user_id)
            REFERENCES users(id);
