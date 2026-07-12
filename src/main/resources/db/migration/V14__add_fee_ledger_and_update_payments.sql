-- ==========================================
-- UPDATE PAYMENTS TABLE
-- ==========================================

-- 1. Rename 'reference' to 'gateway_reference'
ALTER TABLE payments RENAME COLUMN reference TO gateway_reference;
ALTER TABLE payments RENAME CONSTRAINT uk_payment_reference TO uk_payment_gateway_reference;

-- 2. Add new columns
ALTER TABLE payments ADD COLUMN applied_semester_id BIGINT;
ALTER TABLE payments ADD COLUMN receipt_number VARCHAR(50);
ALTER TABLE payments ADD COLUMN status VARCHAR(30) DEFAULT 'PENDING';

-- 3. Migrate 'confirmed' boolean to 'status' string
UPDATE payments SET status = 'VERIFIED' WHERE confirmed = true;
UPDATE payments SET status = 'PENDING' WHERE confirmed = false;

-- 4. Drop old 'confirmed' column and enforce NOT NULL on 'status'
ALTER TABLE payments DROP COLUMN confirmed;
ALTER TABLE payments ALTER COLUMN status SET NOT NULL;

-- 5. Add constraints
ALTER TABLE payments ADD CONSTRAINT fk_payment_semester FOREIGN KEY (applied_semester_id) REFERENCES semesters(id);
ALTER TABLE payments ADD CONSTRAINT uk_payment_receipt_number UNIQUE (receipt_number);


-- ==========================================
-- RENAME AND UPDATE LEDGER TABLE
-- ==========================================

-- 1. Rename table and index
ALTER TABLE student_ledger RENAME TO fee_ledger;
ALTER INDEX idx_ledger_student RENAME TO idx_fee_ledger_student;

-- 2. Add new columns
ALTER TABLE fee_ledger ADD COLUMN semester_id BIGINT;
ALTER TABLE fee_ledger ADD COLUMN transaction_type VARCHAR(30);
ALTER TABLE fee_ledger ADD COLUMN fee_structure_id BIGINT;
ALTER TABLE fee_ledger ADD COLUMN running_balance NUMERIC(12,2) DEFAULT 0;

-- 3. Add constraints
ALTER TABLE fee_ledger ADD CONSTRAINT fk_fee_ledger_semester FOREIGN KEY (semester_id) REFERENCES semesters(id);
ALTER TABLE fee_ledger ADD CONSTRAINT fk_fee_ledger_fee_structure FOREIGN KEY (fee_structure_id) REFERENCES fee_structures(id);
