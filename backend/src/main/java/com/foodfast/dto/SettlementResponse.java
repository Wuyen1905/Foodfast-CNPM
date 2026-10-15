package com.foodfast.dto;

import com.foodfast.entity.SettlementStatus;

import java.time.Instant;
import java.util.List;

public record SettlementResponse(
        String id,
        String restaurantId,
        String restaurantName,
        Instant periodStart,
        Instant periodEnd,
        SettlementStatus status,
        int orderCount,
        long foodAmount,
        long restaurantTaxAmount,
        long restaurantDiscountAmount,
        long platformDiscountAmount,
        int commissionRateBps,
        long commissionAmount,
        long refundAmount,
        long adjustmentAmount,
        long payableAmount,
        String createdBy,
        Instant createdAt,
        String submittedBy,
        Instant submittedAt,
        String paidBy,
        Instant paidAt,
        String paymentReference,
        List<SettlementOrderResponse> orders
) {
}
