package com.foodfast.service;

import com.foodfast.dto.CancelOrderRequest;
import com.foodfast.entity.CancellationActor;
import com.foodfast.entity.CancellationReasonCode;
import com.foodfast.entity.Order;
import com.foodfast.entity.OrderStatus;
import com.foodfast.event.OrderEventPublisher;
import com.foodfast.exception.ForbiddenOperationException;
import com.foodfast.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCancellationServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    @Mock
    private NotificationService notificationService;

    @Mock
    private VoucherService voucherService;

    @Mock
    private OrderStatusHistoryService orderStatusHistoryService;

    @Mock
    private CustomerNotificationService customerNotificationService;

    @Mock
    private OrderGroupPaymentService orderGroupPaymentService;

    private OrderCancellationService service;

    @BeforeEach
    void setUp() {
        service = new OrderCancellationService(
                orderRepository,
                orderEventPublisher,
                notificationService,
                voucherService,
                orderStatusHistoryService,
                customerNotificationService,
                orderGroupPaymentService
        );
    }

    @Test
    void customerCanCancelOwnedPendingOrderWithCustomerReason() {
        Order order = order(OrderStatus.PENDING);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        Order saved = service.cancel(order.getId(),
                CancellationActor.CUSTOMER,
                "customer-1",
                request(CancellationReasonCode.CUSTOMER_ORDERED_BY_MISTAKE, null));

        assertThat(saved.getStatusEnum()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(saved.getCancellationActor()).isEqualTo(CancellationActor.CUSTOMER);
        assertThat(saved.getCancellationReasonCode())
                .isEqualTo(CancellationReasonCode.CUSTOMER_ORDERED_BY_MISTAKE);
        assertThat(saved.getCancelledAt()).isNotNull();
        verify(orderEventPublisher).publishOrderUpdate(saved);
    }

    @Test
    void customerCannotCancelAnotherCustomersOrder() {
        Order order = order(OrderStatus.PENDING);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.cancel(order.getId(),
                CancellationActor.CUSTOMER,
                "customer-2",
                request(CancellationReasonCode.CUSTOMER_CHANGED_MIND, null)))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("chính mình");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void customerMustContactSupportAfterRestaurantConfirms() {
        Order order = order(OrderStatus.CONFIRMED);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.cancel(order.getId(),
                CancellationActor.CUSTOMER,
                "customer-1",
                request(CancellationReasonCode.CUSTOMER_WAIT_TOO_LONG, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("đã tiếp nhận đơn");
    }

    @Test
    void restaurantCanRejectItsOwnOrder() {
        Order order = order(OrderStatus.PENDING);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        Order saved = service.cancel(order.getId(),
                CancellationActor.RESTAURANT,
                "aloha",
                request(CancellationReasonCode.RESTAURANT_OUT_OF_STOCK, "Pizza Hawaii đã hết"));

        assertThat(saved.getCancellationActor()).isEqualTo(CancellationActor.RESTAURANT);
        assertThat(saved.getCancellationActorId()).isEqualTo("aloha");
        assertThat(saved.getCancellationReasonDetail()).isEqualTo("Pizza Hawaii đã hết");
    }

    @Test
    void restaurantCannotRejectAnotherRestaurantsOrder() {
        Order order = order(OrderStatus.PENDING);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.cancel(order.getId(),
                CancellationActor.RESTAURANT,
                "rest_2",
                request(CancellationReasonCode.RESTAURANT_OVERLOADED, null)))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("nhà hàng của mình");
    }

    @Test
    void paidOrderMovesToRefundPendingWhenCancelled() {
        Order order = order(OrderStatus.CONFIRMED);
        order.setPaymentMethod("vnpay");
        order.setPaymentStatus(PaymentStatusPolicy.PAID);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        Order saved = service.cancel(order.getId(),
                CancellationActor.RESTAURANT,
                "aloha",
                request(CancellationReasonCode.RESTAURANT_CANNOT_DELIVER, null));

        assertThat(saved.getPaymentStatus()).isEqualTo(PaymentStatusPolicy.REFUND_PENDING);
    }

    @Test
    void otherReasonRequiresMeaningfulDetail() {
        Order order = order(OrderStatus.PENDING);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.cancel(order.getId(),
                CancellationActor.CUSTOMER,
                "customer-1",
                request(CancellationReasonCode.OTHER, "abc")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ít nhất 5 ký tự");
    }

    private Order order(OrderStatus status) {
        Order order = new Order();
        order.setId("ORDER-1");
        order.setUserId("customer-1");
        order.setRestaurantId("restaurant_2");
        order.setStatus(status);
        order.setPaymentMethod("cod");
        order.setPaymentStatus(PaymentStatusPolicy.PENDING);
        return order;
    }

    private CancelOrderRequest request(CancellationReasonCode reasonCode, String detail) {
        CancelOrderRequest request = new CancelOrderRequest();
        request.reasonCode = reasonCode;
        request.detail = detail;
        return request;
    }
}
