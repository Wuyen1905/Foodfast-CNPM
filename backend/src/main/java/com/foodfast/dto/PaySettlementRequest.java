package com.foodfast.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PaySettlementRequest(
        @NotBlank(message = "Vui lòng nhập mã tham chiếu thanh toán")
        @Size(max = 120, message = "Mã tham chiếu không được quá 120 ký tự")
        String paymentReference
) {
}
