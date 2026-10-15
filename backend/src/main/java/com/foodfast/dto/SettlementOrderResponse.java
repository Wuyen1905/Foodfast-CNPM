package com.foodfast.dto;

public record SettlementOrderResponse(
        String orderId,
        long foodAmount,
        long restaurantTaxAmount,
        long restaurantDiscountAmount,
        long platformDiscountAmount,
        long commissionAmount,
        long refundAmount,
        long payableAmount
) {
}
