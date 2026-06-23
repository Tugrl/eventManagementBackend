ALTER TABLE financial_transactions
    ADD COLUMN operation_source VARCHAR(50) NOT NULL DEFAULT 'MANUAL';

ALTER TABLE financial_transactions
    ADD COLUMN operation_context VARCHAR(50) NOT NULL DEFAULT 'COMPANY';

ALTER TABLE financial_transactions
    ADD COLUMN reference_id BIGINT NULL;
