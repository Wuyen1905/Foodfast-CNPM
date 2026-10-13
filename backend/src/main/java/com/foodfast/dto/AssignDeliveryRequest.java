package com.foodfast.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssignDeliveryRequest(
        @NotBlank String orderId,
        @NotBlank String courierId,
        Long estimatedDeliveryAt,
        @Size(max = 1000) String deliveryNote
) {
}
