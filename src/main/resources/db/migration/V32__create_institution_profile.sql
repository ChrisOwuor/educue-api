CREATE TABLE institution_profiles (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    name VARCHAR(180) NOT NULL,
    short_name VARCHAR(30),
    registration_number VARCHAR(80),
    motto VARCHAR(180),
    official_email VARCHAR(150),
    phone VARCHAR(30),
    website VARCHAR(255),
    address VARCHAR(300),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_institution_profiles_uuid UNIQUE (uuid),
    CONSTRAINT chk_single_institution_profile CHECK (id = 1)
);

-- EduCue currently manages one institution per installation. The fixed row
-- makes that invariant explicit and prevents accidentally creating profiles
-- whose branding and contact details would compete with each other.
INSERT INTO institution_profiles (
    id, name, short_name, motto, official_email, phone, website, address
)
VALUES (
    1,
    'EduCue Training Institution',
    'EDUCUE',
    'Knowledge, growth and service',
    'office@educue.test',
    '+254 700 000 000',
    'https://educue.test',
    'P.O. Box 00000, Nairobi, Kenya'
);

SELECT setval(
    pg_get_serial_sequence('institution_profiles', 'id'),
    (SELECT MAX(id) FROM institution_profiles)
);
