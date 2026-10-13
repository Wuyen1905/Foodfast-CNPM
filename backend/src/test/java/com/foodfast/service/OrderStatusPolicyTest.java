package com.foodfast.service;

import com.foodfast.entity.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderStatusPolicyTest {

    @Test
    void acceptsNormalPreparationAndDeliveryFlow() {
        assertThat(OrderStatusPolicy.canTransition(OrderStatus.PENDING, OrderStatus.CONFIRMED)).isTrue();
        assertThat(OrderStatusPolicy.canTransition(OrderStatus.CONFIRMED, OrderStatus.PREPARING)).isTrue();
        assertThat(OrderStatusPolicy.canTransition(OrderStatus.PREPARING, OrderStatus.READY)).isTrue();
        assertThat(OrderStatusPolicy.canTransition(OrderStatus.READY, OrderStatus.DELIVERING)).isTrue();
        assertThat(OrderStatusPolicy.canTransition(OrderStatus.DELIVERING, OrderStatus.DELIVERED)).isTrue();
    }

    @Test
    void permitsRetryAfterDeliveryFailure() {
        assertThat(OrderStatusPolicy.canTransition(OrderStatus.DELIVERING, OrderStatus.READY)).isTrue();
    }

    @Test
    void rejectsSkippingPreparationOrChangingTerminalOrder() {
        assertThatThrownBy(() ->
                OrderStatusPolicy.requireTransition(OrderStatus.PENDING, OrderStatus.DELIVERED))
                .isInstanceOf(IllegalStateException.class);
        assertThat(OrderStatusPolicy.canTransition(OrderStatus.DELIVERED, OrderStatus.READY)).isFalse();
        assertThat(OrderStatusPolicy.canTransition(OrderStatus.CANCELLED, OrderStatus.CONFIRMED)).isFalse();
    }
}
