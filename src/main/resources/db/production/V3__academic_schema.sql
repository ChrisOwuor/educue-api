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
-- Name: academic_periods; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.academic_periods (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    code character varying(20) NOT NULL,
    name character varying(100) NOT NULL,
    period_type character varying(20) NOT NULL,
    year_number integer NOT NULL,
    period_number integer NOT NULL,
    sequence_number integer NOT NULL,
    active boolean DEFAULT true NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_academic_period_period_number CHECK ((period_number > 0)),
    CONSTRAINT chk_academic_period_sequence_number CHECK ((sequence_number > 0)),
    CONSTRAINT chk_academic_period_type CHECK (((period_type)::text = ANY ((ARRAY['SEMESTER'::character varying, 'TERM'::character varying, 'TRIMESTER'::character varying, 'MODULE'::character varying, 'QUARTER'::character varying, 'BLOCK'::character varying])::text[]))),
    CONSTRAINT chk_academic_period_year_number CHECK ((year_number > 0))
);


--
-- Name: academic_periods_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.academic_periods_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: academic_periods_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.academic_periods_id_seq OWNED BY public.academic_periods.id;


--
-- Name: course_academic_periods; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.course_academic_periods (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    course_id bigint NOT NULL,
    academic_period_id bigint NOT NULL,
    "position" integer NOT NULL,
    next_period_id bigint,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_course_period_position CHECK (("position" > 0))
);


--
-- Name: course_academic_periods_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.course_academic_periods_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: course_academic_periods_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.course_academic_periods_id_seq OWNED BY public.course_academic_periods.id;


--
-- Name: course_unit_placements; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.course_unit_placements (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    course_id bigint,
    unit_id bigint NOT NULL,
    academic_period_id bigint,
    unit_type character varying(20) NOT NULL,
    effective_from_intake_year integer NOT NULL,
    effective_to_intake_year integer,
    active boolean DEFAULT true NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    course_academic_period_id bigint,
    CONSTRAINT chk_course_unit_placement_from_year CHECK ((effective_from_intake_year > 0)),
    CONSTRAINT chk_course_unit_placement_type CHECK (((unit_type)::text = ANY ((ARRAY['CORE'::character varying, 'ELECTIVE'::character varying, 'OPTIONAL'::character varying])::text[]))),
    CONSTRAINT chk_course_unit_placement_year_range CHECK (((effective_to_intake_year IS NULL) OR (effective_to_intake_year >= effective_from_intake_year)))
);


--
-- Name: course_unit_placements_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.course_unit_placements_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: course_unit_placements_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.course_unit_placements_id_seq OWNED BY public.course_unit_placements.id;


--
-- Name: courses; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.courses (
    id bigint NOT NULL,
    uuid uuid NOT NULL,
    department_id bigint NOT NULL,
    code character varying(20) NOT NULL,
    name character varying(150) NOT NULL,
    duration_value integer,
    active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    qualification_type character varying(50) NOT NULL,
    study_mode character varying(50) NOT NULL,
    total_credits integer,
    version bigint DEFAULT 0 NOT NULL,
    award_title character varying(150),
    duration_unit character varying(50)
);


--
-- Name: courses_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.courses_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: courses_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.courses_id_seq OWNED BY public.courses.id;


--
-- Name: lecturer_unit_assignments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.lecturer_unit_assignments (
    id bigint NOT NULL,
    uuid uuid DEFAULT gen_random_uuid() NOT NULL,
    course_unit_placement_id bigint NOT NULL,
    lecturer_id bigint NOT NULL,
    effective_from_academic_year_id bigint NOT NULL,
    effective_to_academic_year_id bigint,
    start_year integer NOT NULL,
    assigned_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    assigned_by bigint,
    enabled boolean DEFAULT true NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_lecturer_assignment_start_year CHECK ((start_year > 0))
);


--
-- Name: lecturer_unit_assignments_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.lecturer_unit_assignments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: lecturer_unit_assignments_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.lecturer_unit_assignments_id_seq OWNED BY public.lecturer_unit_assignments.id;


--
-- Name: units; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.units (
    id bigint NOT NULL,
    department_id bigint NOT NULL,
    code character varying(30) NOT NULL,
    name character varying(150) NOT NULL,
    credit_hours integer,
    description character varying(255),
    active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    uuid uuid NOT NULL,
    version bigint NOT NULL,
    updated_at timestamp without time zone NOT NULL
);


--
-- Name: units_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.units_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: units_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.units_id_seq OWNED BY public.units.id;


--
-- Name: academic_periods id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.academic_periods ALTER COLUMN id SET DEFAULT nextval('public.academic_periods_id_seq'::regclass);


--
-- Name: course_academic_periods id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.course_academic_periods ALTER COLUMN id SET DEFAULT nextval('public.course_academic_periods_id_seq'::regclass);


--
-- Name: course_unit_placements id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.course_unit_placements ALTER COLUMN id SET DEFAULT nextval('public.course_unit_placements_id_seq'::regclass);


--
-- Name: courses id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.courses ALTER COLUMN id SET DEFAULT nextval('public.courses_id_seq'::regclass);


--
-- Name: lecturer_unit_assignments id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lecturer_unit_assignments ALTER COLUMN id SET DEFAULT nextval('public.lecturer_unit_assignments_id_seq'::regclass);


--
-- Name: units id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.units ALTER COLUMN id SET DEFAULT nextval('public.units_id_seq'::regclass);


--
-- PostgreSQL database dump complete
--

