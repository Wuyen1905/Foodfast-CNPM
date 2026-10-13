package com.foodfast.entity;

/**
 * Lifecycle of one restaurant-managed delivery assignment.
 */
public enum DeliveryStatus {
    ASSIGNED,
    DELIVERING,
    DELIVERED,
    FAILED,
    CANCELLED
}
