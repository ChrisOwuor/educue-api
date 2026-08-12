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
VALUES
 ('Administration','System administration and initial institutional setup',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
 ('Registrar','Admissions, enrollment and academic records',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
 ('Finance','Fees, payments and institutional finance',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
 ('Human Health','Human health academic programmes and training',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

INSERT INTO institution_profiles (id,uuid,name,short_name,motto,registration_number,official_email,phone,address,website,version,created_at,updated_at)
VALUES (1,'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11','Apex Institute of Technology','AIT',
        'Excellence Through Innovation','REG-2026-90412','info@apexinstitute.edu','+1 (555) 019-2834',
        '123 University Ave, Suite 400, Tech City, CA 94016','https://www.apexinstitute.edu',0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO academic_years (uuid,code,start_date,start_year,end_date,current,closed,active,version,created_at,updated_at) VALUES
 (gen_random_uuid(),'2020/2021',DATE '2020-09-01',2020,DATE '2021-08-31',FALSE,TRUE,TRUE,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
 (gen_random_uuid(),'2021/2022',DATE '2021-09-01',2021,DATE '2022-08-31',FALSE,TRUE,TRUE,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
 (gen_random_uuid(),'2022/2023',DATE '2022-09-01',2022,DATE '2023-08-31',FALSE,TRUE,TRUE,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
 (gen_random_uuid(),'2023/2024',DATE '2023-09-01',2023,DATE '2024-08-31',FALSE,TRUE,TRUE,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
 (gen_random_uuid(),'2024/2025',DATE '2024-09-01',2024,DATE '2025-08-31',FALSE,TRUE,TRUE,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
 (gen_random_uuid(),'2025/2026',DATE '2025-09-01',2025,DATE '2026-08-31',TRUE,FALSE,TRUE,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
 (gen_random_uuid(),'2026/2027',DATE '2026-09-01',2026,DATE '2027-08-31',FALSE,FALSE,TRUE,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- The reusable starter catalogue is owned temporarily by Administration.
-- Units can be moved to their final academic departments during school setup.
INSERT INTO units (uuid,department_id,code,name,credit_hours,description,active,version,created_at,updated_at)
SELECT gen_random_uuid(),d.id,v.code,v.name,v.credits,v.description,TRUE,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
FROM departments d CROSS JOIN (VALUES
 ('CHN 101','Principles of Community Health',3,'Community diagnosis, prevention and primary healthcare principles'),
 ('CHN 102','Health Promotion and Education',3,'Planning and delivering behaviour-change and health-education interventions'),
 ('CHN 201','Epidemiology and Disease Surveillance',4,'Measurement, investigation and reporting of disease patterns'),
 ('CHN 202','Environmental and Occupational Health',3,'Environmental hazards, sanitation and workplace health protection'),
 ('CHN 301','Maternal and Child Community Health',4,'Community-level reproductive, maternal, newborn and child health services'),
 ('CHN 302','Community Health Practicum',6,'Supervised community assessment, intervention and evaluation practice'),
 ('FND 101','Medical Terminology',3,'Medical vocabulary used in clinical documentation and communication'),
 ('COM 102','Communication Skills in Healthcare',3,'Patient-centred, interprofessional and written healthcare communication'),
 ('ANA 101','Human Anatomy and Physiology',4,'Structure and normal function of major human body systems'),
 ('BCH 102','Medical Biochemistry',4,'Biochemical processes relevant to human health and disease'),
 ('ETH 101','Healthcare Ethics and Law',3,'Ethical practice, patient rights, consent and health-sector legal duties'),
 ('BLS 102','First Aid and Basic Life Support',3,'Immediate emergency response, CPR and basic stabilization skills'),
 ('RES 201','Research Methods and Biostatistics',3,'Health research design, data interpretation and introductory statistics'),
 ('IPC 102','Infection Prevention and Control',3,'Standard precautions, sterilization, isolation and healthcare-associated infection control'),
 ('MED101','Clinical Examination and Diagnostics',4,'Patient history, physical examination and selection of diagnostic tests'),
 ('MED102','General Pathology',4,'Mechanisms, morphology and clinical effects of disease'),
 ('MED201','Clinical Pharmacology',4,'Drug actions, indications, contraindications and safe prescribing principles'),
 ('MED202','Internal Medicine',5,'Diagnosis and non-operative management of adult medical conditions'),
 ('MED301','General Surgery',5,'Surgical assessment, perioperative care and common surgical conditions'),
 ('MED302','Pediatrics and Child Health',5,'Diagnosis and management of common childhood conditions'),
 ('OBG 301','Obstetrics and Gynaecology',5,'Reproductive health, pregnancy care and common gynaecological conditions'),
 ('EMG 302','Emergency Medicine',5,'Triage, resuscitation and initial management of acute emergencies'),
 ('MLT101','Hematology I',4,'Blood cell morphology, hematological tests and common blood disorders'),
 ('MLT102','Clinical Chemistry I',4,'Biochemical analysis of body fluids for clinical diagnosis'),
 ('MLT201','Medical Microbiology',4,'Isolation and identification of medically important microorganisms'),
 ('MLT202','Immunology and Serology',4,'Immune mechanisms and serological diagnostic techniques'),
 ('MLT301','Histopathology and Cytology',5,'Tissue processing, microscopy and cellular diagnostic techniques'),
 ('MLT302','Laboratory Quality Management',4,'Quality assurance, biosafety, equipment control and laboratory accreditation principles'),
 ('NUR 101','Foundations of Nursing Practice',4,'Fundamental nursing procedures, safety, dignity and professional conduct'),
 ('NUR 102','Health Assessment',4,'Systematic history-taking, physical examination and nursing assessment'),
 ('NUR 201','Medical-Surgical Nursing I',4,'Nursing management of common adult medical and surgical conditions'),
 ('NUR 202','Maternal and Newborn Health',4,'Antenatal, intrapartum, postnatal and newborn nursing care'),
 ('NUR 301','Pediatric Nursing',4,'Nursing care of infants, children and adolescents'),
 ('NUR 302','Mental Health Nursing',4,'Assessment and nursing care for common mental-health conditions'),
 ('NUR 401','Nursing Leadership and Management',3,'Ward leadership, staffing, quality improvement and clinical governance'),
 ('NUR 402','Advanced Clinical Practicum',6,'Supervised consolidation of nursing competencies in clinical settings'),
 ('NUR 501','Advanced Nursing Research',4,'Advanced research design and evidence-based nursing practice'),
 ('NUR 502','Specialist Clinical Nursing',5,'Specialist assessment and advanced clinical nursing interventions'),
 ('NUR 601','Nursing Education and Policy',4,'Curriculum leadership, health policy and professional education'),
 ('NUR 602','Doctoral Nursing Practicum',6,'Advanced supervised practice, leadership and scholarly integration'),
 ('PHA101','Introduction to Pharmacy Practice',3,'Professional roles, workflows and standards in pharmacy practice'),
 ('PHA102','Pharmaceutics I',4,'Formulation, preparation and quality of common dosage forms'),
 ('PHA201','Pharmaceutical Chemistry',4,'Chemical properties, analysis and stability of medicinal compounds'),
 ('PHA202','Pharmacognosy',4,'Medicinal products derived from natural sources'),
 ('PHA301','Clinical Pharmacy and Therapeutics',5,'Medicine optimization and evidence-based therapeutic decision-making'),
 ('PHA302','Dispensing Practice and Pharmacy Law',4,'Safe dispensing, records, controlled medicines and pharmacy regulation')
) AS v(code,name,credits,description)
WHERE d.name='Human Health'
ON CONFLICT (code) DO NOTHING;

UPDATE units
SET code = regexp_replace(code, '^([A-Z]+)([1-6]0[12])$', E'\\1 \\2'),
    updated_at = CURRENT_TIMESTAMP
WHERE code ~ '^[A-Z]+[1-6]0[12]$';

UPDATE units
SET code = regexp_replace(code, '^([A-Z]+)([1-6]0[12])$', E'\\1 \\2'),
    updated_at = CURRENT_TIMESTAMP
WHERE code ~ '^[A-Z]+[1-6]0[12]$';

-- Unit codes keep a subject prefix while the numeric suffix identifies the
-- course stage: 101/102, 201/202 ... 601/602.
UPDATE units
SET code = regexp_replace(code, '^([A-Z]+)([1-6]0[12])$', '\1 \2'),
    updated_at = CURRENT_TIMESTAMP
WHERE code ~ '^[A-Z]+[1-6]0[12]$';
