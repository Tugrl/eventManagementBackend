CREATE TABLE event_staff_assignments (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    event_id BIGINT NOT NULL REFERENCES events(id),
    employee_id BIGINT NOT NULL REFERENCES employees(id),
    planned_daily_cost NUMERIC(19,2) NOT NULL DEFAULT 0,
    service_point NUMERIC(19,2) NOT NULL DEFAULT 0,
    notes TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_event_staff_assignment UNIQUE (event_id, employee_id)
);

CREATE INDEX idx_event_staff_assignments_company_event
    ON event_staff_assignments(company_id, event_id);
