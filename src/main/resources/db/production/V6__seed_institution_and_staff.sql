-- V6: Initial institution, departments, staff accounts and academic years.
-- All seeded users must change the shared bootstrap password at first login.
SET search_path TO public;

INSERT INTO institution_profiles (
    id, uuid, name, short_name, motto, registration_number,
    official_email, phone, address, website, version, created_at, updated_at
) VALUES (
    1,
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'Apex Institute of Technology',
    'AIT',
    'Excellence Through Innovation',
    'REG-2026-90412',
    'info@apexinstitute.edu',
    '+1 (555) 019-2834',
    '123 University Ave, Suite 400, Tech City, CA 94016',
    'https://www.apexinstitute.edu',
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO departments (name, description, active, created_at, updated_at) VALUES
    ('Registrar', 'Admissions, enrollment and academic records', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Finance', 'Fees, payments and institutional finance', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Human Health', 'Human health academic programmes and training', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- The health catalogue created in V5 belongs to the academic department.
UPDATE units
SET department_id = (SELECT id FROM departments WHERE name = 'Human Health'),
    updated_at = CURRENT_TIMESTAMP
WHERE department_id = (SELECT id FROM departments WHERE name = 'Administration');

INSERT INTO academic_years (
    uuid, code, start_date, start_year, end_date,
    current, closed, active, version, created_at, updated_at
) VALUES
    (gen_random_uuid(), '2020/2021', DATE '2020-09-01', 2020, DATE '2021-08-31', FALSE, TRUE,  TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), '2021/2022', DATE '2021-09-01', 2021, DATE '2022-08-31', FALSE, TRUE,  TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), '2022/2023', DATE '2022-09-01', 2022, DATE '2023-08-31', FALSE, TRUE,  TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), '2023/2024', DATE '2023-09-01', 2023, DATE '2024-08-31', FALSE, TRUE,  TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), '2024/2025', DATE '2024-09-01', 2024, DATE '2025-08-31', FALSE, TRUE,  TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), '2025/2026', DATE '2025-09-01', 2025, DATE '2026-08-31', TRUE,  FALSE, TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), '2026/2027', DATE '2026-09-01', 2026, DATE '2027-08-31', FALSE, FALSE, TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

WITH staff(full_name, email, username, role_name, department_name) AS (
    VALUES
        ('System Administrator', 'admin@educue.local',     'admin',     'ADMIN',     'Administration'),
        ('College Registrar',    'registrar@educue.local', 'registrar', 'REGISTRAR', 'Registrar'),
        ('Finance Officer',      'finance@educue.local',   'finance',   'FINANCE',   'Finance'),
        ('Human Health HOD',     'hod@educue.local',       'health.hod','HOD',       'Human Health'),
        ('Human Health Trainer', 'trainer@educue.local',   'trainer',   'TRAINER',   'Human Health')
)
INSERT INTO users (
    full_name, email, username, password_hash, role_id, department_id,
    active, must_change_password, version, created_at, updated_at
)
SELECT
    staff.full_name,
    staff.email,
    staff.username,
    crypt('ChangeMe@123', gen_salt('bf', 10)),
    roles.id,
    departments.id,
    TRUE,
    TRUE,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM staff
JOIN roles ON roles.name = staff.role_name
JOIN departments ON departments.name = staff.department_name
ON CONFLICT (email) DO UPDATE SET
    full_name = EXCLUDED.full_name,
    username = EXCLUDED.username,
    password_hash = EXCLUDED.password_hash,
    role_id = EXCLUDED.role_id,
    department_id = EXCLUDED.department_id,
    active = TRUE,
    must_change_password = TRUE,
    updated_at = CURRENT_TIMESTAMP;
