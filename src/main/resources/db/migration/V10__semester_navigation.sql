-- ==========================================
-- COURSE CURRICULUM
-- ==========================================

ALTER TABLE course_curriculums
    ADD COLUMN first_semester_id BIGINT;

ALTER TABLE course_curriculums
    ADD CONSTRAINT fk_curriculum_first_semester
        FOREIGN KEY (first_semester_id)
            REFERENCES semesters(id);


-- ==========================================
-- SEMESTERS
-- ==========================================

ALTER TABLE semesters
    ADD COLUMN next_semester_id BIGINT;

ALTER TABLE semesters
    ADD CONSTRAINT fk_semester_next
        FOREIGN KEY (next_semester_id)
            REFERENCES semesters(id);
