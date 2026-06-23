UPDATE financial_transactions ft
SET reference_id = rp.id
FROM reservation_payments rp
WHERE rp.financial_transaction_id = ft.id
  AND ft.operation_source = 'RESERVATION_PAYMENT';

UPDATE financial_transactions ft
SET reference_id = sp.id
FROM service_payouts sp
WHERE sp.financial_transaction_id = ft.id
  AND ft.operation_source = 'STAFF_PAYOUT';

CREATE UNIQUE INDEX uq_financial_transactions_active_source_reference
    ON financial_transactions (company_id, operation_source, reference_id)
    WHERE status = 'ACTIVE'
      AND reference_id IS NOT NULL
      AND operation_source IN ('RESERVATION_PAYMENT', 'STAFF_PAYOUT', 'COMPANY_EXPENSE');
