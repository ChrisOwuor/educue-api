-- The immutable ledger is the sole source of truth for student balances.
DROP TABLE IF EXISTS student_finance_accounts;

-- Running balances are derived in posting order and must not be persisted,
-- otherwise backdated entries and concurrent writes can leave stale snapshots.
ALTER TABLE fee_ledger DROP COLUMN running_balance;

-- Enforce lifecycle billing idempotency at the database level as well.
CREATE UNIQUE INDEX uk_fee_ledger_student_fee_structure
    ON fee_ledger (student_id, fee_structure_id)
    WHERE fee_structure_id IS NOT NULL;
