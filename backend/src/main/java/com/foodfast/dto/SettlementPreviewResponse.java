package com.foodfast.dto;

import java.util.List;

public record SettlementPreviewResponse(
        String restaurantId,
        String restaurantName,
        int eligibleOrderCount,
        long foodAmount,
        long restaurantTaxAmount,
        long restaurantDiscountAmount,
        long platformDiscountAmount,
        long commissionAmount,
        long refundAmount,
        long payableAmount,
        List<String> orderIds,
        List<String> alerts
) {
}
