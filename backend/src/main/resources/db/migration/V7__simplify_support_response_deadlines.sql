UPDATE support_tickets
SET department = 'FINANCE',
    priority = 'URGENT'
WHERE UPPER(intent) = 'REFUND_OR_PAYMENT';

UPDATE support_tickets
SET sla_due_at = created_at + CASE
    WHEN department = 'FINANCE' OR UPPER(intent) = 'REFUND_OR_PAYMENT' THEN 14400000
    WHEN priority = 'URGENT' THEN 14400000
    WHEN priority = 'HIGH' THEN 28800000
    ELSE 86400000
END;
