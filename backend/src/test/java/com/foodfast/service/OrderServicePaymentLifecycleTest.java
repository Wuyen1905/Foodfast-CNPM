package com.foodfast.service;

import com.foodfast.entity.Order;
import com.foodfast.entity.OrderStatus;
import com.foodfast.event.OrderEventPublisher;
import com.foodfast.repository.OrderRepository;
import com.foodfast.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServicePaymentLifecycleTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ProductRepository productRepository;
    @Mock private OrderEventPublisher orderEventPublisher;
    @Mock private NotificationService notificationService;
    @Mock private RestaurantService restaurantService;
    @Mock private ProductOptionPricingService optionPricingService;
    @Mock private VoucherService voucherService;
    @Mock private OrderStatusHistoryService orderStatusHistoryService;
    @Mock private CustomerNotificationService customerNotificationService;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderRepository,
                new ProductAvailabilityService(productRepository),
                orderEventPublisher,
                notificationService,
                restaurantService,
                optionPricingService,
                voucherService,
                orderStatusHistoryService,
                customerNotificationService
        );
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void deliveringCodOrderBecomesPaidWhenDelivered() {
        Order order = new Order();
        order.setId("ORDER-COD-1");
        order.setStatus(OrderStatus.DELIVERING);
        order.setPaymentMethod("cod");
        order.setPaymentStatus("pending");
        when(orderRepository.findById("ORDER-COD-1")).thenReturn(Optional.of(order));

        Order delivered = orderService.updateStatus("ORDER-COD-1", OrderStatus.DELIVERED);

        assertThat(delivered.getStatusEnum()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(delivered.getPaymentStatus()).isEqualTo(PaymentStatusPolicy.PAID);
        assertThat(delivered.getPaidAt()).isNotNull();
    }

    @Test
    void confirmationAuditFieldsComeFromAuthenticatedBackendActor() {
        Order order = new Order();
        order.setId("ORDER-CONFIRM-1");
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMethod("cod");
        order.setPaymentStatus("pending");
        when(orderRepository.findById("ORDER-CONFIRM-1")).thenReturn(Optional.of(order));

        long before = System.currentTimeMillis();
        Order confirmed = orderService.updateStatus(
                "ORDER-CONFIRM-1",
                OrderStatus.CONFIRMED,
                "rest_2"
        );

        assertThat(confirmed.getConfirmedBy()).isEqualTo("rest_2");
        assertThat(confirmed.getConfirmedAt()).isGreaterThanOrEqualTo(before);
    }
}
