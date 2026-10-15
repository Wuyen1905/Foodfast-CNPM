package com.foodfast.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateSettlementRequest(
        @NotBlank(message = "Vui lòng chọn nhà hàng") String restaurantId,
        @NotNull(message = "Vui lòng chọn thời điểm bắt đầu") Instant periodStart,
        @NotNull(message = "Vui lòng chọn thời điểm kết thúc") Instant periodEnd
) {
}
