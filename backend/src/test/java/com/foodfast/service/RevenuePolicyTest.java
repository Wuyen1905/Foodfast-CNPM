package com.foodfast.service;

import com.foodfast.entity.Order;
import com.foodfast.entity.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RevenuePolicyTest {

    @Test
    void countsOnlyDeliveredAndPaidOrders() {
        assertThat(RevenuePolicy.netRevenue(order(OrderStatus.DELIVERED, "paid", 120_000, 0)))
                .isEqualTo(120_000);
        assertThat(RevenuePolicy.netRevenue(order(OrderStatus.PREPARING, "paid", 120_000, 0)))
                .isZero();
        assertThat(RevenuePolicy.netRevenue(order(OrderStatus.CANCELLED, "paid", 120_000, 0)))
                .isZero();
        assertThat(RevenuePolicy.netRevenue(order(OrderStatus.DELIVERED, "pending", 120_000, 0)))
                .isZero();
    }

    @Test
    void subtractsPartialRefundAndExcludesFullRefund() {
        assertThat(RevenuePolicy.netRevenue(
                order(OrderStatus.DELIVERED, "partial_refunded", 120_000, 25_000)
        )).isEqualTo(95_000);
        assertThat(RevenuePolicy.netRevenue(
                order(OrderStatus.DELIVERED, "partial_refund_pending", 120_000, 25_000)
        )).isEqualTo(95_000);
        assertThat(RevenuePolicy.netRevenue(
                order(OrderStatus.DELIVERED, "refunded", 120_000, 0)
        )).isZero();
        assertThat(RevenuePolicy.netRevenue(
                order(OrderStatus.DELIVERED, "refund_pending", 120_000, 0)
        )).isZero();
    }

    private Order order(OrderStatus status, String paymentStatus, int total, int partialRefund) {
        Order order = new Order();
        order.setStatus(status);
        order.setPaymentStatus(paymentStatus);
        order.setTotal(total);
        order.setPartialRefundAmount(partialRefund);
        return order;
    }
}
