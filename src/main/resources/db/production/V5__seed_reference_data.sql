-- Minimal fresh-database bootstrap. No institution, academic year, course,
-- intake, student, finance, clearance or graduation transaction data is seeded.
SET search_path TO public;

INSERT INTO roles (name, description) VALUES
 ('ADMIN','Full system administration'),
 ('REGISTRAR','Admissions, enrollment and academic records'),
 ('ADMISSIONS_OFFICER','Application and admission processing'),
 ('FINANCE','Fees, payments and student finance'),
 ('HOD','Department academic administration'),
 ('TRAINER','Teaching and assessment'),
 ('STUDENT','Student self-service')
ON CONFLICT (name) DO NOTHING;

INSERT INTO permissions (name, description) VALUES
 ('INSTITUTION_READ','View institution settings'),('INSTITUTION_MANAGE','Manage institution settings'),
 ('DEPARTMENT_READ','View departments'),('DEPARTMENT_MANAGE','Manage departments'),
 ('ACADEMIC_YEAR_READ','View academic configuration'),('ACADEMIC_YEAR_MANAGE','Manage academic configuration'),
 ('COURSE_READ','View courses'),('COURSE_MANAGE','Manage courses'),
 ('UNIT_READ','View units'),('UNIT_MANAGE','Manage units'),
 ('USER_READ','View users'),('USER_MANAGE','Manage users'),
 ('ROLE_READ','View roles and permissions'),('ROLE_MANAGE','Manage roles and permissions'),
 ('ADMISSION_READ','View admissions'),('ADMISSION_MANAGE','Manage admissions'),
 ('ENROLLMENT_READ','View enrollments'),('ENROLLMENT_MANAGE','Manage enrollments'),
 ('STUDENT_READ','View students'),('STUDENT_MANAGE','Manage students'),
 ('FINANCE_READ','View financial records'),('FINANCE_MANAGE','Manage financial records'),
 ('FEE_STRUCTURE_READ','View fee structures'),('FEE_STRUCTURE_MANAGE','Manage fee structures'),
 ('PAYMENT_READ','View payments'),('PAYMENT_MANAGE','Manage payments'),
 ('REPORT_READ','View reports'),('REPORT_EXPORT','Export reports'),
 ('TEACHING_READ','View teaching assignments'),('ATTENDANCE_MANAGE','Manage attendance'),
 ('ASSESSMENT_MANAGE','Manage assessments and marks'),('RESULT_READ','View results'),
 ('RESULT_MANAGE','Manage and approve results'),('PROFILE_READ','View own profile'),
 ('PROFILE_MANAGE','Manage own profile')
ON CONFLICT (name) DO NOTHING;

-- The system administrator receives every permission.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.name='ADMIN'
ON CONFLICT DO NOTHING;

-- Default least-privilege mappings for roles that administrators can assign later.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id,p.id FROM (VALUES
 ('REGISTRAR','INSTITUTION_READ'),('REGISTRAR','DEPARTMENT_READ'),('REGISTRAR','ACADEMIC_YEAR_READ'),('REGISTRAR','ACADEMIC_YEAR_MANAGE'),('REGISTRAR','COURSE_READ'),('REGISTRAR','UNIT_READ'),('REGISTRAR','ADMISSION_READ'),('REGISTRAR','ADMISSION_MANAGE'),('REGISTRAR','ENROLLMENT_READ'),('REGISTRAR','ENROLLMENT_MANAGE'),('REGISTRAR','STUDENT_READ'),('REGISTRAR','STUDENT_MANAGE'),('REGISTRAR','REPORT_READ'),('REGISTRAR','REPORT_EXPORT'),('REGISTRAR','PROFILE_READ'),('REGISTRAR','PROFILE_MANAGE'),
 ('ADMISSIONS_OFFICER','INSTITUTION_READ'),('ADMISSIONS_OFFICER','DEPARTMENT_READ'),('ADMISSIONS_OFFICER','ACADEMIC_YEAR_READ'),('ADMISSIONS_OFFICER','COURSE_READ'),('ADMISSIONS_OFFICER','UNIT_READ'),('ADMISSIONS_OFFICER','ADMISSION_READ'),('ADMISSIONS_OFFICER','ADMISSION_MANAGE'),('ADMISSIONS_OFFICER','ENROLLMENT_READ'),('ADMISSIONS_OFFICER','ENROLLMENT_MANAGE'),('ADMISSIONS_OFFICER','STUDENT_READ'),('ADMISSIONS_OFFICER','STUDENT_MANAGE'),('ADMISSIONS_OFFICER','PROFILE_READ'),('ADMISSIONS_OFFICER','PROFILE_MANAGE'),
 ('FINANCE','INSTITUTION_READ'),('FINANCE','ACADEMIC_YEAR_READ'),('FINANCE','COURSE_READ'),('FINANCE','ENROLLMENT_READ'),('FINANCE','STUDENT_READ'),('FINANCE','FINANCE_READ'),('FINANCE','FINANCE_MANAGE'),('FINANCE','FEE_STRUCTURE_READ'),('FINANCE','FEE_STRUCTURE_MANAGE'),('FINANCE','PAYMENT_READ'),('FINANCE','PAYMENT_MANAGE'),('FINANCE','REPORT_READ'),('FINANCE','REPORT_EXPORT'),('FINANCE','PROFILE_READ'),('FINANCE','PROFILE_MANAGE'),
 ('HOD','INSTITUTION_READ'),('HOD','DEPARTMENT_READ'),('HOD','ACADEMIC_YEAR_READ'),('HOD','COURSE_READ'),('HOD','COURSE_MANAGE'),('HOD','UNIT_READ'),('HOD','UNIT_MANAGE'),('HOD','USER_READ'),('HOD','STUDENT_READ'),('HOD','TEACHING_READ'),('HOD','ATTENDANCE_MANAGE'),('HOD','ASSESSMENT_MANAGE'),('HOD','RESULT_READ'),('HOD','RESULT_MANAGE'),('HOD','REPORT_READ'),('HOD','REPORT_EXPORT'),('HOD','PROFILE_READ'),('HOD','PROFILE_MANAGE'),
 ('TRAINER','INSTITUTION_READ'),('TRAINER','DEPARTMENT_READ'),('TRAINER','ACADEMIC_YEAR_READ'),('TRAINER','COURSE_READ'),('TRAINER','UNIT_READ'),('TRAINER','STUDENT_READ'),('TRAINER','TEACHING_READ'),('TRAINER','ATTENDANCE_MANAGE'),('TRAINER','ASSESSMENT_MANAGE'),('TRAINER','RESULT_READ'),('TRAINER','PROFILE_READ'),('TRAINER','PROFILE_MANAGE'),
 ('STUDENT','ACADEMIC_YEAR_READ'),('STUDENT','COURSE_READ'),('STUDENT','UNIT_READ'),('STUDENT','FINANCE_READ'),('STUDENT','PAYMENT_READ'),('STUDENT','RESULT_READ'),('STUDENT','PROFILE_READ'),('STUDENT','PROFILE_MANAGE')
) AS m(role_name,permission_name)
JOIN roles r ON r.name=m.role_name JOIN permissions p ON p.name=m.permission_name
ON CONFLICT DO NOTHING;

INSERT INTO departments (name,description,active,created_at,updated_at)
VALUES ('Administration','System administration and initial institutional setup',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- The reusable starter catalogue is owned temporarily by Administration.
-- Units can be moved to their final academic departments during school setup.
INSERT INTO units (uuid,department_id,code,name,credit_hours,description,active,version,created_at,updated_at)
SELECT gen_random_uuid(),d.id,v.code,v.name,v.credits,v.description,TRUE,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
FROM departments d CROSS JOIN (VALUES
 ('CHN101','Principles of Community Health',3,'Community diagnosis, prevention and primary healthcare principles'),
 ('CHN102','Health Promotion and Education',3,'Planning and delivering behaviour-change and health-education interventions'),
 ('CHN201','Epidemiology and Disease Surveillance',4,'Measurement, investigation and reporting of disease patterns'),
 ('CHN202','Environmental and Occupational Health',3,'Environmental hazards, sanitation and workplace health protection'),
 ('CHN301','Maternal and Child Community Health',4,'Community-level reproductive, maternal, newborn and child health services'),
 ('CHN302','Community Health Practicum',6,'Supervised community assessment, intervention and evaluation practice'),
 ('FND101','Medical Terminology',3,'Medical vocabulary used in clinical documentation and communication'),
 ('FND102','Communication Skills in Healthcare',3,'Patient-centred, interprofessional and written healthcare communication'),
 ('FND103','Human Anatomy and Physiology',4,'Structure and normal function of major human body systems'),
 ('FND104','Medical Biochemistry',4,'Biochemical processes relevant to human health and disease'),
 ('FND105','Healthcare Ethics and Law',3,'Ethical practice, patient rights, consent and health-sector legal duties'),
 ('FND106','First Aid and Basic Life Support',3,'Immediate emergency response, CPR and basic stabilization skills'),
 ('FND107','Research Methods and Biostatistics',3,'Health research design, data interpretation and introductory statistics'),
 ('FND108','Infection Prevention and Control',3,'Standard precautions, sterilization, isolation and healthcare-associated infection control'),
 ('MED101','Clinical Examination and Diagnostics',4,'Patient history, physical examination and selection of diagnostic tests'),
 ('MED102','General Pathology',4,'Mechanisms, morphology and clinical effects of disease'),
 ('MED201','Clinical Pharmacology',4,'Drug actions, indications, contraindications and safe prescribing principles'),
 ('MED202','Internal Medicine',5,'Diagnosis and non-operative management of adult medical conditions'),
 ('MED301','General Surgery',5,'Surgical assessment, perioperative care and common surgical conditions'),
 ('MED302','Pediatrics and Child Health',5,'Diagnosis and management of common childhood conditions'),
 ('MED303','Obstetrics and Gynaecology',5,'Reproductive health, pregnancy care and common gynaecological conditions'),
 ('MED304','Emergency Medicine',5,'Triage, resuscitation and initial management of acute emergencies'),
 ('MLT101','Hematology I',4,'Blood cell morphology, hematological tests and common blood disorders'),
 ('MLT102','Clinical Chemistry I',4,'Biochemical analysis of body fluids for clinical diagnosis'),
 ('MLT201','Medical Microbiology',4,'Isolation and identification of medically important microorganisms'),
 ('MLT202','Immunology and Serology',4,'Immune mechanisms and serological diagnostic techniques'),
 ('MLT301','Histopathology and Cytology',5,'Tissue processing, microscopy and cellular diagnostic techniques'),
 ('MLT302','Laboratory Quality Management',4,'Quality assurance, biosafety, equipment control and laboratory accreditation principles'),
 ('NUR101','Foundations of Nursing Practice',4,'Fundamental nursing procedures, safety, dignity and professional conduct'),
 ('NUR102','Health Assessment',4,'Systematic history-taking, physical examination and nursing assessment'),
 ('NUR201','Medical-Surgical Nursing I',4,'Nursing management of common adult medical and surgical conditions'),
 ('NUR202','Maternal and Newborn Health',4,'Antenatal, intrapartum, postnatal and newborn nursing care'),
 ('NUR301','Pediatric Nursing',4,'Nursing care of infants, children and adolescents'),
 ('NUR302','Mental Health Nursing',4,'Assessment and nursing care for common mental-health conditions'),
 ('NUR401','Nursing Leadership and Management',3,'Ward leadership, staffing, quality improvement and clinical governance'),
 ('NUR402','Advanced Clinical Practicum',6,'Supervised consolidation of nursing competencies in clinical settings'),
 ('PHA101','Introduction to Pharmacy Practice',3,'Professional roles, workflows and standards in pharmacy practice'),
 ('PHA102','Pharmaceutics I',4,'Formulation, preparation and quality of common dosage forms'),
 ('PHA201','Pharmaceutical Chemistry',4,'Chemical properties, analysis and stability of medicinal compounds'),
 ('PHA202','Pharmacognosy',4,'Medicinal products derived from natural sources'),
 ('PHA301','Clinical Pharmacy and Therapeutics',5,'Medicine optimization and evidence-based therapeutic decision-making'),
 ('PHA302','Dispensing Practice and Pharmacy Law',4,'Safe dispensing, records, controlled medicines and pharmacy regulation')
) AS v(code,name,credits,description)
WHERE d.name='Administration'
ON CONFLICT (code) DO NOTHING;

INSERT INTO users (full_name,email,password_hash,role_id,active,version,created_at,updated_at,department_id,must_change_password)
SELECT 'Frank','admin@educue.local',crypt('ChangeMe@123',gen_salt('bf',10)),r.id,TRUE,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,d.id,TRUE
FROM roles r CROSS JOIN departments d
WHERE r.name='ADMIN' AND d.name='Administration'
ON CONFLICT (email) DO NOTHING;

