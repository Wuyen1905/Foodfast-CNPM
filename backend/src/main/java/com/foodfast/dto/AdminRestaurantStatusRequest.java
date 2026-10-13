package com.foodfast.dto;

import com.foodfast.entity.RestaurantLifecycleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminRestaurantStatusRequest(
        @NotNull RestaurantLifecycleStatus status,
        @NotBlank @Size(min = 5, max = 500) String reason
) {}
