ALTER TABLE orders ADD COLUMN paid_at BIGINT;
ALTER TABLE order_items ADD COLUMN product_image VARCHAR(2048);

UPDATE order_items oi
SET product_image = (
    SELECT p.image_url FROM products p WHERE p.id = oi.product_id
)
WHERE oi.product_image IS NULL
  AND EXISTS (SELECT 1 FROM products p WHERE p.id = oi.product_id);

UPDATE orders
SET paid_at = COALESCE(
    delivered_at,
    CAST(EXTRACT(EPOCH FROM updated_at) * 1000 AS BIGINT)
)
WHERE LOWER(COALESCE(payment_status, '')) = 'paid'
  AND paid_at IS NULL;

UPDATE orders
SET tax_amount = 0,
    total = CASE
        WHEN subtotal + shipping_fee - discount_amount < 0 THEN 0
        ELSE subtotal + shipping_fee - discount_amount
    END
WHERE tax_amount <> 0;
