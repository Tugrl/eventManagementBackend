ALTER TABLE financial_transactions
    DROP CONSTRAINT IF EXISTS financial_transactions_operation_source_check;

CREATE TABLE event_cost_realizations (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    event_id BIGINT NOT NULL REFERENCES events(id),
    event_cost_id BIGINT REFERENCES event_costs(id),
    cost_category_id BIGINT NOT NULL REFERENCES cost_categories(id),
    name VARCHAR(255) NOT NULL,
    estimated_unit_cost NUMERIC(19,2) NOT NULL DEFAULT 0,
    estimated_quantity NUMERIC(19,2) NOT NULL DEFAULT 0,
    estimated_total_cost NUMERIC(19,2) NOT NULL DEFAULT 0,
    actual_unit_cost NUMERIC(19,2) NOT NULL DEFAULT 0,
    actual_quantity NUMERIC(19,2) NOT NULL DEFAULT 0,
    actual_total_cost NUMERIC(19,2) NOT NULL DEFAULT 0,
    verification_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    payment_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    account_id BIGINT REFERENCES financial_accounts(id),
    payment_method_id BIGINT REFERENCES payment_methods(id),
    financial_category_id BIGINT REFERENCES financial_categories(id),
    contact_id BIGINT REFERENCES contacts(id),
    transaction_date DATE,
    financial_transaction_id BIGINT REFERENCES financial_transactions(id),
    contact_movement_id BIGINT REFERENCES contact_movements(id),
    notes TEXT,
    finalized BOOLEAN NOT NULL DEFAULT FALSE,
    verified_by_user_id BIGINT REFERENCES users(id),
    verified_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX uq_event_cost_realization_planned
    ON event_cost_realizations(company_id, event_id, event_cost_id)
    WHERE event_cost_id IS NOT NULL;

CREATE UNIQUE INDEX uq_event_closing_cost_finance
    ON financial_transactions(company_id, operation_source, reference_id)
    WHERE status = 'ACTIVE'
      AND operation_source = 'EVENT_CLOSING_COST'
      AND reference_id IS NOT NULL;

CREATE INDEX idx_event_cost_realizations_event
    ON event_cost_realizations(company_id, event_id);
