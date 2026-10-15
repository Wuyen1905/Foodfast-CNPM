package com.foodfast.entity;

/**
 * Lifecycle of one administrator-managed delivery task.
 */
public enum DeliveryStatus {
    WAITING,
    ASSIGNED,
    DELIVERING,
    DELIVERED,
    FAILED,
    CANCELLED
}
