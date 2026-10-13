package com.foodfast.service;

import com.foodfast.dto.AdminAnalyticsResponse;
import com.foodfast.entity.CancellationReasonCode;
import com.foodfast.entity.DeliveryAssignment;
import com.foodfast.entity.DeliveryStatus;
import com.foodfast.entity.Order;
import com.foodfast.entity.OrderStatus;
import com.foodfast.entity.Restaurant;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminAnalyticsServiceTest {
    private final AdminAnalyticsService service = new AdminAnalyticsService(null, null, null);

    @Test
    void reportUsesBusinessTimestampsAndNetRevenue() {
        long created = Instant.now().minusSeconds(24 * 60 * 60).toEpochMilli();

        Order delivered = order("ORDER-1", created, OrderStatus.DELIVERED);
        delivered.setPaymentStatus(PaymentStatusPolicy.PAID);
        delivered.setTotal(100_000);
        delivered.setConfirmedAt(created + 10 * 60_000L);
        delivered.setReadyAt(created + 40 * 60_000L);

        Order cancelled = order("ORDER-2", created + 60_000L, OrderStatus.CANCELLED);
        cancelled.setPaymentStatus(PaymentStatusPolicy.PENDING);
        cancelled.setTotal(80_000);
        cancelled.setCancellationReasonCode(CancellationReasonCode.CUSTOMER_CHANGED_MIND);

        DeliveryAssignment delivery = new DeliveryAssignment();
        delivery.setOrderId(delivered.getId());
        delivery.setRestaurantId("r1");
        delivery.setStatus(DeliveryStatus.DELIVERED);
        delivery.setAssignedAt(created + 40 * 60_000L);
        delivery.setStartedAt(created + 45 * 60_000L);
        delivery.setEstimatedDeliveryAt(created + 90 * 60_000L);
        delivery.setDeliveredAt(created + 80 * 60_000L);

        Restaurant restaurant = new Restaurant();
        restaurant.setId("r1");
        restaurant.setName("Nhà hàng kiểm thử");

        AdminAnalyticsResponse report = service.calculate(
                7,
                List.of(delivered, cancelled),
                List.of(delivery),
                List.of(restaurant)
        );

        assertThat(report.summary().totalOrders()).isEqualTo(2);
        assertThat(report.summary().completedOrders()).isEqualTo(1);
        assertThat(report.summary().cancelledOrders()).isEqualTo(1);
        assertThat(report.summary().netRevenue()).isEqualTo(100_000);
        assertThat(report.summary().completionRate()).isEqualTo(50.0);
        assertThat(report.summary().cancellationRate()).isEqualTo(50.0);
        assertThat(report.summary().onTimeDeliveryRate()).isEqualTo(100.0);
        assertThat(report.summary().averageConfirmationMinutes()).isEqualTo(10.0);
        assertThat(report.summary().averagePreparationMinutes()).isEqualTo(30.0);
        assertThat(report.summary().averageDeliveryMinutes()).isEqualTo(35.0);
        assertThat(report.trend()).hasSize(7);
        assertThat(report.trend().stream().mapToLong(AdminAnalyticsResponse.DailyPoint::orders).sum())
                .isEqualTo(2);
        assertThat(report.restaurants()).singleElement().satisfies(item -> {
            assertThat(item.name()).isEqualTo("Nhà hàng kiểm thử");
            assertThat(item.revenue()).isEqualTo(100_000);
        });
        assertThat(report.cancellationReasons()).singleElement()
                .satisfies(item -> assertThat(item.code()).isEqualTo("CUSTOMER_CHANGED_MIND"));
    }

    @Test
    void rejectsUnsupportedReportRange() {
        assertThatThrownBy(() -> service.calculate(14, List.of(), List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("7, 30 hoặc 90");
    }

    @Test
    void ignoresImpossibleHistoricalDeliveryDuration() {
        long created = Instant.now().minusSeconds(60 * 60).toEpochMilli();
        Order delivered = order("ORDER-STALE", created, OrderStatus.DELIVERED);

        DeliveryAssignment stale = new DeliveryAssignment();
        stale.setOrderId(delivered.getId());
        stale.setRestaurantId("r1");
        stale.setStatus(DeliveryStatus.DELIVERED);
        stale.setAssignedAt(created);
        stale.setStartedAt(1_690_000_300_000L);
        stale.setDeliveredAt(created + 30 * 60_000L);
        stale.setEstimatedDeliveryAt(created + 45 * 60_000L);

        AdminAnalyticsResponse report = service.calculate(
                7,
                List.of(delivered),
                List.of(stale),
                List.of()
        );

        assertThat(report.summary().averageDeliveryMinutes()).isNull();
        assertThat(report.summary().onTimeDeliveryRate()).isNull();
    }

    private Order order(String id, long createdAt, OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setRestaurantId("r1");
        order.setRestaurant("Nhà hàng kiểm thử");
        order.setCreatedAt(Instant.ofEpochMilli(createdAt));
        order.setStatus(status);
        return order;
    }
}
