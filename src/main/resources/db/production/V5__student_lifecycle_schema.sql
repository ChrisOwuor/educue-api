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
-- Name: enrollments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.enrollments (
    id bigint NOT NULL,
    student_id bigint NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    admission_date date DEFAULT CURRENT_DATE NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    intake_course_id bigint NOT NULL,
    current_course_academic_period_id bigint NOT NULL
);


--
-- Name: enrollments_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.enrollments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: enrollments_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.enrollments_id_seq OWNED BY public.enrollments.id;


--
-- Name: exam_cards; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.exam_cards (
    id bigint NOT NULL,
    verification_code uuid NOT NULL,
    student_id bigint NOT NULL,
    course_academic_period_id bigint NOT NULL,
    issued_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: exam_cards_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.exam_cards_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: exam_cards_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.exam_cards_id_seq OWNED BY public.exam_cards.id;


--
-- Name: student_unit_registrations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.student_unit_registrations (
    id bigint NOT NULL,
    enrollment_id bigint NOT NULL,
    attempt_type character varying(20) DEFAULT 'NORMAL'::character varying NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    registered_at timestamp without time zone DEFAULT now() NOT NULL,
    course_unit_placement_id bigint NOT NULL
);


--
-- Name: student_unit_registrations_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.student_unit_registrations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: student_unit_registrations_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.student_unit_registrations_id_seq OWNED BY public.student_unit_registrations.id;


--
-- Name: students; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.students (
    id bigint NOT NULL,
    admission_number character varying(30) NOT NULL,
    full_name character varying(150) NOT NULL,
    email character varying(150),
    phone character varying(20),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    application_id bigint NOT NULL,
    user_id bigint NOT NULL
);


--
-- Name: students_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.students_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: students_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.students_id_seq OWNED BY public.students.id;


--
-- Name: enrollments id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enrollments ALTER COLUMN id SET DEFAULT nextval('public.enrollments_id_seq'::regclass);


--
-- Name: exam_cards id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.exam_cards ALTER COLUMN id SET DEFAULT nextval('public.exam_cards_id_seq'::regclass);


--
-- Name: student_unit_registrations id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_unit_registrations ALTER COLUMN id SET DEFAULT nextval('public.student_unit_registrations_id_seq'::regclass);


--
-- Name: students id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.students ALTER COLUMN id SET DEFAULT nextval('public.students_id_seq'::regclass);


--
-- PostgreSQL database dump complete
--

