-- Development/demo seed for a freshly initialized EduCue database.
--
-- Common password for every seeded staff account: EduCue@123
-- PostgreSQL pgcrypto generates a BCrypt hash understood by Spring Security.
-- These users must change the shared password after their first login.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ---------------------------------------------------------------------------
-- Departments
-- ---------------------------------------------------------------------------
INSERT INTO departments (name, description, active)
VALUES
    ('Computing and Informatics', 'Computing, software and information technology programmes', TRUE),
    ('Business and Management', 'Business, accounting and management programmes', TRUE),
    ('Engineering and Technology', 'Applied engineering and technology programmes', TRUE),
    ('General Studies', 'Institution-wide communication and foundational units', TRUE)
ON CONFLICT (name) DO UPDATE
SET description = EXCLUDED.description,
    active = EXCLUDED.active,
    updated_at = CURRENT_TIMESTAMP;

-- ---------------------------------------------------------------------------
-- Staff users (intentionally excludes STUDENT)
-- ---------------------------------------------------------------------------
INSERT INTO users (
    full_name, email, password_hash, role_id, active, phone,
    department_id, must_change_password, version, created_at, updated_at
)
SELECT seed.full_name,
       seed.email,
       crypt('EduCue@123', gen_salt('bf', 10)),
       role.id,
       TRUE,
       seed.phone,
       department.id,
       TRUE,
       0,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
FROM (VALUES
    ('System Administrator', 'admin@educue.test',     '0700000001', 'ADMIN',     NULL),
    ('Admissions Registrar', 'registrar@educue.test', '0700000002', 'REGISTRAR', 'General Studies'),
    ('Finance Officer',       'finance@educue.test',   '0700000003', 'FINANCE',   'Business and Management'),
    ('Computing Trainer',     'trainer@educue.test',   '0700000004', 'TRAINER',   'Computing and Informatics'),
    ('Head of Department',    'hod@educue.test',       '0700000005', 'HOD',       'Computing and Informatics')
) AS seed(full_name, email, phone, role_name, department_name)
JOIN roles role ON role.name = seed.role_name
LEFT JOIN departments department ON department.name = seed.department_name
ON CONFLICT (email) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Current academic year
-- A clean installation should have a year available immediately after login.
-- If this seed is applied to a database that already has a current year, it
-- deliberately leaves that institutional choice unchanged.
-- ---------------------------------------------------------------------------
INSERT INTO academic_years (
    code, start_date, start_year, end_date, current, closed, active, version
)
SELECT '2026/2027', DATE '2026-01-01', 2026, DATE '2026-12-31',
       TRUE, FALSE, TRUE, 0
WHERE NOT EXISTS (SELECT 1 FROM academic_years WHERE current = TRUE)
ON CONFLICT (code) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Global academic-period catalogue
-- Certificate programmes use terms; diploma/degree programmes use semesters.
-- ---------------------------------------------------------------------------
INSERT INTO academic_periods (
    code, name, period_type, year_number, period_number,
    sequence_number, active, version
)
VALUES
    ('Y1T1', 'Year 1 Term 1', 'TERM', 1, 1, 1, TRUE, 0),
    ('Y1T2', 'Year 1 Term 2', 'TERM', 1, 2, 2, TRUE, 0),
    ('Y1T3', 'Year 1 Term 3', 'TERM', 1, 3, 3, TRUE, 0),
    ('Y1S1', 'Year 1 Semester 1', 'SEMESTER', 1, 1, 1, TRUE, 0),
    ('Y1S2', 'Year 1 Semester 2', 'SEMESTER', 1, 2, 2, TRUE, 0),
    ('Y2S1', 'Year 2 Semester 1', 'SEMESTER', 2, 1, 3, TRUE, 0),
    ('Y2S2', 'Year 2 Semester 2', 'SEMESTER', 2, 2, 4, TRUE, 0),
    ('Y3S1', 'Year 3 Semester 1', 'SEMESTER', 3, 1, 5, TRUE, 0),
    ('Y3S2', 'Year 3 Semester 2', 'SEMESTER', 3, 2, 6, TRUE, 0),
    ('Y4S1', 'Year 4 Semester 1', 'SEMESTER', 4, 1, 7, TRUE, 0),
    ('Y4S2', 'Year 4 Semester 2', 'SEMESTER', 4, 2, 8, TRUE, 0)
ON CONFLICT (code) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Courses
-- ---------------------------------------------------------------------------
INSERT INTO courses (
    uuid, department_id, code, name, duration_value, active,
    qualification_type, study_mode, total_credits, version,
    award_title, duration_unit, created_at, updated_at
)
SELECT gen_random_uuid(), department.id, seed.code, seed.name,
       seed.duration_value, TRUE, seed.qualification_type,
       seed.study_mode, seed.total_credits, 0, seed.award_title,
       seed.duration_unit, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (VALUES
    ('Computing and Informatics', 'CICT', 'Certificate in Information and Communication Technology', 1, 'CERTIFICATE', 'FULL_TIME', 36,  'Certificate in Information and Communication Technology', 'YEARS'),
    ('Business and Management', 'DBM', 'Diploma in Business Management', 2, 'DIPLOMA', 'FULL_TIME', 72, 'Diploma in Business Management', 'YEARS'),
    ('Computing and Informatics', 'BSC-CS', 'Bachelor of Science in Computer Science', 4, 'BACHELOR', 'FULL_TIME', 144, 'Bachelor of Science in Computer Science', 'YEARS')
) AS seed(department_name, code, name, duration_value, qualification_type, study_mode, total_credits, award_title, duration_unit)
JOIN departments department ON department.name = seed.department_name
ON CONFLICT (code) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Reusable unit catalogue
-- ---------------------------------------------------------------------------
INSERT INTO units (
    uuid, department_id, code, name, credit_hours, description,
    active, version, created_at, updated_at
)
SELECT gen_random_uuid(), department.id, seed.code, seed.name,
       seed.credit_hours, seed.description, TRUE, 0,
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (VALUES
    ('Computing and Informatics', 'ICT101', 'Computer Applications', 3, 'Introduction to computers and productivity applications'),
    ('Computing and Informatics', 'ICT102', 'Computer Hardware and Support', 3, 'Computer components, maintenance and user support'),
    ('Computing and Informatics', 'ICT103', 'Introduction to Networking', 3, 'Networking concepts, devices and basic configuration'),
    ('Computing and Informatics', 'CSC101', 'Introduction to Programming', 4, 'Programming fundamentals and problem solving'),
    ('Computing and Informatics', 'CSC102', 'Discrete Mathematics', 3, 'Logic, sets, relations and discrete structures'),
    ('Computing and Informatics', 'CSC201', 'Data Structures and Algorithms', 4, 'Core data structures and algorithm design'),
    ('Computing and Informatics', 'CSC202', 'Database Systems', 4, 'Relational modelling, SQL and database design'),
    ('Computing and Informatics', 'CSC301', 'Software Engineering', 4, 'Software lifecycle, architecture, testing and teamwork'),
    ('Computing and Informatics', 'CSC302', 'Computer Networks', 4, 'Network design, protocols and administration'),
    ('Computing and Informatics', 'CSC401', 'Artificial Intelligence', 4, 'Foundations and applications of artificial intelligence'),
    ('Computing and Informatics', 'CSC402', 'Computer Science Project', 6, 'Supervised final-year computing project'),
    ('Business and Management', 'BUS101', 'Principles of Management', 3, 'Foundations of management and organisational practice'),
    ('Business and Management', 'BUS102', 'Financial Accounting', 3, 'Accounting concepts and preparation of financial statements'),
    ('Business and Management', 'BUS201', 'Marketing Management', 3, 'Marketing planning, customers and markets'),
    ('Business and Management', 'BUS202', 'Human Resource Management', 3, 'People management and workplace practice'),
    ('General Studies', 'COM101', 'Communication Skills', 3, 'Academic and professional communication'),
    ('General Studies', 'ENT201', 'Entrepreneurship', 3, 'Entrepreneurial thinking and new venture fundamentals')
) AS seed(department_name, code, name, credit_hours, description)
JOIN departments department ON department.name = seed.department_name
ON CONFLICT (code) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Course-specific academic periods
-- ---------------------------------------------------------------------------
INSERT INTO course_academic_periods (
    uuid, course_id, academic_period_id, position, version, created_at
)
SELECT gen_random_uuid(), course.id, period.id, mapping.position, 0, CURRENT_TIMESTAMP
FROM (VALUES
    ('CICT', 'Y1T1', 1), ('CICT', 'Y1T2', 2), ('CICT', 'Y1T3', 3),
    ('DBM', 'Y1S1', 1), ('DBM', 'Y1S2', 2), ('DBM', 'Y2S1', 3), ('DBM', 'Y2S2', 4),
    ('BSC-CS', 'Y1S1', 1), ('BSC-CS', 'Y1S2', 2),
    ('BSC-CS', 'Y2S1', 3), ('BSC-CS', 'Y2S2', 4),
    ('BSC-CS', 'Y3S1', 5), ('BSC-CS', 'Y3S2', 6),
    ('BSC-CS', 'Y4S1', 7), ('BSC-CS', 'Y4S2', 8)
) AS mapping(course_code, period_code, position)
JOIN courses course ON course.code = mapping.course_code
JOIN academic_periods period ON period.code = mapping.period_code
ON CONFLICT (course_id, academic_period_id) DO NOTHING;

-- Build the progression chain independently for every course.
UPDATE course_academic_periods current_period
SET next_period_id = next_period.id
FROM course_academic_periods next_period
WHERE next_period.course_id = current_period.course_id
  AND next_period.position = current_period.position + 1
  AND current_period.course_id IN (
      SELECT id FROM courses WHERE code IN ('CICT', 'DBM', 'BSC-CS')
  );

-- ---------------------------------------------------------------------------
-- Place sample units into course academic periods.
-- A unit remains globally reusable; this table defines where each course uses it.
-- ---------------------------------------------------------------------------
INSERT INTO course_unit_placements (
    uuid, course_academic_period_id, unit_id, unit_type,
    effective_from_intake_year, active, version, created_at, updated_at
)
SELECT gen_random_uuid(), course_period.id, unit.id, 'CORE',
       2026, TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (VALUES
    ('CICT', 'Y1T1', 'ICT101'), ('CICT', 'Y1T1', 'COM101'),
    ('CICT', 'Y1T2', 'ICT102'), ('CICT', 'Y1T2', 'ICT103'),
    ('CICT', 'Y1T3', 'ENT201'),
    ('DBM', 'Y1S1', 'BUS101'), ('DBM', 'Y1S1', 'COM101'),
    ('DBM', 'Y1S2', 'BUS102'), ('DBM', 'Y2S1', 'BUS201'),
    ('DBM', 'Y2S2', 'BUS202'), ('DBM', 'Y2S2', 'ENT201'),
    ('BSC-CS', 'Y1S1', 'CSC101'), ('BSC-CS', 'Y1S1', 'COM101'),
    ('BSC-CS', 'Y1S2', 'CSC102'), ('BSC-CS', 'Y2S1', 'CSC201'),
    ('BSC-CS', 'Y2S2', 'CSC202'), ('BSC-CS', 'Y3S1', 'CSC301'),
    ('BSC-CS', 'Y3S2', 'CSC302'), ('BSC-CS', 'Y4S1', 'CSC401'),
    ('BSC-CS', 'Y4S2', 'CSC402'), ('BSC-CS', 'Y4S2', 'ENT201')
) AS placement(course_code, period_code, unit_code)
JOIN courses course ON course.code = placement.course_code
JOIN academic_periods period ON period.code = placement.period_code
JOIN course_academic_periods course_period
  ON course_period.course_id = course.id
 AND course_period.academic_period_id = period.id
JOIN units unit ON unit.code = placement.unit_code
ON CONFLICT (course_academic_period_id, unit_id, effective_from_intake_year)
DO NOTHING;
