package com.foodfast.dto;

import java.util.List;
import java.util.Map;

public record AdminAnalyticsResponse(
        int days,
        long from,
        long to,
        Summary summary,
        List<DailyPoint> trend,
        Map<String, Long> statusBreakdown,
        Map<String, Long> paymentBreakdown,
        List<ReasonCount> cancellationReasons,
        List<RestaurantPerformance> restaurants
) {
    public record Summary(
            long totalOrders,
            long completedOrders,
            long cancelledOrders,
            long netRevenue,
            long averageOrderValue,
            double completionRate,
            double cancellationRate,
            Double onTimeDeliveryRate,
            Double averageConfirmationMinutes,
            Double averagePreparationMinutes,
            Double averageDeliveryMinutes
    ) {
    }

    public record DailyPoint(
            String date,
            String label,
            long orders,
            long completed,
            long cancelled,
            long revenue
    ) {
    }

    public record ReasonCount(String code, long count) {
    }

    public record RestaurantPerformance(
            String id,
            String name,
            long orders,
            long completed,
            long cancelled,
            long revenue,
            double completionRate,
            double cancellationRate
    ) {
    }
}
