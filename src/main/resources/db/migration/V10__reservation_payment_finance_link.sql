ALTER TABLE reservation_payments
    ADD COLUMN financial_transaction_id BIGINT REFERENCES financial_transactions(id);

CREATE INDEX idx_reservation_payments_financial_transaction
    ON reservation_payments(company_id, financial_transaction_id);
