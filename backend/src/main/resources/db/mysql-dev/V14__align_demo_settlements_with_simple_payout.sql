-- Dữ liệu phát triển cũ từng trừ toàn bộ khoản hoàn giao dịch (kể cả phí giao hàng)
-- vào tiền nhà hàng. Hiệu chỉnh các đợt demo theo công thức mới, không áp dụng cho prod.
UPDATE restaurant_settlement_orders
SET refund_amount = 0,
    payable_amount = GREATEST(
        0,
        food_amount + restaurant_tax_amount - restaurant_discount_amount - commission_amount
    );

UPDATE restaurant_settlements settlement
JOIN (
    SELECT settlement_id,
           SUM(food_amount) AS food_amount,
           SUM(restaurant_tax_amount) AS restaurant_tax_amount,
           SUM(restaurant_discount_amount) AS restaurant_discount_amount,
           SUM(platform_discount_amount) AS platform_discount_amount,
           SUM(commission_amount) AS commission_amount,
           SUM(payable_amount) AS payable_amount
    FROM restaurant_settlement_orders
    GROUP BY settlement_id
) totals ON totals.settlement_id = settlement.id
SET settlement.food_amount = totals.food_amount,
    settlement.restaurant_tax_amount = totals.restaurant_tax_amount,
    settlement.restaurant_discount_amount = totals.restaurant_discount_amount,
    settlement.platform_discount_amount = totals.platform_discount_amount,
    settlement.commission_amount = totals.commission_amount,
    settlement.refund_amount = 0,
    settlement.adjustment_amount = 0,
    settlement.payable_amount = totals.payable_amount;
