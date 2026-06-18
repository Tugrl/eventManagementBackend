ALTER TABLE event_inventory_usages
    ADD COLUMN financial_transaction_id BIGINT REFERENCES financial_transactions(id);

CREATE INDEX idx_event_inventory_usages_financial_transaction
    ON event_inventory_usages(company_id, financial_transaction_id);
