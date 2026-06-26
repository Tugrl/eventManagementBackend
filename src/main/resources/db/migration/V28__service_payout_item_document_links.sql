ALTER TABLE service_payout_items
    ADD COLUMN finance_document_id BIGINT,
    ADD COLUMN contact_movement_id BIGINT;

ALTER TABLE service_payout_items
    ADD CONSTRAINT fk_service_payout_items_finance_document
        FOREIGN KEY (finance_document_id) REFERENCES finance_documents(id);

ALTER TABLE service_payout_items
    ADD CONSTRAINT fk_service_payout_items_contact_movement
        FOREIGN KEY (contact_movement_id) REFERENCES contact_movements(id);

CREATE INDEX idx_service_payout_items_company_payout_document
    ON service_payout_items(company_id, payout_id, finance_document_id);
