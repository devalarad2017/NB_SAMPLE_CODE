-- V1__create_tables.sql
-- Run against PostgreSQL before starting the application.

-- partner_config: one row per external partner
CREATE TABLE partner_config (
    partner_code        VARCHAR(20)  PRIMARY KEY,
    reverse_feed_url    VARCHAR(500) NOT NULL,
    auth_type           VARCHAR(20),             -- BEARER | API_KEY | BASIC
    auth_credential     TEXT,                    -- store encrypted
    timeout_ms          INTEGER      DEFAULT 30000,
    is_active           BOOLEAN      DEFAULT TRUE
);

-- partner_field_mapping: one row per field per API per partner
-- Same source_param can appear multiple times (once per target_api that uses it)
-- default_value: used when partner sends blank/null for this param
CREATE TABLE partner_field_mapping (
    id                  BIGSERIAL    PRIMARY KEY,
    partner_code        VARCHAR(20)  NOT NULL REFERENCES partner_config(partner_code),
    source_param        VARCHAR(20)  NOT NULL,       -- stringval1..stringvalN
    target_api          VARCHAR(50)  NOT NULL,       -- must match ApiName enum value
    target_field        VARCHAR(100) NOT NULL,       -- field name in downstream API request
    data_type           VARCHAR(20)  NOT NULL,       -- STRING|DATE|DECIMAL|INTEGER|BOOLEAN
    date_format         VARCHAR(30),                 -- dd/MM/yyyy — only for DATE type
    is_mandatory        BOOLEAN      DEFAULT FALSE,
    default_value       VARCHAR(200),               -- fallback if partner sends blank
    transformation      VARCHAR(50),                -- TRIM | UPPERCASE | LOWERCASE | NONE
    validation_regex    VARCHAR(500),
    is_active           BOOLEAN      DEFAULT TRUE,
    created_at          TIMESTAMP    DEFAULT NOW(),
    updated_at          TIMESTAMP    DEFAULT NOW(),
    CONSTRAINT uq_partner_param_api UNIQUE (partner_code, source_param, target_api)
);
CREATE INDEX idx_pfm_partner_api ON partner_field_mapping(partner_code, target_api, is_active);

-- raw_request: immutable record of what partner sent us
CREATE TABLE raw_request (
    id                  BIGSERIAL    PRIMARY KEY,
    correlation_id      VARCHAR(36)  UNIQUE NOT NULL,
    partner_code        VARCHAR(20)  NOT NULL,
    raw_payload         TEXT         NOT NULL,
    received_at         TIMESTAMP    DEFAULT NOW()
);
CREATE INDEX idx_raw_correlation ON raw_request(correlation_id);

-- journey_execution: overall status per request
-- NOTE: no last_completed_stage column — resume is driven by journey_stage_log
CREATE TABLE journey_execution (
    id                  BIGSERIAL    PRIMARY KEY,
    correlation_id      VARCHAR(36)  UNIQUE NOT NULL,
    partner_code        VARCHAR(20),
    raw_request_id      BIGINT       REFERENCES raw_request(id),
    overall_status      VARCHAR(20)  DEFAULT 'IN_PROGRESS',  -- IN_PROGRESS|COMPLETED|FAILED
    application_number  VARCHAR(100),
    created_at          TIMESTAMP    DEFAULT NOW(),
    updated_at          TIMESTAMP    DEFAULT NOW()
);
CREATE INDEX idx_journey_correlation ON journey_execution(correlation_id);
CREATE INDEX idx_journey_status      ON journey_execution(overall_status);

-- journey_stage_log: immutable audit log — never updated, only inserted
-- ONE ROW per API call attempt. attempt_number increments per retry.
-- THIS IS THE SOURCE OF TRUTH for retry resume:
--   JourneyOrchestrator queries: SELECT api_name WHERE correlation_id=? AND status='SUCCESS'
--   Any api_name in that result is SKIPPED on retry.
CREATE TABLE journey_stage_log (
    id                  BIGSERIAL    PRIMARY KEY,
    correlation_id      VARCHAR(36)  NOT NULL,
    stage_name          VARCHAR(50),
    api_name            VARCHAR(50),
    attempt_number      INTEGER      DEFAULT 1,
    request_payload     TEXT,
    response_payload    TEXT,
    status              VARCHAR(20),        -- SUCCESS | FAILED | RETRYING
    error_code          VARCHAR(50),
    error_message       TEXT,
    duration_ms         BIGINT,
    executed_at         TIMESTAMP    DEFAULT NOW()
);
CREATE INDEX idx_stage_log_correlation ON journey_stage_log(correlation_id);
CREATE INDEX idx_stage_log_api_status  ON journey_stage_log(correlation_id, api_name, status);

-- Sample data: PARTNER_A config + example mapping rows
INSERT INTO partner_config(partner_code, reverse_feed_url, auth_type, auth_credential, is_active)
VALUES ('PARTNER_A', 'https://partner-a.example.com/reverse-feed', 'BEARER', 'REPLACE_TOKEN', TRUE);

INSERT INTO partner_field_mapping
    (partner_code, source_param, target_api, target_field, data_type, is_mandatory, default_value, transformation)
VALUES
    ('PARTNER_A','stringval1', 'ELIGIBILITY_API', 'firstName',    'STRING',  TRUE,  NULL,    'TRIM'),
    ('PARTNER_A','stringval2', 'ELIGIBILITY_API', 'lastName',     'STRING',  TRUE,  NULL,    'TRIM'),
    ('PARTNER_A','stringval3', 'ELIGIBILITY_API', 'dateOfBirth',  'DATE',    TRUE,  NULL,    NULL),
    ('PARTNER_A','stringval4', 'ELIGIBILITY_API', 'gender',       'STRING',  FALSE, 'MALE',  'UPPERCASE'),
    ('PARTNER_A','stringval45','EDC_API',          'panNumber',    'STRING',  TRUE,  NULL,    'UPPERCASE'),
    ('PARTNER_A','stringval1', 'MEDICAL_API',      'firstName',    'STRING',  TRUE,  NULL,    'TRIM'),
    ('PARTNER_A','stringval3', 'MEDICAL_API',      'dateOfBirth',  'DATE',    TRUE,  NULL,    NULL),
    ('PARTNER_A','stringval50','MEDICAL_API',      'sumAssured',   'DECIMAL', TRUE,  NULL,    NULL);
-- Add remaining 590+ rows from BA team Excel import
