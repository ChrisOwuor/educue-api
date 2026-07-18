CREATE SEQUENCE finance_document_number_seq START WITH 100001;

ALTER TABLE fee_ledger
    ADD COLUMN posting_date DATE,
    ADD COLUMN document_number VARCHAR(40),
    ADD COLUMN external_reference VARCHAR(100),
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'POSTED',
    ADD COLUMN reversal_of_id BIGINT;

UPDATE fee_ledger
SET posting_date = created_at::date,
    document_number = 'LEGACY-' || LPAD(id::text, 10, '0');

ALTER TABLE fee_ledger
    ALTER COLUMN posting_date SET NOT NULL,
    ALTER COLUMN document_number SET NOT NULL,
    ADD CONSTRAINT uk_fee_ledger_document_number UNIQUE (document_number),
    ADD CONSTRAINT fk_fee_ledger_reversal_of FOREIGN KEY (reversal_of_id) REFERENCES fee_ledger(id),
    ADD CONSTRAINT ck_fee_ledger_one_sided CHECK (
        (debit > 0 AND credit = 0) OR (credit > 0 AND debit = 0)
    ),
    ADD CONSTRAINT ck_fee_ledger_amount_positive CHECK (debit >= 0 AND credit >= 0);

CREATE UNIQUE INDEX uk_fee_ledger_one_reversal
    ON fee_ledger (reversal_of_id) WHERE reversal_of_id IS NOT NULL;
CREATE INDEX idx_fee_ledger_student_posting
    ON fee_ledger (student_id, posting_date, id);
CREATE INDEX idx_fee_ledger_external_reference
    ON fee_ledger (external_reference) WHERE external_reference IS NOT NULL;

CREATE TABLE student_finance_accounts (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL UNIQUE REFERENCES students(id),
    balance NUMERIC(14,2) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO student_finance_accounts (student_id, balance)
SELECT s.id, COALESCE(SUM(fl.debit - fl.credit), 0)
FROM students s
LEFT JOIN fee_ledger fl ON fl.student_id = s.id
GROUP BY s.id;
