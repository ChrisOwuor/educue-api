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

-- Standalone sequences used by FinanceDocumentNumberService. They are not
-- owned by a table column, so pg_dump's table-scoped snapshot does not emit them.
CREATE SEQUENCE public.finance_document_number_seq START WITH 100001;
CREATE SEQUENCE public.finance_receipt_number_seq START WITH 1;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: fee_ledger; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fee_ledger (
    id bigint NOT NULL,
    student_id bigint NOT NULL,
    payment_id bigint,
    debit numeric(12,2) DEFAULT 0 NOT NULL,
    credit numeric(12,2) DEFAULT 0 NOT NULL,
    description character varying(255) NOT NULL,
    created_by bigint,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    transaction_type character varying(30),
    fee_structure_id bigint,
    course_academic_period_id bigint,
    posting_date date NOT NULL,
    document_number character varying(40) NOT NULL,
    external_reference character varying(100),
    status character varying(20) DEFAULT 'POSTED'::character varying NOT NULL,
    reversal_of_id bigint,
    CONSTRAINT ck_fee_ledger_amount_positive CHECK (((debit >= (0)::numeric) AND (credit >= (0)::numeric))),
    CONSTRAINT ck_fee_ledger_one_sided CHECK ((((debit > (0)::numeric) AND (credit = (0)::numeric)) OR ((credit > (0)::numeric) AND (debit = (0)::numeric))))
);


--
-- Name: fee_structure_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fee_structure_items (
    id bigint NOT NULL,
    fee_structure_id bigint NOT NULL,
    name character varying(150) NOT NULL,
    amount numeric(12,2) NOT NULL
);


--
-- Name: fee_structure_items_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.fee_structure_items_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: fee_structure_items_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.fee_structure_items_id_seq OWNED BY public.fee_structure_items.id;


--
-- Name: fee_structures; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fee_structures (
    id bigint NOT NULL,
    active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    total_amount numeric(12,2) DEFAULT 0.00 NOT NULL,
    intake_course_id bigint NOT NULL,
    course_academic_period_id bigint NOT NULL
);


--
-- Name: fee_structures_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.fee_structures_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: fee_structures_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.fee_structures_id_seq OWNED BY public.fee_structures.id;


--
-- Name: mpesa_payment_events; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mpesa_payment_events (
    id bigint NOT NULL,
    transaction_id character varying(40) NOT NULL,
    transaction_type character varying(40),
    transaction_time timestamp without time zone NOT NULL,
    amount numeric(12,2) NOT NULL,
    business_short_code character varying(20) NOT NULL,
    account_reference character varying(80) NOT NULL,
    invoice_number character varying(80),
    third_party_transaction_id character varying(80),
    phone_hash character varying(64),
    phone_last_four character varying(4),
    status character varying(20) DEFAULT 'RECEIVED'::character varying NOT NULL,
    attempt_count integer DEFAULT 0 NOT NULL,
    next_attempt_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    processing_started_at timestamp without time zone,
    processed_at timestamp without time zone,
    payment_id bigint,
    failure_code character varying(50),
    failure_detail character varying(500),
    received_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ck_mpesa_event_status CHECK (((status)::text = ANY ((ARRAY['RECEIVED'::character varying, 'PROCESSING'::character varying, 'PROCESSED'::character varying, 'RETRY'::character varying, 'REVIEW'::character varying, 'REJECTED'::character varying])::text[]))),
    CONSTRAINT mpesa_payment_events_amount_check CHECK ((amount > (0)::numeric))
);


--
-- Name: mpesa_payment_events_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.mpesa_payment_events_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: mpesa_payment_events_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.mpesa_payment_events_id_seq OWNED BY public.mpesa_payment_events.id;


--
-- Name: mpesa_stk_push_requests; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mpesa_stk_push_requests (
    id bigint NOT NULL,
    uuid uuid NOT NULL,
    idempotency_key uuid NOT NULL,
    student_id bigint NOT NULL,
    course_academic_period_id bigint,
    account_reference character varying(80) NOT NULL,
    amount numeric(12,2) NOT NULL,
    phone_hash character varying(64) NOT NULL,
    phone_last_four character varying(4) NOT NULL,
    merchant_request_id character varying(100),
    checkout_request_id character varying(100),
    status character varying(20) NOT NULL,
    response_code character varying(30),
    response_description character varying(255),
    customer_message character varying(255),
    result_code integer,
    result_description character varying(500),
    mpesa_receipt_number character varying(40),
    requested_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    callback_received_at timestamp without time zone,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ck_stk_status CHECK (((status)::text = ANY ((ARRAY['REQUESTED'::character varying, 'PENDING'::character varying, 'SUCCESS'::character varying, 'FAILED'::character varying])::text[]))),
    CONSTRAINT mpesa_stk_push_requests_amount_check CHECK ((amount > (0)::numeric))
);


--
-- Name: mpesa_stk_push_requests_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.mpesa_stk_push_requests_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: mpesa_stk_push_requests_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.mpesa_stk_push_requests_id_seq OWNED BY public.mpesa_stk_push_requests.id;


--
-- Name: payments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.payments (
    id bigint NOT NULL,
    student_id bigint NOT NULL,
    amount numeric(12,2) NOT NULL,
    gateway_reference character varying(100) NOT NULL,
    payment_method character varying(30) NOT NULL,
    paid_at timestamp without time zone NOT NULL,
    recorded_by bigint,
    remarks character varying(255),
    receipt_number character varying(50),
    status character varying(30) DEFAULT 'PENDING'::character varying NOT NULL,
    applied_course_academic_period_id bigint,
    payer_type character varying(30) DEFAULT 'STUDENT'::character varying NOT NULL,
    payer_name character varying(160),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: payments_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.payments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: payments_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.payments_id_seq OWNED BY public.payments.id;


--
-- Name: student_ledger_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.student_ledger_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: student_ledger_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.student_ledger_id_seq OWNED BY public.fee_ledger.id;


--
-- Name: fee_ledger id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fee_ledger ALTER COLUMN id SET DEFAULT nextval('public.student_ledger_id_seq'::regclass);


--
-- Name: fee_structure_items id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fee_structure_items ALTER COLUMN id SET DEFAULT nextval('public.fee_structure_items_id_seq'::regclass);


--
-- Name: fee_structures id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fee_structures ALTER COLUMN id SET DEFAULT nextval('public.fee_structures_id_seq'::regclass);


--
-- Name: mpesa_payment_events id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mpesa_payment_events ALTER COLUMN id SET DEFAULT nextval('public.mpesa_payment_events_id_seq'::regclass);


--
-- Name: mpesa_stk_push_requests id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mpesa_stk_push_requests ALTER COLUMN id SET DEFAULT nextval('public.mpesa_stk_push_requests_id_seq'::regclass);


--
-- Name: payments id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments ALTER COLUMN id SET DEFAULT nextval('public.payments_id_seq'::regclass);


--
-- PostgreSQL database dump complete
--
