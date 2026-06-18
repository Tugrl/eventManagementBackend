ALTER TABLE event_consumption_plans
    ADD COLUMN inventory_item_id BIGINT REFERENCES inventory_items(id);

CREATE INDEX idx_consumption_inventory_item
    ON event_consumption_plans(inventory_item_id);
