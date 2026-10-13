package com.foodfast.service;

public class CartRestaurantConflictException extends RuntimeException {

    private final String currentRestaurantId;
    private final String requestedRestaurantId;

    public CartRestaurantConflictException(
            String currentRestaurantId,
            String requestedRestaurantId
    ) {
        super("Mỗi đơn hàng chỉ được chứa món của một nhà hàng");
        this.currentRestaurantId = currentRestaurantId;
        this.requestedRestaurantId = requestedRestaurantId;
    }

    public String getCurrentRestaurantId() {
        return currentRestaurantId;
    }

    public String getRequestedRestaurantId() {
        return requestedRestaurantId;
    }
}
