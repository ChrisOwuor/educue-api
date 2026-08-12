-- V7: Academic users. Add more rows using the same explicit department convention.
SET search_path TO public;

WITH academic_users(full_name,email,username,role_name,department_name) AS (VALUES
 ('Human Health HOD','hod@educue.local','health.hod','HOD','Human Health'),
 ('Human Health Trainer','trainer@educue.local','trainer','TRAINER','Human Health')
)
INSERT INTO users(full_name,email,username,password_hash,role_id,department_id,active,must_change_password,version,created_at,updated_at)
SELECT academic_users.full_name,academic_users.email,academic_users.username,
       crypt('ChangeMe@123',gen_salt('bf',10)),roles.id,departments.id,
       TRUE,TRUE,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
FROM academic_users
JOIN roles ON roles.name=academic_users.role_name
JOIN departments ON departments.name=academic_users.department_name
ON CONFLICT (email) DO NOTHING;
