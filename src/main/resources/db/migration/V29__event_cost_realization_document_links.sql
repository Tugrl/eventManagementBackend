ALTER TABLE event_cost_realizations
    ADD COLUMN finance_document_id BIGINT,
    ADD COLUMN settlement_id BIGINT;

ALTER TABLE event_cost_realizations
    ADD CONSTRAINT fk_event_cost_realizations_finance_document
        FOREIGN KEY (finance_document_id) REFERENCES finance_documents(id);

ALTER TABLE event_cost_realizations
    ADD CONSTRAINT fk_event_cost_realizations_settlement
        FOREIGN KEY (settlement_id) REFERENCES finance_document_settlements(id);

CREATE INDEX idx_event_cost_realizations_finance_document
    ON event_cost_realizations(company_id, event_id, finance_document_id);
