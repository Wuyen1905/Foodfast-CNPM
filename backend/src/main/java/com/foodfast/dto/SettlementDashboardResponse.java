package com.foodfast.dto;

import java.util.List;

public record SettlementDashboardResponse(
        long pendingReviewCount,
        long pendingPaymentCount,
        long paidCount,
        long pendingPaymentAmount,
        long paidAmount,
        List<SettlementPreviewResponse> previews,
        List<SettlementResponse> settlements,
        List<String> alerts
) {
}
