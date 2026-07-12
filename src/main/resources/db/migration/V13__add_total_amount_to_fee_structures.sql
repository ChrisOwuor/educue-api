-- V13__add_total_amount_to_fee_structures.sql

ALTER TABLE fee_structures
    ADD COLUMN total_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00;
