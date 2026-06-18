ALTER TABLE events
    ADD COLUMN revenue_model VARCHAR(40) NOT NULL DEFAULT 'CLOSED_ORGANIZATION',
    ADD COLUMN primary_contact_id BIGINT,
    ADD COLUMN agreed_revenue NUMERIC(19,2),
    ADD COLUMN ticket_price NUMERIC(19,2),
    ADD COLUMN target_ticket_count INTEGER,
    ADD COLUMN complimentary_guest_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN sponsor_revenue_target NUMERIC(19,2),
    ADD COLUMN target_revenue_amount NUMERIC(19,2),
    ADD COLUMN target_profit_amount NUMERIC(19,2);
