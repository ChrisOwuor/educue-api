-- V2__create_departments.sql

CREATE TABLE departments (
                             id BIGSERIAL PRIMARY KEY,
                             name VARCHAR(100) NOT NULL UNIQUE,
                             description VARCHAR(255),
                             active BOOLEAN NOT NULL DEFAULT TRUE,
                             created_at TIMESTAMP NOT NULL DEFAULT now(),
                             updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_departments_name ON departments(name);
CREATE INDEX idx_departments_created_at ON departments(created_at);
