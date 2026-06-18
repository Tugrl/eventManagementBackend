CREATE TABLE employees (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    full_name VARCHAR(255) NOT NULL,
    role_name VARCHAR(100),
    employee_type VARCHAR(20) NOT NULL,
    default_daily_rate NUMERIC(19,2) NOT NULL DEFAULT 0,
    service_point NUMERIC(19,2) NOT NULL DEFAULT 0,
    phone VARCHAR(100),
    notes TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE service_payouts (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    payout_date DATE NOT NULL,
    event_id BIGINT REFERENCES events(id),
    total_service_amount NUMERIC(19,2) NOT NULL,
    total_points NUMERIC(19,2) NOT NULL,
    amount_per_point NUMERIC(19,2) NOT NULL,
    notes TEXT,
    created_by_user_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE service_payout_items (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    payout_id BIGINT NOT NULL REFERENCES service_payouts(id),
    employee_id BIGINT NOT NULL REFERENCES employees(id),
    points NUMERIC(19,2) NOT NULL,
    payout_amount NUMERIC(19,2) NOT NULL
);

CREATE INDEX idx_employees_company_type ON employees(company_id, employee_type, is_active);
CREATE INDEX idx_service_payouts_company_date ON service_payouts(company_id, payout_date);
