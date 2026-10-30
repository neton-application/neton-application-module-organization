-- organization: 1.0.0-beta fresh-install schema. Not an upgrade path.
-- Apply through the migration runner; never edit after this baseline is released.
SET search_path = public;
--
--


SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
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
-- Name: organization; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.organization (
    id bigint NOT NULL,
    name character varying(128) NOT NULL,
    logo character varying(255) DEFAULT ''::character varying NOT NULL,
    code character varying(64) DEFAULT ''::character varying NOT NULL,
    invite_code character varying(32) DEFAULT ''::character varying NOT NULL,
    owner_account_id bigint NOT NULL,
    status smallint DEFAULT 1 NOT NULL,
    remark character varying(512) DEFAULT ''::character varying NOT NULL,
    deleted smallint DEFAULT 0 NOT NULL,
    created_at bigint,
    updated_at bigint
);


--
-- Name: organization_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.organization_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: organization_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.organization_id_seq OWNED BY public.organization.id;


--
-- Name: organization_member; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.organization_member (
    id bigint NOT NULL,
    org_id bigint NOT NULL,
    account_id bigint NOT NULL,
    role smallint DEFAULT 3 NOT NULL,
    status smallint DEFAULT 1 NOT NULL,
    source smallint DEFAULT 0 NOT NULL,
    joined_at bigint,
    deleted smallint DEFAULT 0 NOT NULL,
    created_at bigint,
    updated_at bigint
);


--
-- Name: organization_member_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.organization_member_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: organization_member_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.organization_member_id_seq OWNED BY public.organization_member.id;


--
-- Name: organization id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.organization ALTER COLUMN id SET DEFAULT nextval('public.organization_id_seq'::regclass);


--
-- Name: organization_member id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.organization_member ALTER COLUMN id SET DEFAULT nextval('public.organization_member_id_seq'::regclass);


--
-- Name: organization_member organization_member_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.organization_member
    ADD CONSTRAINT organization_member_pkey PRIMARY KEY (id);


--
-- Name: organization organization_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.organization
    ADD CONSTRAINT organization_pkey PRIMARY KEY (id);


--
-- Name: idx_org_member_account; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_org_member_account ON public.organization_member USING btree (account_id);


--
-- Name: idx_org_member_org; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_org_member_org ON public.organization_member USING btree (org_id);


--
-- Name: idx_org_member_org_account; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_org_member_org_account ON public.organization_member USING btree (org_id, account_id) WHERE (deleted = 0);


--
-- Name: idx_organization_code; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_organization_code ON public.organization USING btree (code) WHERE ((deleted = 0) AND ((code)::text <> ''::text));


--
-- Name: idx_organization_invite_code; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_organization_invite_code ON public.organization USING btree (invite_code) WHERE ((deleted = 0) AND ((invite_code)::text <> ''::text));


--
-- Name: idx_organization_owner; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_organization_owner ON public.organization USING btree (owner_account_id);


--
--


SET search_path = public;
