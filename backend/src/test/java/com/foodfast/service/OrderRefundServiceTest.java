package com.foodfast.service;

import com.foodfast.dto.CompleteRefundRequest;
import com.foodfast.entity.Order;
import com.foodfast.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderRefundServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderGroupPaymentService orderGroupPaymentService;
    private OrderRefundService service;

    @BeforeEach
    void setUp() {
        service = new OrderRefundService(orderRepository, orderGroupPaymentService);
    }

    @Test
    void completesPartialRefundAndRefreshesGroupLedger() {
        Order order = new Order();
        order.setId("ORDER-REFUND-1");
        order.setTotal(150_000);
        order.setPartialRefundAmount(40_000);
        order.setPaymentStatus(PaymentStatusPolicy.PARTIAL_REFUND_PENDING);
        when(orderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        Order result = service.complete(
                order.getId(), new CompleteRefundRequest("REFUND-TXN-1", "Hoàn món thiếu")
        );

        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatusPolicy.PARTIAL_REFUNDED);
        assertThat(result.getRefundTransactionReference()).isEqualTo("REFUND-TXN-1");
        assertThat(result.getRefundCompletedAt()).isNotNull();
        verify(orderGroupPaymentService).refreshForOrder(order);
    }

    @Test
    void refusesOrderWithoutPendingRefund() {
        Order order = new Order();
        order.setId("ORDER-PAID-1");
        order.setPaymentStatus(PaymentStatusPolicy.PAID);
        when(orderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.complete(
                order.getId(), new CompleteRefundRequest("REFUND-TXN-2", null)
        )).hasMessageContaining("không có khoản hoàn tiền");
    }
}
