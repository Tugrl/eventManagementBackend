ALTER TABLE contact_movements
    ADD COLUMN document_id BIGINT,
    ADD COLUMN settlement_id BIGINT,
    ADD COLUMN remaining_document_amount NUMERIC(19,2);

ALTER TABLE contact_movements
    ADD CONSTRAINT fk_contact_movements_document
        FOREIGN KEY (document_id) REFERENCES finance_documents(id);

ALTER TABLE contact_movements
    ADD CONSTRAINT fk_contact_movements_settlement
        FOREIGN KEY (settlement_id) REFERENCES finance_document_settlements(id);

CREATE INDEX idx_contact_movements_company_contact_document
    ON contact_movements(company_id, contact_id, document_id);
