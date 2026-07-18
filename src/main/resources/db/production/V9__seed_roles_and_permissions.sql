--
-- PostgreSQL database dump
--

-- Dumped from database version 17.5
-- Dumped by pg_dump version 17.5

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Data for Name: permissions; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.permissions (id, name, description) VALUES (1, 'create_student', 'Create a student record');
INSERT INTO public.permissions (id, name, description) VALUES (2, 'edit_student', 'Edit a student record');
INSERT INTO public.permissions (id, name, description) VALUES (3, 'view_student', 'View student records');
INSERT INTO public.permissions (id, name, description) VALUES (4, 'manage_courses', 'Create/edit courses and units');
INSERT INTO public.permissions (id, name, description) VALUES (5, 'manage_intakes', 'Create/edit intakes');
INSERT INTO public.permissions (id, name, description) VALUES (6, 'manage_grading_config', 'Configure assessment weights and grading scale');
INSERT INTO public.permissions (id, name, description) VALUES (7, 'create_invoice', 'Generate invoices');
INSERT INTO public.permissions (id, name, description) VALUES (8, 'record_payment', 'Record manual payments');
INSERT INTO public.permissions (id, name, description) VALUES (9, 'view_finance', 'View financial records');
INSERT INTO public.permissions (id, name, description) VALUES (10, 'enter_marks', 'Enter marks for a unit');
INSERT INTO public.permissions (id, name, description) VALUES (11, 'approve_results', 'Approve and publish results');
INSERT INTO public.permissions (id, name, description) VALUES (12, 'assign_trainer', 'Assign trainers to units');
INSERT INTO public.permissions (id, name, description) VALUES (13, 'mark_attendance', 'Record student attendance');
INSERT INTO public.permissions (id, name, description) VALUES (14, 'manage_users', 'Create users and assign roles');
INSERT INTO public.permissions (id, name, description) VALUES (15, 'view_own_profile', 'View own student profile');
INSERT INTO public.permissions (id, name, description) VALUES (16, 'view_own_finance', 'View own invoices and balance');
INSERT INTO public.permissions (id, name, description) VALUES (17, 'view_own_results', 'View own published results');


--
-- Data for Name: roles; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.roles (id, name, description) VALUES (1, 'ADMIN', 'Full system access');
INSERT INTO public.roles (id, name, description) VALUES (2, 'REGISTRAR', 'Manages admissions, students, enrollment');
INSERT INTO public.roles (id, name, description) VALUES (3, 'FINANCE', 'Manages invoices and payments');
INSERT INTO public.roles (id, name, description) VALUES (4, 'TRAINER', 'Enters marks and attendance for assigned units');
INSERT INTO public.roles (id, name, description) VALUES (5, 'HOD', 'Approves results, assigns trainers');
INSERT INTO public.roles (id, name, description) VALUES (6, 'STUDENT', 'Views own profile, finance, and results');


--
-- Data for Name: role_permissions; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.role_permissions (role_id, permission_id) VALUES (1, 1);
INSERT INTO public.role_permissions (role_id, permission_id) VALUES (1, 2);
INSERT INTO public.role_permissions (role_id, permission_id) VALUES (1, 3);
INSERT INTO public.role_permissions (role_id, permission_id) VALUES (1, 4);
INSERT INTO public.role_permissions (role_id, permission_id) VALUES (1, 5);
INSERT INTO public.role_permissions (role_id, permission_id) VALUES (1, 6);
INSERT INTO public.role_permissions (role_id, permission_id) VALUES (1, 7);
INSERT INTO public.role_permissions (role_id, permission_id) VALUES (1, 8);
INSERT INTO public.role_permissions (role_id, permission_id) VALUES (1, 9);
INSERT INTO public.role_permissions (role_id, permission_id) VALUES (1, 10);
INSERT INTO public.role_permissions (role_id, permission_id) VALUES (1, 11);
INSERT INTO public.role_permissions (role_id, permission_id) VALUES (1, 12);
INSERT INTO public.role_permissions (role_id, permission_id) VALUES (1, 13);
INSERT INTO public.role_permissions (role_id, permission_id) VALUES (1, 14);
INSERT INTO public.role_permissions (role_id, permission_id) VALUES (1, 15);
INSERT INTO public.role_permissions (role_id, permission_id) VALUES (1, 16);
INSERT INTO public.role_permissions (role_id, permission_id) VALUES (1, 17);
INSERT INTO public.role_permissions (role_id, permission_id) VALUES (2, 1);
INSERT INTO public.role_permissions (role_id, permission_id) VALUES (2, 2);
INSERT INTO public.role_permissions (role_id, permission_id) VALUES (2, 3);
INSERT INTO public.role_permissions (role_id, permission_id) VALUES (2, 5);
INSERT INTO public.role_permissions (role_id, permission_id) VALUES (3, 3);
INSERT INTO public.role_permissions (role_id, permission_id) VALUES (3, 7);
INSERT INTO public.role_permissions (role_id, permission_id) VALUES (3, 8);
INSERT INTO public.role_permissions (role_id, permission_id) VALUES (3, 9);
INSERT INTO public.role_permissions (role_id, permission_id) VALUES (4, 3);
INSERT INTO public.role_permissions (role_id, permission_id) VALUES (4, 10);
INSERT INTO public.role_permissions (role_id, permission_id) VALUES (4, 13);
INSERT INTO public.role_permissions (role_id, permission_id) VALUES (5, 3);
INSERT INTO public.role_permissions (role_id, permission_id) VALUES (5, 6);
INSERT INTO public.role_permissions (role_id, permission_id) VALUES (5, 11);
INSERT INTO public.role_permissions (role_id, permission_id) VALUES (5, 12);
INSERT INTO public.role_permissions (role_id, permission_id) VALUES (6, 15);
INSERT INTO public.role_permissions (role_id, permission_id) VALUES (6, 16);
INSERT INTO public.role_permissions (role_id, permission_id) VALUES (6, 17);


--
-- Name: permissions_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.permissions_id_seq', 17, true);


--
-- Name: roles_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.roles_id_seq', 6, true);


--
-- PostgreSQL database dump complete
--

