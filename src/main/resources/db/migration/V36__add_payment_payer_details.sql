ALTER TABLE payments
    ADD COLUMN payer_type VARCHAR(30) NOT NULL DEFAULT 'STUDENT',
    ADD COLUMN payer_name VARCHAR(160);

UPDATE payments
SET payer_name = 'Student'
WHERE payer_name IS NULL;

CREATE INDEX idx_payments_payer_type ON payments (payer_type);
