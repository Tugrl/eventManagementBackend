ALTER TABLE service_payouts
    ADD COLUMN financial_transaction_id BIGINT REFERENCES financial_transactions(id);

CREATE INDEX idx_service_payouts_financial_transaction
    ON service_payouts(company_id, financial_transaction_id);
