package com.foodfast.dto;

import com.foodfast.entity.DeliveryAssignment;
import com.foodfast.entity.Order;
import com.foodfast.entity.RestaurantCourier;

public record AdminDeliveryQueueResponse(
        String orderId,
        String orderGroupId,
        Integer groupPosition,
        String restaurantId,
        String restaurantName,
        String customerName,
        String customerPhone,
        String address,
        int orderTotal,
        String orderStatus,
        DeliveryDetailsResponse delivery
) {
    public static AdminDeliveryQueueResponse from(
            Order order,
            DeliveryAssignment assignment,
            RestaurantCourier courier
    ) {
        return new AdminDeliveryQueueResponse(
                order.getId(), order.getOrderGroupId(), order.getGroupPosition(),
                order.getRestaurantId(), order.getRestaurant(), order.getCustomerName(),
                order.getCustomerPhone(), order.getAddress(), order.getTotal(),
                order.getStatusEnum().apiValue(), DeliveryDetailsResponse.from(assignment, courier)
        );
    }
}
