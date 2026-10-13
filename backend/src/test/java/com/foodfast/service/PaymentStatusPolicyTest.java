package com.foodfast.service;

import com.foodfast.entity.Order;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentStatusPolicyTest {

    @Test
    void unpaidVNPayOrderIsHiddenFromRestaurant() {
        Order order = new Order();
        order.setPaymentMethod("vnpay");
        order.setPaymentStatus("pending");

        assertThat(PaymentStatusPolicy.isReadyForRestaurant(order)).isFalse();
    }

    @Test
    void paidVNPayAndCodOrdersAreVisibleToRestaurant() {
        Order paidVNPay = new Order();
        paidVNPay.setPaymentMethod("vnpay");
        paidVNPay.setPaymentStatus("paid");

        Order cod = new Order();
        cod.setPaymentMethod("cod");
        cod.setPaymentStatus("pending");

        assertThat(PaymentStatusPolicy.isReadyForRestaurant(paidVNPay)).isTrue();
        assertThat(PaymentStatusPolicy.isReadyForRestaurant(cod)).isTrue();
    }

    @Test
    void pendingCodBecomesPaidOnlyAfterSuccessfulDelivery() {
        Order cod = new Order();
        cod.setPaymentMethod("cod");
        cod.setPaymentStatus("pending");

        assertThat(PaymentStatusPolicy.statusAfterSuccessfulDelivery(cod)).isEqualTo("paid");
    }

    @Test
    void deliveryDoesNotRewriteOnlinePaymentState() {
        Order vnpay = new Order();
        vnpay.setPaymentMethod("vnpay");
        vnpay.setPaymentStatus("paid");

        assertThat(PaymentStatusPolicy.statusAfterSuccessfulDelivery(vnpay)).isEqualTo("paid");
    }
}
