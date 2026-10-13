package com.foodfast.dto;

public record VNPayPaymentResult(
        boolean verified,
        boolean success,
        String orderId,
        String paymentStatus,
        String transactionId,
        Long amount,
        String responseCode,
        String message
) {
}
