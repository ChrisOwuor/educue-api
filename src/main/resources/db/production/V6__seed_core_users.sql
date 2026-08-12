-- V6: Core administrative users. Every user belongs to a department.
SET search_path TO public;

WITH core_users(full_name,email,username,role_name,department_name) AS (VALUES
 ('System Administrator','admin@educue.local','admin','ADMIN','Administration'),
 ('College Registrar','registrar@educue.local','registrar','REGISTRAR','Registrar'),
 ('Finance Officer','finance@educue.local','finance','FINANCE','Finance')
)
INSERT INTO users(full_name,email,username,password_hash,role_id,department_id,active,must_change_password,version,created_at,updated_at)
SELECT core_users.full_name,core_users.email,core_users.username,
       crypt('ChangeMe@123',gen_salt('bf',10)),roles.id,departments.id,
       TRUE,TRUE,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
FROM core_users
JOIN roles ON roles.name=core_users.role_name
JOIN departments ON departments.name=core_users.department_name
ON CONFLICT (email) DO NOTHING;
