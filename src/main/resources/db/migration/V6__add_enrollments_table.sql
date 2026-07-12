
-- V6__add_enrollments_table.sql
CREATE TABLE enrollments (
                             id BIGSERIAL PRIMARY KEY,

                             student_id BIGINT NOT NULL,
                             course_curriculum_id BIGINT NOT NULL,
                             current_semester_id BIGINT NOT NULL,

                             status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                             admission_date DATE NOT NULL DEFAULT CURRENT_DATE,

                             created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                             CONSTRAINT fk_enrollment_student
                                 FOREIGN KEY (student_id) REFERENCES students(id),

                             CONSTRAINT fk_enrollment_curriculum
                                 FOREIGN KEY (course_curriculum_id) REFERENCES course_curriculums(id),

                             CONSTRAINT fk_enrollment_semester
                                 FOREIGN KEY (current_semester_id) REFERENCES semesters(id)
);
