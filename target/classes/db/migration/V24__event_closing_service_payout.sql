ALTER TABLE service_payouts
    ADD COLUMN payout_source VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
    ADD COLUMN closing_status VARCHAR(20) NOT NULL DEFAULT 'FINALIZED',
    ADD COLUMN payment_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    ADD COLUMN account_id BIGINT REFERENCES financial_accounts(id),
    ADD COLUMN payment_method_id BIGINT REFERENCES payment_methods(id),
    ADD COLUMN category_id BIGINT REFERENCES financial_categories(id),
    ADD COLUMN payment_date DATE;

UPDATE service_payouts sp
SET payment_status = 'PAID',
    account_id = ft.account_id,
    payment_method_id = ft.payment_method_id,
    category_id = ft.category_id,
    payment_date = ft.transaction_date
FROM financial_transactions ft
WHERE sp.financial_transaction_id = ft.id;

CREATE UNIQUE INDEX uq_service_payouts_event_closing
    ON service_payouts(company_id, event_id)
    WHERE payout_source = 'EVENT_CLOSING' AND event_id IS NOT NULL;

