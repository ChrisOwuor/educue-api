-- V1__create_users_roles_permissions.sql

CREATE TABLE roles (
                       id          BIGSERIAL PRIMARY KEY,
                       name        VARCHAR(50) NOT NULL UNIQUE,
                       description VARCHAR(255)
);

CREATE TABLE permissions (
                             id          BIGSERIAL PRIMARY KEY,
                             name        VARCHAR(100) NOT NULL UNIQUE, -- e.g. 'create_invoice', 'edit_marks'
                             description VARCHAR(255)
);

CREATE TABLE role_permissions (
                                  role_id       BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
                                  permission_id BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
                                  PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE users (
                       id            BIGSERIAL PRIMARY KEY,
                       full_name     VARCHAR(150) NOT NULL,
                       email         VARCHAR(150) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       role_id       BIGINT NOT NULL REFERENCES roles(id),
                       active        BOOLEAN NOT NULL DEFAULT TRUE,
                       version       BIGINT NOT NULL DEFAULT 0,   -- optimistic locking (@Version)
                       created_at    TIMESTAMP NOT NULL DEFAULT now(),
                       updated_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role_id ON users(role_id);

-- ===== Seed roles =====
INSERT INTO roles (name, description) VALUES
                                          ('ADMIN', 'Full system access'),
                                          ('REGISTRAR', 'Manages admissions, students, enrollment'),
                                          ('FINANCE', 'Manages invoices and payments'),
                                          ('TRAINER', 'Enters marks and attendance for assigned units'),
                                          ('HOD', 'Approves results, assigns trainers'),
                                          ('STUDENT', 'Views own profile, finance, and results');

-- ===== Seed permissions =====
INSERT INTO permissions (name, description) VALUES
                                                ('create_student', 'Create a student record'),
                                                ('edit_student', 'Edit a student record'),
                                                ('view_student', 'View student records'),

                                                ('manage_courses', 'Create/edit courses and units'),
                                                ('manage_intakes', 'Create/edit intakes'),
                                                ('manage_grading_config', 'Configure assessment weights and grading scale'),

                                                ('create_invoice', 'Generate invoices'),
                                                ('record_payment', 'Record manual payments'),
                                                ('view_finance', 'View financial records'),

                                                ('enter_marks', 'Enter marks for a unit'),
                                                ('approve_results', 'Approve and publish results'),
                                                ('assign_trainer', 'Assign trainers to units'),
                                                ('mark_attendance', 'Record student attendance'),

                                                ('manage_users', 'Create users and assign roles'),

                                                ('view_own_profile', 'View own student profile'),
                                                ('view_own_finance', 'View own invoices and balance'),
                                                ('view_own_results', 'View own published results');

-- ===== Seed role_permissions (logical defaults) =====
-- ADMIN: everything
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE name = 'ADMIN'), id FROM permissions;

-- REGISTRAR
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE name = 'REGISTRAR'), id FROM permissions
WHERE name IN ('create_student', 'edit_student', 'view_student', 'manage_intakes');

-- FINANCE
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE name = 'FINANCE'), id FROM permissions
WHERE name IN ('create_invoice', 'record_payment', 'view_finance', 'view_student');

-- TRAINER
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE name = 'TRAINER'), id FROM permissions
WHERE name IN ('enter_marks', 'mark_attendance', 'view_student');

-- HOD
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE name = 'HOD'), id FROM permissions
WHERE name IN ('approve_results', 'assign_trainer', 'view_student', 'manage_grading_config');

-- STUDENT
INSERT INTO role_permissions (role_id, permission_id)
SELECT (SELECT id FROM roles WHERE name = 'STUDENT'), id FROM permissions
WHERE name IN ('view_own_profile', 'view_own_finance', 'view_own_results');
