package com.foodfast.service;

import com.foodfast.entity.Order;
import com.foodfast.entity.OrderStatus;

/**
 * One source of truth for monetary reports. FoodFast reports net recognized
 * revenue, not the value of every order ever created.
 */
public final class RevenuePolicy {

    private RevenuePolicy() {
    }

    public static boolean contributesToRevenue(Order order) {
        if (order == null || order.getStatusEnum() != OrderStatus.DELIVERED) {
            return false;
        }
        String paymentStatus = PaymentStatusPolicy.normalize(order.getPaymentStatus());
        return PaymentStatusPolicy.PAID.equals(paymentStatus)
                || PaymentStatusPolicy.PARTIAL_REFUND_PENDING.equals(paymentStatus)
                || PaymentStatusPolicy.PARTIAL_REFUNDED.equals(paymentStatus);
    }

    public static long netRevenue(Order order) {
        if (!contributesToRevenue(order)) {
            return 0L;
        }

        long total = Math.max(0, order.getTotal());
        String paymentStatus = PaymentStatusPolicy.normalize(order.getPaymentStatus());
        if (PaymentStatusPolicy.PARTIAL_REFUND_PENDING.equals(paymentStatus)
                || PaymentStatusPolicy.PARTIAL_REFUNDED.equals(paymentStatus)) {
            return Math.max(0L, total - Math.max(0, order.getPartialRefundAmount()));
        }
        return total;
    }
}
