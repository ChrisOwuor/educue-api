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
-- Name: application_documents; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.application_documents (
    id bigint NOT NULL,
    application_id bigint NOT NULL,
    document_type character varying(30) NOT NULL,
    s3_key character varying(500) NOT NULL,
    original_filename character varying(255),
    content_type character varying(100),
    file_size_bytes bigint,
    uploaded_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: application_documents_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.application_documents_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: application_documents_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.application_documents_id_seq OWNED BY public.application_documents.id;


--
-- Name: applications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.applications (
    id bigint NOT NULL,
    full_name character varying(150) NOT NULL,
    email character varying(150) NOT NULL,
    phone character varying(20) NOT NULL,
    national_id character varying(30),
    date_of_birth date,
    guardian_name character varying(150),
    guardian_phone character varying(20),
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    review_notes character varying(500),
    submitted_at timestamp without time zone DEFAULT now() NOT NULL,
    reviewed_at timestamp without time zone,
    version bigint DEFAULT 0 NOT NULL,
    approved_at timestamp without time zone,
    approved_by_user_id bigint,
    application_number character varying(40) NOT NULL,
    intake_course_id bigint NOT NULL
);


--
-- Name: applications_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.applications_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: applications_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.applications_id_seq OWNED BY public.applications.id;


--
-- Name: intake_courses; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.intake_courses (
    id bigint NOT NULL,
    intake_id bigint NOT NULL,
    course_id bigint NOT NULL
);


--
-- Name: intake_courses_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.intake_courses_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: intake_courses_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.intake_courses_id_seq OWNED BY public.intake_courses.id;


--
-- Name: intakes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.intakes (
    id bigint NOT NULL,
    uuid uuid NOT NULL,
    name character varying(100) NOT NULL,
    start_date date NOT NULL,
    application_deadline date NOT NULL,
    status character varying(20) DEFAULT 'UPCOMING'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    academic_year_id bigint NOT NULL
);


--
-- Name: intakes_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.intakes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: intakes_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.intakes_id_seq OWNED BY public.intakes.id;


--
-- Name: application_documents id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.application_documents ALTER COLUMN id SET DEFAULT nextval('public.application_documents_id_seq'::regclass);


--
-- Name: applications id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.applications ALTER COLUMN id SET DEFAULT nextval('public.applications_id_seq'::regclass);


--
-- Name: intake_courses id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.intake_courses ALTER COLUMN id SET DEFAULT nextval('public.intake_courses_id_seq'::regclass);


--
-- Name: intakes id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.intakes ALTER COLUMN id SET DEFAULT nextval('public.intakes_id_seq'::regclass);


--
-- PostgreSQL database dump complete
--

