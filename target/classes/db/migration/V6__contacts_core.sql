CREATE TABLE contacts (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    name VARCHAR(255) NOT NULL,
    contact_type VARCHAR(30) NOT NULL,
    phone VARCHAR(100),
    email VARCHAR(255),
    tax_number VARCHAR(100),
    notes TEXT,
    opening_balance NUMERIC(19,2) NOT NULL DEFAULT 0,
    current_balance NUMERIC(19,2) NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE contact_movements (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    contact_id BIGINT NOT NULL REFERENCES contacts(id),
    financial_transaction_id BIGINT REFERENCES financial_transactions(id),
    movement_date DATE NOT NULL,
    movement_type VARCHAR(30) NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    balance_delta NUMERIC(19,2) NOT NULL,
    description TEXT,
    created_by_user_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL
);

ALTER TABLE financial_transactions
    ADD COLUMN contact_id BIGINT REFERENCES contacts(id);

CREATE INDEX idx_contacts_company_type ON contacts(company_id, contact_type, is_active);
CREATE INDEX idx_contact_movements_company_contact ON contact_movements(company_id, contact_id, movement_date);
CREATE INDEX idx_financial_transactions_contact ON financial_transactions(company_id, contact_id);
