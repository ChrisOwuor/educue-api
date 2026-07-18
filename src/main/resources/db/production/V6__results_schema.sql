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

SET default_table_access_method = heap;

--
-- Name: student_results; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.student_results (
    id bigint NOT NULL,
    uuid uuid NOT NULL,
    version bigint,
    student_unit_registration_id bigint NOT NULL,
    ca_marks numeric(5,2),
    exam_marks numeric(5,2),
    total_marks numeric(5,2),
    grade character varying(10),
    passed boolean DEFAULT false NOT NULL,
    status character varying(20) DEFAULT 'DRAFT'::character varying NOT NULL,
    remarks text,
    recorded_by_id bigint,
    approved_by_id bigint,
    approved_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_student_results_ca_marks CHECK (((ca_marks IS NULL) OR ((ca_marks >= (0)::numeric) AND (ca_marks <= (100)::numeric)))),
    CONSTRAINT chk_student_results_exam_marks CHECK (((exam_marks IS NULL) OR ((exam_marks >= (0)::numeric) AND (exam_marks <= (100)::numeric)))),
    CONSTRAINT chk_student_results_status CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'SUBMITTED'::character varying, 'APPROVED'::character varying, 'RELEASED'::character varying, 'WITHHELD'::character varying])::text[]))),
    CONSTRAINT chk_student_results_total_marks CHECK (((total_marks IS NULL) OR ((total_marks >= (0)::numeric) AND (total_marks <= (100)::numeric))))
);


--
-- Name: student_results_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.student_results_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: student_results_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.student_results_id_seq OWNED BY public.student_results.id;


--
-- Name: student_results id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_results ALTER COLUMN id SET DEFAULT nextval('public.student_results_id_seq'::regclass);


--
-- PostgreSQL database dump complete
--

