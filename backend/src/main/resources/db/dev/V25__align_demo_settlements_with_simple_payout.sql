-- Tương đương migration MySQL dành cho dữ liệu demo H2/test.
UPDATE restaurant_settlement_orders
SET refund_amount = 0,
    payable_amount = CASE
        WHEN food_amount + restaurant_tax_amount - restaurant_discount_amount - commission_amount < 0 THEN 0
        ELSE food_amount + restaurant_tax_amount - restaurant_discount_amount - commission_amount
    END;

UPDATE restaurant_settlements settlement
SET food_amount = (
        SELECT COALESCE(SUM(item.food_amount), 0)
        FROM restaurant_settlement_orders item
        WHERE item.settlement_id = settlement.id
    ),
    restaurant_tax_amount = (
        SELECT COALESCE(SUM(item.restaurant_tax_amount), 0)
        FROM restaurant_settlement_orders item
        WHERE item.settlement_id = settlement.id
    ),
    restaurant_discount_amount = (
        SELECT COALESCE(SUM(item.restaurant_discount_amount), 0)
        FROM restaurant_settlement_orders item
        WHERE item.settlement_id = settlement.id
    ),
    platform_discount_amount = (
        SELECT COALESCE(SUM(item.platform_discount_amount), 0)
        FROM restaurant_settlement_orders item
        WHERE item.settlement_id = settlement.id
    ),
    commission_amount = (
        SELECT COALESCE(SUM(item.commission_amount), 0)
        FROM restaurant_settlement_orders item
        WHERE item.settlement_id = settlement.id
    ),
    refund_amount = 0,
    adjustment_amount = 0,
    payable_amount = (
        SELECT COALESCE(SUM(item.payable_amount), 0)
        FROM restaurant_settlement_orders item
        WHERE item.settlement_id = settlement.id
    )
WHERE EXISTS (
    SELECT 1
    FROM restaurant_settlement_orders item
    WHERE item.settlement_id = settlement.id
);
