ALTER TABLE event_costs
    ADD COLUMN financial_transaction_id BIGINT REFERENCES financial_transactions(id);

CREATE INDEX idx_event_costs_financial_transaction
    ON event_costs(company_id, financial_transaction_id);
