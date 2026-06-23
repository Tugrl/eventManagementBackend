CREATE TABLE finance_documents (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    document_type VARCHAR(20) NOT NULL,
    document_scope VARCHAR(30) NOT NULL DEFAULT 'COMPANY',
    category_id BIGINT NOT NULL REFERENCES financial_categories(id),
    contact_id BIGINT REFERENCES contacts(id),
    event_id BIGINT REFERENCES events(id),
    issue_date DATE NOT NULL,
    due_date DATE,
    amount NUMERIC(19,2) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    operation_group VARCHAR(40) NOT NULL DEFAULT 'OTHER',
    operation_context VARCHAR(30) NOT NULL DEFAULT 'COMPANY',
    created_by_user_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE finance_document_settlements (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    document_id BIGINT NOT NULL REFERENCES finance_documents(id),
    financial_transaction_id BIGINT REFERENCES financial_transactions(id),
    settlement_date DATE NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    account_id BIGINT NOT NULL REFERENCES financial_accounts(id),
    payment_method_id BIGINT REFERENCES payment_methods(id),
    notes TEXT,
    created_by_user_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_finance_documents_company_issue_date
    ON finance_documents(company_id, issue_date, status);

CREATE INDEX idx_finance_documents_company_contact
    ON finance_documents(company_id, contact_id);

CREATE INDEX idx_finance_document_settlements_document
    ON finance_document_settlements(company_id, document_id);
