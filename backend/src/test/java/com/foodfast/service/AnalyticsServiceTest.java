package com.foodfast.service;

import com.foodfast.entity.DeliveryAssignment;
import com.foodfast.entity.Order;
import com.foodfast.entity.OrderItem;
import com.foodfast.entity.OrderStatus;
import com.foodfast.entity.Restaurant;
import com.foodfast.repository.AnalyticsRepository;
import com.foodfast.repository.DeliveryAssignmentRepository;
import com.foodfast.repository.OrderRepository;
import com.foodfast.repository.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock AnalyticsRepository analyticsRepository;
    @Mock OrderRepository orderRepository;
    @Mock RestaurantRepository restaurantRepository;
    @Mock DeliveryAssignmentRepository deliveryAssignmentRepository;

    private AnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new AnalyticsService(
                analyticsRepository, orderRepository, restaurantRepository, deliveryAssignmentRepository
        );
    }

    @Test
    void dashboardUsesRecognizedRevenueAndRealOperationalTimes() {
        Instant created = Instant.now().minusSeconds(30 * 60);
        Order delivered = order("ORDER-1", OrderStatus.DELIVERED, created, 120_000);
        delivered.setConfirmedAt(created.plusSeconds(5 * 60).toEpochMilli());
        Order cancelled = order("ORDER-2", OrderStatus.CANCELLED, created, 90_000);

        Restaurant restaurant = new Restaurant();
        restaurant.setId("restaurant_2");
        restaurant.setName("Aloha");
        restaurant.setRating(4.7);

        DeliveryAssignment assignment = new DeliveryAssignment();
        assignment.setRestaurantId("restaurant_2");
        assignment.setStartedAt(Instant.now().minusSeconds(25 * 60).toEpochMilli());
        assignment.setDeliveredAt(Instant.now().minusSeconds(5 * 60).toEpochMilli());

        when(restaurantRepository.findById("restaurant_2")).thenReturn(Optional.of(restaurant));
        when(orderRepository.findByRestaurantIdIn(List.of("restaurant_2", "aloha")))
                .thenReturn(List.of(delivered, cancelled));
        when(deliveryAssignmentRepository.findByRestaurantIdOrderByAssignedAtDesc("restaurant_2"))
                .thenReturn(List.of(assignment));

        Map<String, Object> overview = service.getRestaurantOverview("aloha");

        assertThat(overview.get("revenue")).isEqualTo(120_000L);
        assertThat(overview.get("ordersToday")).isEqualTo(2);
        assertThat((double) overview.get("completionRate")).isEqualTo(50.0);
        assertThat((double) overview.get("cancellationRate")).isEqualTo(50.0);
        assertThat(overview.get("avgConfirmationTime")).isEqualTo(5);
        assertThat(overview.get("avgDeliveryTime")).isEqualTo(20);
        assertThat((List<?>) overview.get("revenueTrend")).hasSize(7);
    }

    private Order order(String id, OrderStatus status, Instant createdAt, int total) {
        Order order = new Order();
        order.setId(id);
        order.setRestaurantId("restaurant_2");
        order.setStatus(status);
        order.setCreatedAt(createdAt);
        order.setTotal(total);
        order.setPaymentStatus("paid");
        OrderItem item = new OrderItem();
        item.setName("Pizza Hawaii");
        item.setQty(1);
        item.setPrice(total);
        order.setItems(List.of(item));
        return order;
    }
}
