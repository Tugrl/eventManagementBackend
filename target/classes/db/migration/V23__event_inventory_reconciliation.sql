ALTER TABLE event_cost_realizations
    ADD COLUMN inventory_item_id BIGINT REFERENCES inventory_items(id),
    ADD COLUMN inventory_reconciliation_id BIGINT;

CREATE TABLE event_inventory_reconciliations (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    event_id BIGINT NOT NULL REFERENCES events(id),
    inventory_item_id BIGINT NOT NULL REFERENCES inventory_items(id),
    consumption_plan_id BIGINT REFERENCES event_consumption_plans(id),
    item_name VARCHAR(255) NOT NULL,
    stock_unit VARCHAR(30) NOT NULL,
    input_unit VARCHAR(30) NOT NULL,
    planned_quantity NUMERIC(19,4) NOT NULL DEFAULT 0,
    actual_consumption_input NUMERIC(19,4) NOT NULL DEFAULT 0,
    manual_waste_input NUMERIC(19,4) NOT NULL DEFAULT 0,
    actual_consumption_stock NUMERIC(19,4) NOT NULL DEFAULT 0,
    total_waste_stock NUMERIC(19,4) NOT NULL DEFAULT 0,
    automatic_waste_ml NUMERIC(19,4) NOT NULL DEFAULT 0,
    deducted_quantity NUMERIC(19,4) NOT NULL DEFAULT 0,
    previously_processed_quantity NUMERIC(19,4) NOT NULL DEFAULT 0,
    unit_cost NUMERIC(19,2) NOT NULL DEFAULT 0,
    total_cost NUMERIC(19,2) NOT NULL DEFAULT 0,
    actual_guest_count INTEGER NOT NULL DEFAULT 1,
    usage_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    finalized BOOLEAN NOT NULL DEFAULT FALSE,
    inventory_movement_id BIGINT REFERENCES inventory_movements(id),
    notes TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_event_inventory_reconciliation_item UNIQUE(company_id, event_id, inventory_item_id)
);

ALTER TABLE event_cost_realizations
    ADD CONSTRAINT fk_cost_realization_inventory_reconciliation
    FOREIGN KEY (inventory_reconciliation_id) REFERENCES event_inventory_reconciliations(id);

CREATE INDEX idx_event_inventory_reconciliation_event
    ON event_inventory_reconciliations(company_id, event_id);
