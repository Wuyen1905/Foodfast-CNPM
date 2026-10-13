package com.foodfast.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RestaurantCompleteDeliveryRequest(
        @NotBlank
        @Size(min = 10, max = 1000)
        String reason
) {
}
