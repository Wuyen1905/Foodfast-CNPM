package com.foodfast.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateVNPayPaymentRequest(
        @NotBlank(message = "Mã đơn hàng không được để trống") String orderId
) {
}
