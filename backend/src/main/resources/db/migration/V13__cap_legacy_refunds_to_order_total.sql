UPDATE complaints c
SET requested_refund_amount = CASE
        WHEN requested_refund_amount > (SELECT o.total FROM orders o WHERE o.id = c.order_id)
            THEN (SELECT o.total FROM orders o WHERE o.id = c.order_id)
        ELSE requested_refund_amount
    END,
    approved_refund_amount = CASE
        WHEN approved_refund_amount > (SELECT o.total FROM orders o WHERE o.id = c.order_id)
            THEN (SELECT o.total FROM orders o WHERE o.id = c.order_id)
        ELSE approved_refund_amount
    END
WHERE EXISTS (SELECT 1 FROM orders o WHERE o.id = c.order_id);

UPDATE complaint_history h
SET refund_amount = CASE
        WHEN refund_amount > (
            SELECT o.total
            FROM complaints c
            JOIN orders o ON o.id = c.order_id
            WHERE c.id = h.complaint_id
        ) THEN (
            SELECT o.total
            FROM complaints c
            JOIN orders o ON o.id = c.order_id
            WHERE c.id = h.complaint_id
        )
        ELSE refund_amount
    END
WHERE EXISTS (
    SELECT 1
    FROM complaints c
    JOIN orders o ON o.id = c.order_id
    WHERE c.id = h.complaint_id
);

UPDATE orders
SET partial_refund_amount = CASE
        WHEN partial_refund_amount > total THEN total
        ELSE partial_refund_amount
    END;
