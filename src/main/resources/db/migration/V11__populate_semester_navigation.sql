BEGIN;

-- ==========================================
-- Populate next semester pointers
-- ==========================================

WITH ordered AS (
    SELECT
        id,
        LEAD(id) OVER (
            PARTITION BY course_curriculum_id
            ORDER BY year_number, semester_number
        ) AS next_id
    FROM semesters
)
UPDATE semesters s
SET next_semester_id = ordered.next_id
    FROM ordered
WHERE s.id = ordered.id;

-- ==========================================
-- Populate first semester pointers
-- ==========================================

UPDATE course_curriculums cc
SET first_semester_id = s.id
    FROM semesters s
WHERE s.course_curriculum_id = cc.id
  AND s.year_number = 1
  AND s.semester_number = 1;

-- ==========================================
-- Verify before committing
-- ==========================================

SELECT
    id,
    course_curriculum_id,
    year_number,
    semester_number,
    next_semester_id
FROM semesters
ORDER BY course_curriculum_id,
         year_number,
         semester_number;

SELECT
    id,
    name,
    first_semester_id
FROM course_curriculums;

-- If everything looks correct:
COMMIT;

-- If something looks wrong instead:
-- ROLLBACK;
