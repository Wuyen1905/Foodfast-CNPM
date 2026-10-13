package com.foodfast.entity;

import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PREPARING,
    READY,
    DELIVERING,
    DELIVERED,
    CANCELLED;

    /**
     * Public API representation shared with the web application.
     * Database values remain the enum names (PENDING, CONFIRMED, ...).
     */
    @JsonValue
    public String apiValue() {
        return switch (this) {
            case PENDING -> "Pending";
            case CONFIRMED -> "Confirmed";
            case PREPARING -> "In Progress";
            case READY -> "Ready";
            case DELIVERING -> "Delivering";
            case DELIVERED -> "Delivered";
            case CANCELLED -> "Cancelled";
        };
    }
}

