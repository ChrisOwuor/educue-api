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

SET default_tablespace = '';

--
-- Name: academic_periods academic_periods_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.academic_periods
    ADD CONSTRAINT academic_periods_pkey PRIMARY KEY (id);


--
-- Name: academic_years academic_years_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.academic_years
    ADD CONSTRAINT academic_years_pkey PRIMARY KEY (id);


--
-- Name: application_documents application_documents_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.application_documents
    ADD CONSTRAINT application_documents_pkey PRIMARY KEY (id);


--
-- Name: applications applications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT applications_pkey PRIMARY KEY (id);


--
-- Name: course_academic_periods course_academic_periods_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.course_academic_periods
    ADD CONSTRAINT course_academic_periods_pkey PRIMARY KEY (id);


--
-- Name: course_unit_placements course_unit_placements_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.course_unit_placements
    ADD CONSTRAINT course_unit_placements_pkey PRIMARY KEY (id);


--
-- Name: courses courses_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.courses
    ADD CONSTRAINT courses_code_key UNIQUE (code);


--
-- Name: courses courses_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.courses
    ADD CONSTRAINT courses_pkey PRIMARY KEY (id);


--
-- Name: courses courses_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.courses
    ADD CONSTRAINT courses_uuid_key UNIQUE (uuid);


--
-- Name: departments departments_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.departments
    ADD CONSTRAINT departments_name_key UNIQUE (name);


--
-- Name: departments departments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.departments
    ADD CONSTRAINT departments_pkey PRIMARY KEY (id);


--
-- Name: enrollments enrollments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enrollments
    ADD CONSTRAINT enrollments_pkey PRIMARY KEY (id);


--
-- Name: exam_cards exam_cards_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.exam_cards
    ADD CONSTRAINT exam_cards_pkey PRIMARY KEY (id);


--
-- Name: exam_cards exam_cards_verification_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.exam_cards
    ADD CONSTRAINT exam_cards_verification_code_key UNIQUE (verification_code);


--
-- Name: fee_structure_items fee_structure_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fee_structure_items
    ADD CONSTRAINT fee_structure_items_pkey PRIMARY KEY (id);


--
-- Name: fee_structures fee_structures_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fee_structures
    ADD CONSTRAINT fee_structures_pkey PRIMARY KEY (id);


--
-- Name: institution_profiles institution_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_profiles
    ADD CONSTRAINT institution_profiles_pkey PRIMARY KEY (id);


--
-- Name: intake_courses intake_courses_intake_id_course_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.intake_courses
    ADD CONSTRAINT intake_courses_intake_id_course_id_key UNIQUE (intake_id, course_id);


--
-- Name: intake_courses intake_courses_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.intake_courses
    ADD CONSTRAINT intake_courses_pkey PRIMARY KEY (id);


--
-- Name: intakes intakes_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.intakes
    ADD CONSTRAINT intakes_name_key UNIQUE (name);


--
-- Name: intakes intakes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.intakes
    ADD CONSTRAINT intakes_pkey PRIMARY KEY (id);


--
-- Name: intakes intakes_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.intakes
    ADD CONSTRAINT intakes_uuid_key UNIQUE (uuid);


--
-- Name: lecturer_unit_assignments lecturer_unit_assignments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lecturer_unit_assignments
    ADD CONSTRAINT lecturer_unit_assignments_pkey PRIMARY KEY (id);


--
-- Name: mpesa_payment_events mpesa_payment_events_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mpesa_payment_events
    ADD CONSTRAINT mpesa_payment_events_pkey PRIMARY KEY (id);


--
-- Name: mpesa_stk_push_requests mpesa_stk_push_requests_checkout_request_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mpesa_stk_push_requests
    ADD CONSTRAINT mpesa_stk_push_requests_checkout_request_id_key UNIQUE (checkout_request_id);


--
-- Name: mpesa_stk_push_requests mpesa_stk_push_requests_idempotency_key_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mpesa_stk_push_requests
    ADD CONSTRAINT mpesa_stk_push_requests_idempotency_key_key UNIQUE (idempotency_key);


--
-- Name: mpesa_stk_push_requests mpesa_stk_push_requests_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mpesa_stk_push_requests
    ADD CONSTRAINT mpesa_stk_push_requests_pkey PRIMARY KEY (id);


--
-- Name: mpesa_stk_push_requests mpesa_stk_push_requests_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mpesa_stk_push_requests
    ADD CONSTRAINT mpesa_stk_push_requests_uuid_key UNIQUE (uuid);


--
-- Name: payments payments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_pkey PRIMARY KEY (id);


--
-- Name: permissions permissions_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.permissions
    ADD CONSTRAINT permissions_name_key UNIQUE (name);


--
-- Name: permissions permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.permissions
    ADD CONSTRAINT permissions_pkey PRIMARY KEY (id);


--
-- Name: role_permissions role_permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT role_permissions_pkey PRIMARY KEY (role_id, permission_id);


--
-- Name: roles roles_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_name_key UNIQUE (name);


--
-- Name: roles roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id);


--
-- Name: fee_ledger student_ledger_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fee_ledger
    ADD CONSTRAINT student_ledger_pkey PRIMARY KEY (id);


--
-- Name: student_results student_results_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_results
    ADD CONSTRAINT student_results_pkey PRIMARY KEY (id);


--
-- Name: student_results student_results_student_unit_registration_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_results
    ADD CONSTRAINT student_results_student_unit_registration_id_key UNIQUE (student_unit_registration_id);


--
-- Name: student_results student_results_uuid_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_results
    ADD CONSTRAINT student_results_uuid_key UNIQUE (uuid);


--
-- Name: student_unit_registrations student_unit_registrations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_unit_registrations
    ADD CONSTRAINT student_unit_registrations_pkey PRIMARY KEY (id);


--
-- Name: students students_admission_number_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.students
    ADD CONSTRAINT students_admission_number_key UNIQUE (admission_number);


--
-- Name: students students_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.students
    ADD CONSTRAINT students_pkey PRIMARY KEY (id);


--
-- Name: academic_periods uk_academic_period_type_year_period; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.academic_periods
    ADD CONSTRAINT uk_academic_period_type_year_period UNIQUE (period_type, year_number, period_number);


--
-- Name: academic_periods uk_academic_periods_code; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.academic_periods
    ADD CONSTRAINT uk_academic_periods_code UNIQUE (code);


--
-- Name: academic_periods uk_academic_periods_uuid; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.academic_periods
    ADD CONSTRAINT uk_academic_periods_uuid UNIQUE (uuid);


--
-- Name: academic_years uk_academic_years_code; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.academic_years
    ADD CONSTRAINT uk_academic_years_code UNIQUE (code);


--
-- Name: academic_years uk_academic_years_uuid; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.academic_years
    ADD CONSTRAINT uk_academic_years_uuid UNIQUE (uuid);


--
-- Name: applications uk_applications_application_number; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT uk_applications_application_number UNIQUE (application_number);


--
-- Name: course_academic_periods uk_course_academic_period; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.course_academic_periods
    ADD CONSTRAINT uk_course_academic_period UNIQUE (course_id, academic_period_id);


--
-- Name: course_academic_periods uk_course_academic_periods_uuid; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.course_academic_periods
    ADD CONSTRAINT uk_course_academic_periods_uuid UNIQUE (uuid);


--
-- Name: course_academic_periods uk_course_period_next; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.course_academic_periods
    ADD CONSTRAINT uk_course_period_next UNIQUE (next_period_id);


--
-- Name: course_academic_periods uk_course_period_position; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.course_academic_periods
    ADD CONSTRAINT uk_course_period_position UNIQUE (course_id, "position");


--
-- Name: course_unit_placements uk_course_unit_placement_start; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.course_unit_placements
    ADD CONSTRAINT uk_course_unit_placement_start UNIQUE (course_academic_period_id, unit_id, effective_from_intake_year);


--
-- Name: course_unit_placements uk_course_unit_placements_uuid; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.course_unit_placements
    ADD CONSTRAINT uk_course_unit_placements_uuid UNIQUE (uuid);


--
-- Name: exam_cards uk_exam_card_student_period; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.exam_cards
    ADD CONSTRAINT uk_exam_card_student_period UNIQUE (student_id, course_academic_period_id);


--
-- Name: fee_ledger uk_fee_ledger_document_number; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fee_ledger
    ADD CONSTRAINT uk_fee_ledger_document_number UNIQUE (document_number);


--
-- Name: fee_structures uk_fee_structure; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fee_structures
    ADD CONSTRAINT uk_fee_structure UNIQUE (intake_course_id, course_academic_period_id);


--
-- Name: institution_profiles uk_institution_profiles_uuid; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institution_profiles
    ADD CONSTRAINT uk_institution_profiles_uuid UNIQUE (uuid);


--
-- Name: lecturer_unit_assignments uk_lecturer_unit_assignments_uuid; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lecturer_unit_assignments
    ADD CONSTRAINT uk_lecturer_unit_assignments_uuid UNIQUE (uuid);


--
-- Name: mpesa_payment_events uk_mpesa_event_transaction; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mpesa_payment_events
    ADD CONSTRAINT uk_mpesa_event_transaction UNIQUE (transaction_id);


--
-- Name: payments uk_payment_gateway_reference; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT uk_payment_gateway_reference UNIQUE (gateway_reference);


--
-- Name: payments uk_payment_receipt_number; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT uk_payment_receipt_number UNIQUE (receipt_number);


--
-- Name: students uk_students_application; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.students
    ADD CONSTRAINT uk_students_application UNIQUE (application_id);


--
-- Name: students uk_students_user; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.students
    ADD CONSTRAINT uk_students_user UNIQUE (user_id);


--
-- Name: units uk_units_uuid; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.units
    ADD CONSTRAINT uk_units_uuid UNIQUE (uuid);


--
-- Name: units units_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.units
    ADD CONSTRAINT units_code_key UNIQUE (code);


--
-- Name: units units_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.units
    ADD CONSTRAINT units_pkey PRIMARY KEY (id);


--
-- Name: student_unit_registrations uq_student_unit_registration_placement; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_unit_registrations
    ADD CONSTRAINT uq_student_unit_registration_placement UNIQUE (enrollment_id, course_unit_placement_id);


--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: idx_academic_periods_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_academic_periods_active ON public.academic_periods USING btree (active);


--
-- Name: idx_academic_periods_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_academic_periods_type ON public.academic_periods USING btree (period_type);


--
-- Name: idx_academic_periods_type_sequence; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_academic_periods_type_sequence ON public.academic_periods USING btree (period_type, sequence_number);


--
-- Name: idx_academic_years_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_academic_years_active ON public.academic_years USING btree (active);


--
-- Name: idx_academic_years_current; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_academic_years_current ON public.academic_years USING btree (current);


--
-- Name: idx_academic_years_start_year; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_academic_years_start_year ON public.academic_years USING btree (start_year);


--
-- Name: idx_application_documents_application_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_application_documents_application_id ON public.application_documents USING btree (application_id);


--
-- Name: idx_applications_email; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_applications_email ON public.applications USING btree (email);


--
-- Name: idx_applications_intake_course; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_applications_intake_course ON public.applications USING btree (intake_course_id);


--
-- Name: idx_applications_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_applications_status ON public.applications USING btree (status);


--
-- Name: idx_course_period_course; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_course_period_course ON public.course_academic_periods USING btree (course_id);


--
-- Name: idx_course_period_next; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_course_period_next ON public.course_academic_periods USING btree (next_period_id);


--
-- Name: idx_course_unit_placements_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_course_unit_placements_active ON public.course_unit_placements USING btree (active);


--
-- Name: idx_course_unit_placements_course; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_course_unit_placements_course ON public.course_unit_placements USING btree (course_id);


--
-- Name: idx_course_unit_placements_course_academic_period; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_course_unit_placements_course_academic_period ON public.course_unit_placements USING btree (course_academic_period_id);


--
-- Name: idx_course_unit_placements_course_period; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_course_unit_placements_course_period ON public.course_unit_placements USING btree (course_id, academic_period_id);


--
-- Name: idx_course_unit_placements_course_period_effective_range; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_course_unit_placements_course_period_effective_range ON public.course_unit_placements USING btree (course_academic_period_id, effective_from_intake_year, effective_to_intake_year);


--
-- Name: idx_course_unit_placements_effective_range; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_course_unit_placements_effective_range ON public.course_unit_placements USING btree (course_id, effective_from_intake_year, effective_to_intake_year);


--
-- Name: idx_course_unit_placements_period; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_course_unit_placements_period ON public.course_unit_placements USING btree (academic_period_id);


--
-- Name: idx_course_unit_placements_unit; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_course_unit_placements_unit ON public.course_unit_placements USING btree (unit_id);


--
-- Name: idx_courses_code; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_courses_code ON public.courses USING btree (code);


--
-- Name: idx_courses_department_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_courses_department_id ON public.courses USING btree (department_id);


--
-- Name: idx_departments_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_departments_created_at ON public.departments USING btree (created_at);


--
-- Name: idx_departments_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_departments_name ON public.departments USING btree (name);


--
-- Name: idx_enrollments_current_course_period; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_enrollments_current_course_period ON public.enrollments USING btree (current_course_academic_period_id);


--
-- Name: idx_enrollments_intake_course; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_enrollments_intake_course ON public.enrollments USING btree (intake_course_id);


--
-- Name: idx_exam_card_verification_code; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_exam_card_verification_code ON public.exam_cards USING btree (verification_code);


--
-- Name: idx_fee_ledger_course_period; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fee_ledger_course_period ON public.fee_ledger USING btree (course_academic_period_id);


--
-- Name: idx_fee_ledger_external_reference; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fee_ledger_external_reference ON public.fee_ledger USING btree (external_reference) WHERE (external_reference IS NOT NULL);


--
-- Name: idx_fee_ledger_statement_order; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fee_ledger_statement_order ON public.fee_ledger USING btree (student_id, posting_date, created_at, id);


--
-- Name: idx_fee_ledger_student; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fee_ledger_student ON public.fee_ledger USING btree (student_id);


--
-- Name: idx_fee_ledger_student_posting; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fee_ledger_student_posting ON public.fee_ledger USING btree (student_id, posting_date, id);


--
-- Name: idx_fee_structures_course_period; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fee_structures_course_period ON public.fee_structures USING btree (course_academic_period_id);


--
-- Name: idx_fee_structures_intake_course; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fee_structures_intake_course ON public.fee_structures USING btree (intake_course_id);


--
-- Name: idx_intakes_academic_year; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_intakes_academic_year ON public.intakes USING btree (academic_year_id);


--
-- Name: idx_lecturer_assignment_assigned_by; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_lecturer_assignment_assigned_by ON public.lecturer_unit_assignments USING btree (assigned_by);


--
-- Name: idx_lecturer_assignment_effective_years; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_lecturer_assignment_effective_years ON public.lecturer_unit_assignments USING btree (effective_from_academic_year_id, effective_to_academic_year_id);


--
-- Name: idx_lecturer_assignment_enabled; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_lecturer_assignment_enabled ON public.lecturer_unit_assignments USING btree (enabled);


--
-- Name: idx_lecturer_assignment_lecturer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_lecturer_assignment_lecturer ON public.lecturer_unit_assignments USING btree (lecturer_id);


--
-- Name: idx_lecturer_assignment_placement; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_lecturer_assignment_placement ON public.lecturer_unit_assignments USING btree (course_unit_placement_id);


--
-- Name: idx_mpesa_event_account_reference; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_mpesa_event_account_reference ON public.mpesa_payment_events USING btree (account_reference);


--
-- Name: idx_mpesa_event_work_queue; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_mpesa_event_work_queue ON public.mpesa_payment_events USING btree (status, next_attempt_at, received_at);


--
-- Name: idx_payment_student; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_student ON public.payments USING btree (student_id);


--
-- Name: idx_payments_business_order; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payments_business_order ON public.payments USING btree (paid_at, created_at, id);


--
-- Name: idx_payments_course_period; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payments_course_period ON public.payments USING btree (applied_course_academic_period_id);


--
-- Name: idx_payments_payer_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payments_payer_type ON public.payments USING btree (payer_type);


--
-- Name: idx_stk_pending; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_stk_pending ON public.mpesa_stk_push_requests USING btree (status, requested_at);


--
-- Name: idx_stk_student_requested; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_stk_student_requested ON public.mpesa_stk_push_requests USING btree (student_id, requested_at DESC);


--
-- Name: idx_student_registration_placement; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_student_registration_placement ON public.student_unit_registrations USING btree (course_unit_placement_id);


--
-- Name: idx_student_results_approved_by; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_student_results_approved_by ON public.student_results USING btree (approved_by_id);


--
-- Name: idx_student_results_recorded_by; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_student_results_recorded_by ON public.student_results USING btree (recorded_by_id);


--
-- Name: idx_student_results_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_student_results_status ON public.student_results USING btree (status);


--
-- Name: idx_students_admission_number; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_students_admission_number ON public.students USING btree (admission_number);


--
-- Name: idx_sureg_enrollment; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sureg_enrollment ON public.student_unit_registrations USING btree (enrollment_id);


--
-- Name: idx_sureg_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sureg_status ON public.student_unit_registrations USING btree (status);


--
-- Name: idx_units_code; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_units_code ON public.units USING btree (code);


--
-- Name: idx_units_department_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_units_department_id ON public.units USING btree (department_id);


--
-- Name: idx_users_department_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_department_id ON public.users USING btree (department_id);


--
-- Name: idx_users_email; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_email ON public.users USING btree (email);


--
-- Name: idx_users_role_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_role_id ON public.users USING btree (role_id);


--
-- Name: uk_academic_years_one_current; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_academic_years_one_current ON public.academic_years USING btree (current) WHERE (current = true);


--
-- Name: uk_fee_ledger_one_reversal; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_fee_ledger_one_reversal ON public.fee_ledger USING btree (reversal_of_id) WHERE (reversal_of_id IS NOT NULL);


--
-- Name: uk_fee_ledger_student_fee_structure; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_fee_ledger_student_fee_structure ON public.fee_ledger USING btree (student_id, fee_structure_id) WHERE (fee_structure_id IS NOT NULL);


--
-- Name: application_documents application_documents_application_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.application_documents
    ADD CONSTRAINT application_documents_application_id_fkey FOREIGN KEY (application_id) REFERENCES public.applications(id) ON DELETE CASCADE;


--
-- Name: courses courses_department_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.courses
    ADD CONSTRAINT courses_department_id_fkey FOREIGN KEY (department_id) REFERENCES public.departments(id);


--
-- Name: exam_cards exam_cards_course_academic_period_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.exam_cards
    ADD CONSTRAINT exam_cards_course_academic_period_id_fkey FOREIGN KEY (course_academic_period_id) REFERENCES public.course_academic_periods(id);


--
-- Name: exam_cards exam_cards_student_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.exam_cards
    ADD CONSTRAINT exam_cards_student_id_fkey FOREIGN KEY (student_id) REFERENCES public.students(id);


--
-- Name: fee_structure_items fee_structure_items_fee_structure_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fee_structure_items
    ADD CONSTRAINT fee_structure_items_fee_structure_id_fkey FOREIGN KEY (fee_structure_id) REFERENCES public.fee_structures(id) ON DELETE CASCADE;


--
-- Name: applications fk_applications_approved_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT fk_applications_approved_by FOREIGN KEY (approved_by_user_id) REFERENCES public.users(id);


--
-- Name: applications fk_applications_intake_course; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT fk_applications_intake_course FOREIGN KEY (intake_course_id) REFERENCES public.intake_courses(id) ON DELETE RESTRICT;


--
-- Name: course_academic_periods fk_course_period_academic_period; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.course_academic_periods
    ADD CONSTRAINT fk_course_period_academic_period FOREIGN KEY (academic_period_id) REFERENCES public.academic_periods(id) ON DELETE RESTRICT;


--
-- Name: course_academic_periods fk_course_period_course; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.course_academic_periods
    ADD CONSTRAINT fk_course_period_course FOREIGN KEY (course_id) REFERENCES public.courses(id) ON DELETE RESTRICT;


--
-- Name: course_academic_periods fk_course_period_next; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.course_academic_periods
    ADD CONSTRAINT fk_course_period_next FOREIGN KEY (next_period_id) REFERENCES public.course_academic_periods(id) ON DELETE RESTRICT;


--
-- Name: course_unit_placements fk_course_unit_placement_course; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.course_unit_placements
    ADD CONSTRAINT fk_course_unit_placement_course FOREIGN KEY (course_id) REFERENCES public.courses(id) ON DELETE RESTRICT;


--
-- Name: course_unit_placements fk_course_unit_placement_course_period; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.course_unit_placements
    ADD CONSTRAINT fk_course_unit_placement_course_period FOREIGN KEY (course_academic_period_id) REFERENCES public.course_academic_periods(id) ON DELETE RESTRICT;


--
-- Name: course_unit_placements fk_course_unit_placement_period; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.course_unit_placements
    ADD CONSTRAINT fk_course_unit_placement_period FOREIGN KEY (academic_period_id) REFERENCES public.academic_periods(id) ON DELETE RESTRICT;


--
-- Name: course_unit_placements fk_course_unit_placement_unit; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.course_unit_placements
    ADD CONSTRAINT fk_course_unit_placement_unit FOREIGN KEY (unit_id) REFERENCES public.units(id) ON DELETE RESTRICT;


--
-- Name: enrollments fk_enrollment_current_course_period; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enrollments
    ADD CONSTRAINT fk_enrollment_current_course_period FOREIGN KEY (current_course_academic_period_id) REFERENCES public.course_academic_periods(id) ON DELETE RESTRICT;


--
-- Name: enrollments fk_enrollment_intake_course; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enrollments
    ADD CONSTRAINT fk_enrollment_intake_course FOREIGN KEY (intake_course_id) REFERENCES public.intake_courses(id) ON DELETE RESTRICT;


--
-- Name: enrollments fk_enrollment_student; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enrollments
    ADD CONSTRAINT fk_enrollment_student FOREIGN KEY (student_id) REFERENCES public.students(id);


--
-- Name: fee_ledger fk_fee_ledger_course_period; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fee_ledger
    ADD CONSTRAINT fk_fee_ledger_course_period FOREIGN KEY (course_academic_period_id) REFERENCES public.course_academic_periods(id) ON DELETE RESTRICT;


--
-- Name: fee_ledger fk_fee_ledger_fee_structure; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fee_ledger
    ADD CONSTRAINT fk_fee_ledger_fee_structure FOREIGN KEY (fee_structure_id) REFERENCES public.fee_structures(id);


--
-- Name: fee_ledger fk_fee_ledger_reversal_of; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fee_ledger
    ADD CONSTRAINT fk_fee_ledger_reversal_of FOREIGN KEY (reversal_of_id) REFERENCES public.fee_ledger(id);


--
-- Name: fee_structures fk_fee_structure_course_period; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fee_structures
    ADD CONSTRAINT fk_fee_structure_course_period FOREIGN KEY (course_academic_period_id) REFERENCES public.course_academic_periods(id) ON DELETE RESTRICT;


--
-- Name: fee_structures fk_fee_structure_intake_course; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fee_structures
    ADD CONSTRAINT fk_fee_structure_intake_course FOREIGN KEY (intake_course_id) REFERENCES public.intake_courses(id) ON DELETE RESTRICT;


--
-- Name: intakes fk_intakes_academic_year; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.intakes
    ADD CONSTRAINT fk_intakes_academic_year FOREIGN KEY (academic_year_id) REFERENCES public.academic_years(id) ON DELETE RESTRICT;


--
-- Name: lecturer_unit_assignments fk_lecturer_assignment_assigned_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lecturer_unit_assignments
    ADD CONSTRAINT fk_lecturer_assignment_assigned_by FOREIGN KEY (assigned_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: lecturer_unit_assignments fk_lecturer_assignment_from_year; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lecturer_unit_assignments
    ADD CONSTRAINT fk_lecturer_assignment_from_year FOREIGN KEY (effective_from_academic_year_id) REFERENCES public.academic_years(id) ON DELETE RESTRICT;


--
-- Name: lecturer_unit_assignments fk_lecturer_assignment_lecturer; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lecturer_unit_assignments
    ADD CONSTRAINT fk_lecturer_assignment_lecturer FOREIGN KEY (lecturer_id) REFERENCES public.users(id) ON DELETE RESTRICT;


--
-- Name: lecturer_unit_assignments fk_lecturer_assignment_placement; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lecturer_unit_assignments
    ADD CONSTRAINT fk_lecturer_assignment_placement FOREIGN KEY (course_unit_placement_id) REFERENCES public.course_unit_placements(id) ON DELETE RESTRICT;


--
-- Name: lecturer_unit_assignments fk_lecturer_assignment_to_year; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lecturer_unit_assignments
    ADD CONSTRAINT fk_lecturer_assignment_to_year FOREIGN KEY (effective_to_academic_year_id) REFERENCES public.academic_years(id) ON DELETE RESTRICT;


--
-- Name: payments fk_payment_course_period; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fk_payment_course_period FOREIGN KEY (applied_course_academic_period_id) REFERENCES public.course_academic_periods(id) ON DELETE RESTRICT;


--
-- Name: student_unit_registrations fk_student_registration_placement; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_unit_registrations
    ADD CONSTRAINT fk_student_registration_placement FOREIGN KEY (course_unit_placement_id) REFERENCES public.course_unit_placements(id) ON DELETE RESTRICT;


--
-- Name: student_results fk_student_results_approved_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_results
    ADD CONSTRAINT fk_student_results_approved_by FOREIGN KEY (approved_by_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: student_results fk_student_results_recorded_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_results
    ADD CONSTRAINT fk_student_results_recorded_by FOREIGN KEY (recorded_by_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: student_results fk_student_results_registration; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_results
    ADD CONSTRAINT fk_student_results_registration FOREIGN KEY (student_unit_registration_id) REFERENCES public.student_unit_registrations(id) ON DELETE CASCADE;


--
-- Name: students fk_students_application; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.students
    ADD CONSTRAINT fk_students_application FOREIGN KEY (application_id) REFERENCES public.applications(id);


--
-- Name: students fk_students_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.students
    ADD CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: intake_courses intake_courses_course_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.intake_courses
    ADD CONSTRAINT intake_courses_course_id_fkey FOREIGN KEY (course_id) REFERENCES public.courses(id);


--
-- Name: intake_courses intake_courses_intake_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.intake_courses
    ADD CONSTRAINT intake_courses_intake_id_fkey FOREIGN KEY (intake_id) REFERENCES public.intakes(id) ON DELETE CASCADE;


--
-- Name: mpesa_payment_events mpesa_payment_events_payment_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mpesa_payment_events
    ADD CONSTRAINT mpesa_payment_events_payment_id_fkey FOREIGN KEY (payment_id) REFERENCES public.payments(id);


--
-- Name: mpesa_stk_push_requests mpesa_stk_push_requests_course_academic_period_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mpesa_stk_push_requests
    ADD CONSTRAINT mpesa_stk_push_requests_course_academic_period_id_fkey FOREIGN KEY (course_academic_period_id) REFERENCES public.course_academic_periods(id);


--
-- Name: mpesa_stk_push_requests mpesa_stk_push_requests_student_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mpesa_stk_push_requests
    ADD CONSTRAINT mpesa_stk_push_requests_student_id_fkey FOREIGN KEY (student_id) REFERENCES public.students(id);


--
-- Name: payments payments_recorded_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_recorded_by_fkey FOREIGN KEY (recorded_by) REFERENCES public.users(id);


--
-- Name: payments payments_student_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_student_id_fkey FOREIGN KEY (student_id) REFERENCES public.students(id);


--
-- Name: role_permissions role_permissions_permission_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT role_permissions_permission_id_fkey FOREIGN KEY (permission_id) REFERENCES public.permissions(id) ON DELETE CASCADE;


--
-- Name: role_permissions role_permissions_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT role_permissions_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.roles(id) ON DELETE CASCADE;


--
-- Name: fee_ledger student_ledger_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fee_ledger
    ADD CONSTRAINT student_ledger_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(id);


--
-- Name: fee_ledger student_ledger_payment_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fee_ledger
    ADD CONSTRAINT student_ledger_payment_id_fkey FOREIGN KEY (payment_id) REFERENCES public.payments(id);


--
-- Name: fee_ledger student_ledger_student_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fee_ledger
    ADD CONSTRAINT student_ledger_student_id_fkey FOREIGN KEY (student_id) REFERENCES public.students(id);


--
-- Name: units units_department_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.units
    ADD CONSTRAINT units_department_id_fkey FOREIGN KEY (department_id) REFERENCES public.departments(id);


--
-- Name: users users_department_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_department_id_fkey FOREIGN KEY (department_id) REFERENCES public.departments(id);


--
-- Name: users users_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.roles(id);


--
-- PostgreSQL database dump complete
--

