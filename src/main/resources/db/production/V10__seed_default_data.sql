-- V10__seed_default_data.sql
-- Development/demo seed for a freshly initialized EduCue database.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ---------------------------------------------------------------------------
-- Institution Profile
-- ---------------------------------------------------------------------------
INSERT INTO institution_profiles (
    id, name, short_name, registration_number, motto, official_email,
    phone, website, address, version, created_at, updated_at
)
VALUES (
    1, 'EduCue Medical College', 'EMC', 'MED-REG-1002', 'Excellence in Healthcare Education',
    'info@educuemed.edu', '+1234567890', 'www.educuemed.edu', '123 Health Way',
    0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Departments
-- ---------------------------------------------------------------------------
INSERT INTO departments (name, description, active)
VALUES
    ('Nursing and Midwifery', 'Nursing and midwifery training programmes', TRUE),
    ('Clinical Medicine', 'Clinical medicine and surgery programmes', TRUE),
    ('Pharmacy and Laboratory', 'Pharmacy, biomedical and laboratory sciences', TRUE),
    ('Foundational Health Sciences', 'Institution-wide foundational and communication units', TRUE)
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
    ('System Administrator', 'admin@educuemed.test',     '0700000001', 'ADMIN',     NULL),
    ('Admissions Registrar', 'registrar@educuemed.test', '0700000002', 'REGISTRAR', 'Foundational Health Sciences'),
    ('Finance Officer',       'finance@educuemed.test',   '0700000003', 'FINANCE',   'Foundational Health Sciences'),
    ('Clinical Trainer',      'trainer@educuemed.test',   '0700000004', 'TRAINER',   'Nursing and Midwifery'),
    ('Head of Department',    'hod@educuemed.test',       '0700000005', 'HOD',       'Nursing and Midwifery')
) AS seed(full_name, email, phone, role_name, department_name)
JOIN roles role ON role.name = seed.role_name
LEFT JOIN departments department ON department.name = seed.department_name
ON CONFLICT (email) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Current academic year
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
    ('Nursing and Midwifery', 'CCHN', 'Certificate in Community Health Nursing', 1, 'CERTIFICATE', 'FULL_TIME', 36,  'Certificate in Community Health Nursing', 'YEARS'),
    ('Clinical Medicine', 'DCM', 'Diploma in Clinical Medicine and Surgery', 2, 'DIPLOMA', 'FULL_TIME', 72, 'Diploma in Clinical Medicine', 'YEARS'),
    ('Nursing and Midwifery', 'BSN', 'Bachelor of Science in Nursing', 4, 'BACHELOR', 'FULL_TIME', 144, 'Bachelor of Science in Nursing', 'YEARS')
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
    ('Nursing and Midwifery', 'NUR101', 'Anatomy and Physiology I', 4, 'Introduction to human anatomy and physiology'),
    ('Nursing and Midwifery', 'NUR102', 'Foundations of Nursing Practice', 4, 'Basic nursing skills, ethics, and patient care'),
    ('Nursing and Midwifery', 'NUR201', 'Medical-Surgical Nursing I', 4, 'Nursing care for adult patients with medical-surgical conditions'),
    ('Nursing and Midwifery', 'NUR202', 'Maternal and Newborn Health', 4, 'Care of women during pregnancy, childbirth, and postpartum'),
    ('Nursing and Midwifery', 'NUR301', 'Pediatric Nursing', 4, 'Nursing care of infants, children, and adolescents'),
    ('Nursing and Midwifery', 'NUR302', 'Community Health Nursing', 4, 'Public health principles and community-based care'),
    ('Clinical Medicine', 'MED101', 'Human Anatomy', 4, 'Detailed study of the structure of the human body'),
    ('Clinical Medicine', 'MED102', 'Medical Physiology', 4, 'Functions and mechanisms of the human body systems'),
    ('Clinical Medicine', 'MED201', 'Clinical Pharmacology', 4, 'Drugs, their mechanisms, therapeutic uses, and side effects'),
    ('Clinical Medicine', 'MED202', 'Pathology and Microbiology', 4, 'Study of disease processes and pathogenic microorganisms'),
    ('Clinical Medicine', 'MED301', 'Internal Medicine', 5, 'Diagnosis and non-surgical treatment of adult diseases'),
    ('Clinical Medicine', 'MED302', 'General Surgery', 5, 'Principles of surgery and pre/post-operative care'),
    ('Pharmacy and Laboratory', 'PHA101', 'Introduction to Pharmacy', 3, 'Basics of pharmacy practice and drug dispensing'),
    ('Foundational Health Sciences', 'FND101', 'Medical Terminology', 3, 'Language of medicine and healthcare'),
    ('Foundational Health Sciences', 'FND102', 'Communication in Healthcare', 3, 'Effective communication with patients and colleagues'),
    ('Foundational Health Sciences', 'FND201', 'First Aid and Emergency Care', 3, 'Basic life support and emergency response techniques'),
    ('Foundational Health Sciences', 'FND202', 'Healthcare Ethics and Law', 3, 'Legal and ethical issues in medical practice')
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
    ('CCHN', 'Y1T1', 1), ('CCHN', 'Y1T2', 2), ('CCHN', 'Y1T3', 3),
    ('DCM', 'Y1S1', 1), ('DCM', 'Y1S2', 2), ('DCM', 'Y2S1', 3), ('DCM', 'Y2S2', 4),
    ('BSN', 'Y1S1', 1), ('BSN', 'Y1S2', 2),
    ('BSN', 'Y2S1', 3), ('BSN', 'Y2S2', 4),
    ('BSN', 'Y3S1', 5), ('BSN', 'Y3S2', 6),
    ('BSN', 'Y4S1', 7), ('BSN', 'Y4S2', 8)
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
      SELECT id FROM courses WHERE code IN ('CCHN', 'DCM', 'BSN')
  );

-- ---------------------------------------------------------------------------
-- Place sample units into course academic periods.
-- ---------------------------------------------------------------------------
INSERT INTO course_unit_placements (
    uuid, course_academic_period_id, unit_id, unit_type,
    effective_from_intake_year, active, version, created_at, updated_at
)
SELECT gen_random_uuid(), course_period.id, unit.id, 'CORE',
       2026, TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (VALUES
    ('CCHN', 'Y1T1', 'FND101'), ('CCHN', 'Y1T1', 'NUR101'),
    ('CCHN', 'Y1T2', 'FND102'), ('CCHN', 'Y1T2', 'NUR102'),
    ('CCHN', 'Y1T3', 'FND201'),
    ('DCM', 'Y1S1', 'MED101'), ('DCM', 'Y1S1', 'FND101'),
    ('DCM', 'Y1S2', 'MED102'), ('DCM', 'Y2S1', 'MED201'),
    ('DCM', 'Y2S2', 'MED202'), ('DCM', 'Y2S2', 'FND201'),
    ('BSN', 'Y1S1', 'NUR101'), ('BSN', 'Y1S1', 'FND101'),
    ('BSN', 'Y1S2', 'NUR102'), ('BSN', 'Y2S1', 'NUR201'),
    ('BSN', 'Y2S2', 'NUR202'), ('BSN', 'Y3S1', 'NUR301'),
    ('BSN', 'Y3S2', 'NUR302'), ('BSN', 'Y4S1', 'MED201'),
    ('BSN', 'Y4S2', 'FND202'), ('BSN', 'Y4S2', 'PHA101')
) AS placement(course_code, period_code, unit_code)
JOIN courses course ON course.code = placement.course_code
JOIN academic_periods period ON period.code = placement.period_code
JOIN course_academic_periods course_period
  ON course_period.course_id = course.id
 AND course_period.academic_period_id = period.id
JOIN units unit ON unit.code = placement.unit_code
ON CONFLICT (course_academic_period_id, unit_id, effective_from_intake_year)
DO NOTHING;
