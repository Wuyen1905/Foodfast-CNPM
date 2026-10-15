package com.foodfast.service;

import com.foodfast.entity.Order;
import com.foodfast.entity.VoucherScope;

/** Hàm tính tiền thuần, không nhận con số lợi nhuận từ trình duyệt. */
public final class RestaurantSettlementCalculator {
    private RestaurantSettlementCalculator() { }

    public static Components calculate(Order order, int commissionRateBps) {
        if (order == null) throw new IllegalArgumentException("Thiếu đơn hàng để tính đối soát");
        if (commissionRateBps < 0 || commissionRateBps > 10_000) {
            throw new IllegalArgumentException("Tỷ lệ hoa hồng không hợp lệ");
        }
        long food = Math.max(0, order.getSubtotal());
        long tax = Math.max(0, order.getTaxAmount());
        long restaurantDiscount = Math.min(food, resolvedRestaurantDiscount(order));
        long platformDiscount = Math.max(0L, (long) order.getDiscountAmount() - restaurantDiscount);
        long commissionBase = Math.max(0L, food - restaurantDiscount);
        long commission = Math.max(0L, (commissionBase * commissionRateBps + 5_000L) / 10_000L);
        // Chỉ đơn đã giao và đã thu tiền mới được dịch vụ đối soát lựa chọn.
        // Hoàn giao dịch cho khách không phải là khoản khấu trừ tự động của nhà hàng:
        // đơn đã hoàn bị loại khỏi đợt đối soát, còn hỗ trợ sau giao do FoodFast chịu.
        long payable = Math.max(0L, food + tax - restaurantDiscount - commission);
        return new Components(food, tax, restaurantDiscount, platformDiscount, commission, 0L, payable);
    }

    private static long resolvedRestaurantDiscount(Order order) {
        if (order.getRestaurantDiscountAmount() > 0) return order.getRestaurantDiscountAmount();
        return order.getVoucherScope() == VoucherScope.RESTAURANT ? order.getDiscountAmount() : 0L;
    }

    public record Components(
            long food,
            long tax,
            long restaurantDiscount,
            long platformDiscount,
            long commission,
            long refund,
            long payable
    ) { }
}
