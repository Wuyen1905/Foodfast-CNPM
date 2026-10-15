package com.foodfast.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminCourierRequest(
        String restaurantId,
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Pattern(regexp = "^(0|\\+84)[0-9]{9}$") String phone,
        @Size(max = 50) String vehicleType,
        @Size(max = 30) String licensePlate
) {
}
