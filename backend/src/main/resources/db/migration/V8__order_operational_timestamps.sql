ALTER TABLE orders ADD COLUMN preparing_at BIGINT;
ALTER TABLE orders ADD COLUMN ready_at BIGINT;
ALTER TABLE orders ADD COLUMN delivering_at BIGINT;
ALTER TABLE orders ADD COLUMN delivered_at BIGINT;

UPDATE orders o
SET delivering_at = (
    SELECT MIN(d.started_at)
    FROM delivery_assignments d
    WHERE d.order_id = o.id AND d.started_at IS NOT NULL
)
WHERE delivering_at IS NULL;

UPDATE orders o
SET delivered_at = (
    SELECT MAX(d.delivered_at)
    FROM delivery_assignments d
    WHERE d.order_id = o.id AND d.delivered_at IS NOT NULL
)
WHERE delivered_at IS NULL;

UPDATE orders o
SET ready_at = (
    SELECT MIN(d.assigned_at)
    FROM delivery_assignments d
    WHERE d.order_id = o.id
)
WHERE ready_at IS NULL;
