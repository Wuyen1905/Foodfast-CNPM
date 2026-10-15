package com.foodfast.service;

import com.foodfast.entity.Order;
import com.foodfast.entity.OrderItem;
import com.foodfast.entity.VoucherScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RestaurantSettlementCalculatorTest {

    @Test
    void restaurantVoucherAndCommissionDeterminePayoutWithoutAutomaticRefundDeduction() {
        Order order = orderWithFoodAmount(200_000);
        order.setDiscountAmount(20_000);
        order.setRestaurantDiscountAmount(20_000);
        order.setVoucherScope(VoucherScope.RESTAURANT);
        order.setPaymentStatus(PaymentStatusPolicy.PARTIAL_REFUNDED);
        order.setPartialRefundAmount(30_000);

        var result = RestaurantSettlementCalculator.calculate(order, 1_500);

        assertThat(result.food()).isEqualTo(200_000);
        assertThat(result.restaurantDiscount()).isEqualTo(20_000);
        assertThat(result.platformDiscount()).isZero();
        assertThat(result.commission()).isEqualTo(27_000);
        assertThat(result.refund()).isZero();
        assertThat(result.payable()).isEqualTo(153_000);
    }

    @Test
    void globalVoucherDoesNotReduceRestaurantPayout() {
        Order order = orderWithFoodAmount(100_000);
        order.setDiscountAmount(10_000);
        order.setVoucherScope(VoucherScope.GLOBAL);
        order.setPaymentStatus(PaymentStatusPolicy.PAID);

        var result = RestaurantSettlementCalculator.calculate(order, 1_500);

        assertThat(result.restaurantDiscount()).isZero();
        assertThat(result.platformDiscount()).isEqualTo(10_000);
        assertThat(result.commission()).isEqualTo(15_000);
        assertThat(result.payable()).isEqualTo(85_000);
    }

    private Order orderWithFoodAmount(int foodAmount) {
        Order order = new Order();
        OrderItem item = new OrderItem();
        item.setQty(1);
        item.setPrice(foodAmount);
        order.addItem(item);
        order.recalculateTotal();
        return order;
    }
}
