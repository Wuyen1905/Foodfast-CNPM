package com.foodfast.dto;

import com.foodfast.entity.DeliveryAssignment;
import com.foodfast.entity.DeliveryCompletionSource;
import com.foodfast.entity.DeliveryStatus;
import com.foodfast.entity.RestaurantCourier;

public record DeliveryDetailsResponse(
        String id,
        String orderId,
        String restaurantId,
        String courierId,
        String courierName,
        String vehicleType,
        String licensePlate,
        DeliveryStatus status,
        Long estimatedDeliveryAt,
        String deliveryNote,
        String failureReason,
        long assignedAt,
        Long startedAt,
        Long deliveredAt,
        DeliveryCompletionSource completionSource,
        String completionNote,
        Long failedAt,
        Long cancelledAt,
        long updatedAt
) {
    public static DeliveryDetailsResponse from(
            DeliveryAssignment assignment,
            RestaurantCourier courier
    ) {
        return new DeliveryDetailsResponse(
                assignment.getId(),
                assignment.getOrderId(),
                assignment.getRestaurantId(),
                assignment.getCourierId(),
                courier == null ? null : courier.getName(),
                courier == null ? null : courier.getVehicleType(),
                courier == null ? null : courier.getLicensePlate(),
                assignment.getStatus(),
                assignment.getEstimatedDeliveryAt(),
                assignment.getDeliveryNote(),
                assignment.getFailureReason(),
                assignment.getAssignedAt(),
                assignment.getStartedAt(),
                assignment.getDeliveredAt(),
                assignment.getCompletionSource(),
                assignment.getCompletionNote(),
                assignment.getFailedAt(),
                assignment.getCancelledAt(),
                assignment.getUpdatedAt()
        );
    }
}
