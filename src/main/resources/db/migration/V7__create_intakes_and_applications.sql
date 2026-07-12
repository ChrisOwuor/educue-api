-- V7__create_intakes_and_applications.sql

CREATE TABLE intakes (
                         id                    BIGSERIAL PRIMARY KEY,
                         uuid                  UUID NOT NULL UNIQUE,
                         name                  VARCHAR(100) NOT NULL UNIQUE,
                         start_date            DATE NOT NULL,
                         application_deadline  DATE NOT NULL,
                         status                VARCHAR(20) NOT NULL DEFAULT 'UPCOMING',
                         created_at            TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE intake_courses (
                                id         BIGSERIAL PRIMARY KEY,
                                intake_id  BIGINT NOT NULL REFERENCES intakes(id) ON DELETE CASCADE,
                                course_id  BIGINT NOT NULL REFERENCES courses(id),
                                UNIQUE (intake_id, course_id)
);

CREATE TABLE applications (
                              id              BIGSERIAL PRIMARY KEY,
                              intake_id       BIGINT NOT NULL REFERENCES intakes(id),
                              course_id       BIGINT NOT NULL REFERENCES courses(id),

                              full_name       VARCHAR(150) NOT NULL,
                              email           VARCHAR(150) NOT NULL,
                              phone           VARCHAR(20) NOT NULL,
                              national_id     VARCHAR(30),
                              date_of_birth   DATE,
                              guardian_name   VARCHAR(150),
                              guardian_phone  VARCHAR(20),

                              status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                              review_notes    VARCHAR(500),

                              submitted_at    TIMESTAMP NOT NULL DEFAULT now(),
                              reviewed_at     TIMESTAMP
);

CREATE INDEX idx_applications_intake_id ON applications(intake_id);
CREATE INDEX idx_applications_status ON applications(status);
CREATE INDEX idx_applications_email ON applications(email);

CREATE TABLE application_documents (
                                       id                  BIGSERIAL PRIMARY KEY,
                                       application_id      BIGINT NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
                                       document_type       VARCHAR(30) NOT NULL,
                                       s3_key              VARCHAR(500) NOT NULL,
                                       original_filename   VARCHAR(255),
                                       content_type        VARCHAR(100),
                                       file_size_bytes     BIGINT,
                                       uploaded_at         TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_application_documents_application_id ON application_documents(application_id);
