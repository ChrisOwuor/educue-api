CREATE TABLE mpesa_payment_events (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(40) NOT NULL,
    transaction_type VARCHAR(40),
    transaction_time TIMESTAMP NOT NULL,
    amount NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    business_short_code VARCHAR(20) NOT NULL,
    account_reference VARCHAR(80) NOT NULL,
    invoice_number VARCHAR(80),
    third_party_transaction_id VARCHAR(80),
    phone_hash VARCHAR(64),
    phone_last_four VARCHAR(4),
    status VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processing_started_at TIMESTAMP,
    processed_at TIMESTAMP,
    payment_id BIGINT REFERENCES payments(id),
    failure_code VARCHAR(50),
    failure_detail VARCHAR(500),
    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_mpesa_event_transaction UNIQUE (transaction_id),
    CONSTRAINT ck_mpesa_event_status CHECK (status IN ('RECEIVED','PROCESSING','PROCESSED','RETRY','REVIEW','REJECTED'))
);

CREATE INDEX idx_mpesa_event_work_queue
    ON mpesa_payment_events (status, next_attempt_at, received_at);
CREATE INDEX idx_mpesa_event_account_reference
    ON mpesa_payment_events (account_reference);

