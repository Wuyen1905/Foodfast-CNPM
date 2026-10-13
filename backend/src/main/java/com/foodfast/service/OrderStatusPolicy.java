package com.foodfast.service;

import com.foodfast.entity.OrderStatus;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Central state machine for the order lifecycle.
 *
 * Delivery failure is the only backward transition: DELIVERING -> READY so
 * that the restaurant can assign another courier.
 */
public final class OrderStatusPolicy {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = Map.of(
            OrderStatus.PENDING, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.PREPARING, OrderStatus.CANCELLED),
            OrderStatus.PREPARING, EnumSet.of(OrderStatus.READY, OrderStatus.CANCELLED),
            OrderStatus.READY, EnumSet.of(OrderStatus.DELIVERING, OrderStatus.CANCELLED),
            OrderStatus.DELIVERING, EnumSet.of(OrderStatus.DELIVERED, OrderStatus.READY),
            OrderStatus.DELIVERED, EnumSet.noneOf(OrderStatus.class),
            OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class)
    );

    private OrderStatusPolicy() {
    }

    public static boolean canTransition(OrderStatus current, OrderStatus next) {
        if (current == null || next == null) return false;
        if (current == next) return true;
        return ALLOWED.getOrDefault(current, Set.of()).contains(next);
    }

    public static void requireTransition(OrderStatus current, OrderStatus next) {
        if (!canTransition(current, next)) {
            throw new IllegalStateException(
                    "Không thể chuyển trạng thái đơn từ " + current + " sang " + next
            );
        }
    }
}
