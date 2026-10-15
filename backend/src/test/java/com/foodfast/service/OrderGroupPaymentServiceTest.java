package com.foodfast.service;

import com.foodfast.entity.Order;
import com.foodfast.entity.OrderGroup;
import com.foodfast.entity.OrderStatus;
import com.foodfast.repository.OrderGroupRepository;
import com.foodfast.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderGroupPaymentServiceTest {

    @Mock OrderGroupRepository groupRepository;
    @Mock OrderRepository orderRepository;
    private OrderGroupPaymentService service;

    @BeforeEach
    void setUp() {
        service = new OrderGroupPaymentService(groupRepository, orderRepository);
    }

    @Test
    void rejectedChildBeforePaymentReducesAmountAndInvalidatesSignedSession() {
        OrderGroup group = group("GROUP-1", "vnpay", PaymentStatusPolicy.PENDING, 200_000);
        group.setPaymentSessionId("FG-OLD");
        group.setPaymentRequestUrl("https://sandbox.example/old");
        Order active = order("ORDER-A", OrderStatus.PENDING, 120_000, PaymentStatusPolicy.PENDING);
        Order cancelled = order("ORDER-B", OrderStatus.CANCELLED, 80_000, PaymentStatusPolicy.PENDING);
        when(groupRepository.findByIdForUpdate(group.getId())).thenReturn(Optional.of(group));
        when(orderRepository.findByOrderGroupIdOrderByGroupPositionAsc(group.getId()))
                .thenReturn(List.of(active, cancelled));
        when(groupRepository.save(group)).thenReturn(group);

        OrderGroup result = service.refresh(group.getId());

        assertThat(result.getPayableAmount()).isEqualTo(120_000);
        assertThat(result.getPaymentSessionId()).isNull();
        assertThat(result.getPaymentRequestUrl()).isNull();
    }

    @Test
    void codGroupIsPaidOnlyAfterEveryActiveChildWasDelivered() {
        OrderGroup group = group("GROUP-COD", "cod", PaymentStatusPolicy.PENDING, 200_000);
        Order delivered = order("ORDER-A", OrderStatus.DELIVERED, 120_000, PaymentStatusPolicy.PAID);
        Order ready = order("ORDER-B", OrderStatus.READY, 80_000, PaymentStatusPolicy.PENDING);
        when(groupRepository.findByIdForUpdate(group.getId())).thenReturn(Optional.of(group));
        when(orderRepository.findByOrderGroupIdOrderByGroupPositionAsc(group.getId()))
                .thenReturn(List.of(delivered, ready));
        when(groupRepository.save(group)).thenReturn(group);

        assertThat(service.refresh(group.getId()).getPaymentStatus())
                .isEqualTo(PaymentStatusPolicy.PENDING);

        ready.setStatus(OrderStatus.DELIVERED);
        ready.setPaymentStatus(PaymentStatusPolicy.PAID);
        assertThat(service.refresh(group.getId()).getPaymentStatus())
                .isEqualTo(PaymentStatusPolicy.PAID);
    }

    private OrderGroup group(String id, String method, String status, int total) {
        OrderGroup group = new OrderGroup();
        group.setId(id);
        group.setPaymentMethod(method);
        group.setPaymentStatus(status);
        group.setTotal(total);
        group.setPayableAmount(total);
        return group;
    }

    private Order order(String id, OrderStatus status, int total, String paymentStatus) {
        Order order = new Order();
        order.setId(id);
        order.setStatus(status);
        order.setTotal(total);
        order.setPaymentStatus(paymentStatus);
        return order;
    }
}
