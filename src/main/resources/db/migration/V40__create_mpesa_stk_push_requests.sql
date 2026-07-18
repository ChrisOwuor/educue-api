CREATE TABLE mpesa_stk_push_requests (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    idempotency_key UUID NOT NULL UNIQUE,
    student_id BIGINT NOT NULL REFERENCES students(id),
    course_academic_period_id BIGINT REFERENCES course_academic_periods(id),
    account_reference VARCHAR(80) NOT NULL,
    amount NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    phone_hash VARCHAR(64) NOT NULL,
    phone_last_four VARCHAR(4) NOT NULL,
    merchant_request_id VARCHAR(100),
    checkout_request_id VARCHAR(100) UNIQUE,
    status VARCHAR(20) NOT NULL,
    response_code VARCHAR(30),
    response_description VARCHAR(255),
    customer_message VARCHAR(255),
    result_code INTEGER,
    result_description VARCHAR(500),
    mpesa_receipt_number VARCHAR(40),
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    callback_received_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_stk_status CHECK (status IN ('REQUESTED','PENDING','SUCCESS','FAILED'))
);

CREATE INDEX idx_stk_student_requested ON mpesa_stk_push_requests(student_id, requested_at DESC);
CREATE INDEX idx_stk_pending ON mpesa_stk_push_requests(status, requested_at);

