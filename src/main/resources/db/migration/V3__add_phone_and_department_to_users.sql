-- V3__add_phone_and_department_to_users.sql

ALTER TABLE users ADD COLUMN phone VARCHAR(20);
ALTER TABLE users ADD COLUMN department_id BIGINT REFERENCES departments(id);

CREATE INDEX idx_users_department_id ON users(department_id);
