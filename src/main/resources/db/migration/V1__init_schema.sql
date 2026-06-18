CREATE TABLE companies (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    tax_number VARCHAR(100),
    email VARCHAR(255),
    phone VARCHAR(100),
    address TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    event_date DATE NOT NULL,
    start_time TIME,
    end_time TIME,
    venue_name VARCHAR(255) NOT NULL,
    max_capacity INTEGER NOT NULL,
    expected_guest_count INTEGER NOT NULL,
    target_profit_margin NUMERIC(19,2) NOT NULL,
    suggested_ticket_price NUMERIC(19,2),
    final_ticket_price NUMERIC(19,2),
    deposit_required BOOLEAN NOT NULL,
    minimum_deposit_amount NUMERIC(19,2),
    status VARCHAR(20) NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE reservations (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    event_id BIGINT NOT NULL REFERENCES events(id),
    customer_name VARCHAR(255) NOT NULL,
    customer_phone VARCHAR(100) NOT NULL,
    customer_email VARCHAR(255),
    guest_count INTEGER NOT NULL,
    table_number VARCHAR(100),
    reservation_code VARCHAR(50) NOT NULL UNIQUE,
    deposit_amount NUMERIC(19,2) NOT NULL DEFAULT 0,
    deposit_status VARCHAR(30) NOT NULL,
    reservation_status VARCHAR(30) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE reservation_payments (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    reservation_id BIGINT NOT NULL REFERENCES reservations(id),
    event_id BIGINT NOT NULL REFERENCES events(id),
    amount NUMERIC(19,2) NOT NULL,
    payment_date DATE NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    bank_reference VARCHAR(255),
    description TEXT,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE bank_transactions (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    transaction_date DATE NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    sender_name VARCHAR(255),
    description TEXT,
    iban VARCHAR(100),
    reference_code VARCHAR(100),
    matched_reservation_id BIGINT,
    is_matched BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE cost_categories (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE event_costs (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    event_id BIGINT NOT NULL REFERENCES events(id),
    cost_category_id BIGINT NOT NULL REFERENCES cost_categories(id),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    calculation_type VARCHAR(30) NOT NULL,
    unit_cost NUMERIC(19,2) NOT NULL,
    quantity NUMERIC(19,2) NOT NULL,
    total_cost NUMERIC(19,2) NOT NULL,
    is_estimated BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE event_consumption_plans (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    event_id BIGINT NOT NULL REFERENCES events(id),
    product_name VARCHAR(255) NOT NULL,
    category VARCHAR(100) NOT NULL,
    estimated_consumption_per_person NUMERIC(19,4) NOT NULL,
    expected_guest_count INTEGER NOT NULL,
    waste_percentage NUMERIC(19,2) NOT NULL,
    unit_cost NUMERIC(19,2) NOT NULL,
    required_quantity NUMERIC(19,4) NOT NULL,
    total_cost NUMERIC(19,2) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE ticket_price_suggestions (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    event_id BIGINT NOT NULL REFERENCES events(id),
    total_estimated_cost NUMERIC(19,2) NOT NULL,
    target_profit_margin NUMERIC(19,2) NOT NULL,
    target_revenue NUMERIC(19,2) NOT NULL,
    expected_guest_count INTEGER NOT NULL,
    suggested_ticket_price NUMERIC(19,2) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE event_closing_reports (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    event_id BIGINT NOT NULL UNIQUE REFERENCES events(id),
    actual_guest_count INTEGER NOT NULL,
    actual_ticket_revenue NUMERIC(19,2) NOT NULL,
    actual_deposit_amount NUMERIC(19,2) NOT NULL,
    actual_door_payment_amount NUMERIC(19,2) NOT NULL,
    actual_extra_sales_amount NUMERIC(19,2) NOT NULL,
    actual_total_revenue NUMERIC(19,2) NOT NULL,
    actual_total_cost NUMERIC(19,2) NOT NULL,
    actual_profit NUMERIC(19,2) NOT NULL,
    estimated_total_revenue NUMERIC(19,2) NOT NULL,
    estimated_total_cost NUMERIC(19,2) NOT NULL,
    estimated_profit NUMERIC(19,2) NOT NULL,
    profit_difference NUMERIC(19,2) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_events_company_date ON events(company_id, event_date);
CREATE INDEX idx_reservation_company_event ON reservations(company_id, event_id);
CREATE INDEX idx_payment_company_reservation ON reservation_payments(company_id, reservation_id);
CREATE INDEX idx_cost_company_event ON event_costs(company_id, event_id);
CREATE INDEX idx_consumption_company_event ON event_consumption_plans(company_id, event_id);
CREATE INDEX idx_ticket_suggestion_company_event ON ticket_price_suggestions(company_id, event_id);
