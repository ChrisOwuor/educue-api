-- paid_at is the business-effective payment time entered by finance.
-- created_at is the immutable system audit time at which EduCue recorded it.
CREATE SEQUENCE IF NOT EXISTS finance_receipt_number_seq START WITH 1;

ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;

UPDATE payments
SET created_at = paid_at
WHERE created_at IS NULL;

ALTER TABLE payments
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN created_at SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_fee_ledger_statement_order
    ON fee_ledger (student_id, posting_date, created_at, id);

CREATE INDEX IF NOT EXISTS idx_payments_business_order
    ON payments (paid_at, created_at, id);
