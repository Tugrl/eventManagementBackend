CREATE TABLE financial_accounts (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    name VARCHAR(255) NOT NULL,
    account_type VARCHAR(30) NOT NULL,
    opening_balance NUMERIC(19,2) NOT NULL DEFAULT 0,
    current_balance NUMERIC(19,2) NOT NULL DEFAULT 0,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE payment_methods (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    name VARCHAR(255) NOT NULL,
    method_type VARCHAR(30) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE financial_categories (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    parent_id BIGINT REFERENCES financial_categories(id),
    name VARCHAR(255) NOT NULL,
    category_type VARCHAR(20) NOT NULL,
    scope VARCHAR(30) NOT NULL,
    description TEXT,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE revenue_channels (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    name VARCHAR(255) NOT NULL,
    channel_type VARCHAR(40) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE financial_transactions (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    event_id BIGINT REFERENCES events(id),
    account_id BIGINT NOT NULL REFERENCES financial_accounts(id),
    payment_method_id BIGINT REFERENCES payment_methods(id),
    category_id BIGINT NOT NULL REFERENCES financial_categories(id),
    revenue_channel_id BIGINT REFERENCES revenue_channels(id),
    transaction_type VARCHAR(20) NOT NULL,
    transaction_date DATE NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    guest_count INTEGER,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    void_reason TEXT,
    created_by_user_id BIGINT REFERENCES users(id),
    voided_by_user_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    voided_at TIMESTAMP
);

CREATE TABLE daily_cash_reports (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    report_date DATE NOT NULL,
    opening_cash NUMERIC(19,2) NOT NULL DEFAULT 0,
    management_cash_in NUMERIC(19,2) NOT NULL DEFAULT 0,
    cash_income NUMERIC(19,2) NOT NULL DEFAULT 0,
    card_income NUMERIC(19,2) NOT NULL DEFAULT 0,
    bank_income NUMERIC(19,2) NOT NULL DEFAULT 0,
    current_account_income NUMERIC(19,2) NOT NULL DEFAULT 0,
    cash_expense NUMERIC(19,2) NOT NULL DEFAULT 0,
    total_income NUMERIC(19,2) NOT NULL DEFAULT 0,
    total_expense NUMERIC(19,2) NOT NULL DEFAULT 0,
    expected_cash NUMERIC(19,2) NOT NULL DEFAULT 0,
    actual_cash NUMERIC(19,2) NOT NULL DEFAULT 0,
    cash_difference NUMERIC(19,2) NOT NULL DEFAULT 0,
    management_cash_out NUMERIC(19,2) NOT NULL DEFAULT 0,
    notes TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'CLOSED',
    closed_by_user_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_daily_cash_reports_company_date UNIQUE (company_id, report_date)
);

CREATE INDEX idx_financial_accounts_company ON financial_accounts(company_id, is_active);
CREATE INDEX idx_payment_methods_company ON payment_methods(company_id, is_active);
CREATE INDEX idx_financial_categories_company ON financial_categories(company_id, category_type, is_active);
CREATE INDEX idx_revenue_channels_company ON revenue_channels(company_id, is_active);
CREATE INDEX idx_financial_transactions_company_date ON financial_transactions(company_id, transaction_date);
CREATE INDEX idx_financial_transactions_event ON financial_transactions(company_id, event_id);

INSERT INTO financial_accounts (company_id, name, account_type, opening_balance, current_balance, is_default, is_active, created_at, updated_at)
VALUES
(1, 'Ana Kasa', 'CASH', 0, 0, TRUE, TRUE, NOW(), NOW()),
(1, 'POS / Kredi Kartı', 'CARD', 0, 0, TRUE, TRUE, NOW(), NOW()),
(1, 'Banka', 'BANK', 0, 0, TRUE, TRUE, NOW(), NOW());

INSERT INTO payment_methods (company_id, name, method_type, is_default, is_active, created_at, updated_at)
VALUES
(1, 'Nakit', 'CASH', TRUE, TRUE, NOW(), NOW()),
(1, 'Kredi Kartı', 'CARD', TRUE, TRUE, NOW(), NOW()),
(1, 'Banka', 'BANK', TRUE, TRUE, NOW(), NOW()),
(1, 'Cari', 'CURRENT_ACCOUNT', TRUE, TRUE, NOW(), NOW());

INSERT INTO financial_categories (company_id, name, category_type, scope, description, is_default, is_active, created_at, updated_at)
VALUES
(1, 'Genel Gelir', 'INCOME', 'COMPANY', NULL, TRUE, TRUE, NOW(), NOW()),
(1, 'Genel Gider', 'EXPENSE', 'COMPANY', NULL, TRUE, TRUE, NOW(), NOW()),
(1, 'Etkinlik Geliri', 'INCOME', 'EVENT', NULL, TRUE, TRUE, NOW(), NOW()),
(1, 'Etkinlik Gideri', 'EXPENSE', 'EVENT', NULL, TRUE, TRUE, NOW(), NOW()),
(1, 'Personel Gideri', 'EXPENSE', 'BOTH', NULL, TRUE, TRUE, NOW(), NOW()),
(1, 'Yemek Gideri', 'EXPENSE', 'EVENT', NULL, TRUE, TRUE, NOW(), NOW()),
(1, 'İçecek Gideri', 'EXPENSE', 'EVENT', NULL, TRUE, TRUE, NOW(), NOW()),
(1, 'Sanatçı Gideri', 'EXPENSE', 'EVENT', NULL, TRUE, TRUE, NOW(), NOW()),
(1, 'Teknik Gider', 'EXPENSE', 'EVENT', NULL, TRUE, TRUE, NOW(), NOW()),
(1, 'Diğer', 'EXPENSE', 'BOTH', NULL, TRUE, TRUE, NOW(), NOW());

INSERT INTO revenue_channels (company_id, name, channel_type, sort_order, is_default, is_active, created_at, updated_at)
VALUES
(1, 'Direkt Satış', 'DIRECT_SALE', 10, TRUE, TRUE, NOW(), NOW()),
(1, 'Bilet Platformu', 'TICKET_PLATFORM', 20, TRUE, TRUE, NOW(), NOW()),
(1, 'Cari Hesap', 'CURRENT_ACCOUNT', 30, TRUE, TRUE, NOW(), NOW()),
(1, 'Otopark', 'PARKING', 40, TRUE, TRUE, NOW(), NOW()),
(1, 'Servis Ücreti', 'SERVICE_FEE', 50, TRUE, TRUE, NOW(), NOW()),
(1, 'Ekstra Satış', 'EXTRA_SALE', 60, TRUE, TRUE, NOW(), NOW()),
(1, 'Diğer', 'OTHER', 70, TRUE, TRUE, NOW(), NOW());
