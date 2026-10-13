package com.foodfast.dto;

import jakarta.validation.constraints.NotBlank;

public record CustomerConfirmDeliveryRequest(
        @NotBlank String customerId
) {
}
