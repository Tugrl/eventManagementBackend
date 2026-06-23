ALTER TABLE event_consumption_plans
    ADD COLUMN input_unit VARCHAR(20),
    ADD COLUMN converted_unit_quantity NUMERIC(19,4) NOT NULL DEFAULT 0;
