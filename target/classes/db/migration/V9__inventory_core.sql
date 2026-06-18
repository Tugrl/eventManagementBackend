CREATE TABLE inventory_items (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    name VARCHAR(255) NOT NULL,
    category VARCHAR(120),
    unit VARCHAR(30) NOT NULL,
    unit_cost NUMERIC(19,2) NOT NULL DEFAULT 0,
    current_quantity NUMERIC(19,4) NOT NULL DEFAULT 0,
    minimum_quantity NUMERIC(19,4) NOT NULL DEFAULT 0,
    bottle_volume_ml NUMERIC(19,4),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE inventory_movements (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    inventory_item_id BIGINT NOT NULL REFERENCES inventory_items(id),
    event_id BIGINT REFERENCES events(id),
    movement_type VARCHAR(30) NOT NULL,
    movement_date DATE NOT NULL,
    quantity NUMERIC(19,4) NOT NULL,
    unit_cost NUMERIC(19,2) NOT NULL DEFAULT 0,
    total_amount NUMERIC(19,2) NOT NULL DEFAULT 0,
    notes TEXT,
    created_by_user_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE event_inventory_usages (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    event_id BIGINT NOT NULL REFERENCES events(id),
    inventory_item_id BIGINT NOT NULL REFERENCES inventory_items(id),
    usage_date DATE NOT NULL,
    actual_guest_count INTEGER NOT NULL,
    consumption_quantity NUMERIC(19,4) NOT NULL DEFAULT 0,
    waste_quantity NUMERIC(19,4) NOT NULL DEFAULT 0,
    unit_cost NUMERIC(19,2) NOT NULL DEFAULT 0,
    total_consumption_cost NUMERIC(19,2) NOT NULL DEFAULT 0,
    total_waste_cost NUMERIC(19,2) NOT NULL DEFAULT 0,
    consumption_per_person NUMERIC(19,4) NOT NULL DEFAULT 0,
    waste_per_person NUMERIC(19,4) NOT NULL DEFAULT 0,
    notes TEXT,
    inventory_movement_id BIGINT REFERENCES inventory_movements(id),
    created_by_user_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_inventory_items_company ON inventory_items(company_id, is_active, name);
CREATE INDEX idx_inventory_movements_company_item ON inventory_movements(company_id, inventory_item_id, movement_date);
CREATE INDEX idx_inventory_movements_company_event ON inventory_movements(company_id, event_id);
CREATE INDEX idx_event_inventory_usages_company_event ON event_inventory_usages(company_id, event_id, usage_date);
