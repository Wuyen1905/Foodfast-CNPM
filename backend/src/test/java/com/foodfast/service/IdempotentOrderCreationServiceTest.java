package com.foodfast.service;

import com.foodfast.dto.CreateOrderRequest;
import com.foodfast.entity.Order;
import com.foodfast.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotentOrderCreationServiceTest {
    @Mock OrderRepository repository;
    @Mock OrderService orderService;
    private IdempotentOrderCreationService service;

    @BeforeEach
    void setUp() { service = new IdempotentOrderCreationService(repository, orderService); }

    @Test
    void repeatedIdenticalRequestCreatesOnlyOneOrder() {
        CreateOrderRequest request = request("customer-1", "product-1");
        Order created = new Order();
        created.setId("ORDER-1");
        when(repository.findByIdempotencyKey("order-attempt-1"))
                .thenReturn(Optional.empty(), Optional.of(created));
        when(orderService.createOrderFromRequest(eq(request), eq("order-attempt-1"), anyString()))
                .thenAnswer(invocation -> {
                    created.setIdempotencyKey(invocation.getArgument(1));
                    created.setIdempotencyRequestHash(invocation.getArgument(2));
                    return created;
                });

        var first = service.create(request, "order-attempt-1");
        var replay = service.create(request, "order-attempt-1");

        assertThat(first.replayed()).isFalse();
        assertThat(replay.replayed()).isTrue();
        assertThat(replay.order().getId()).isEqualTo("ORDER-1");
        verify(orderService).createOrderFromRequest(eq(request), eq("order-attempt-1"), anyString());
    }

    @Test
    void reusedKeyWithDifferentCartIsRejected() {
        CreateOrderRequest firstRequest = request("customer-1", "product-1");
        CreateOrderRequest changedRequest = request("customer-1", "product-2");
        Order created = new Order();
        created.setId("ORDER-1");
        when(repository.findByIdempotencyKey("order-attempt-2"))
                .thenReturn(Optional.empty(), Optional.of(created));
        when(orderService.createOrderFromRequest(eq(firstRequest), eq("order-attempt-2"), anyString()))
                .thenAnswer(invocation -> {
                    created.setIdempotencyKey(invocation.getArgument(1));
                    created.setIdempotencyRequestHash(invocation.getArgument(2));
                    return created;
                });

        service.create(firstRequest, "order-attempt-2");

        assertThatThrownBy(() -> service.create(changedRequest, "order-attempt-2"))
                .isInstanceOf(IdempotencyKeyReuseException.class)
                .hasMessageContaining("nội dung đặt hàng khác");
    }

    private CreateOrderRequest request(String userId, String productId) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.userId = userId;
        request.customerName = "Khách hàng";
        request.customerPhone = "0900000000";
        request.address = "123 Đường A";
        request.restaurantId = "aloha";
        request.paymentMethod = "cod";
        CreateOrderRequest.Item item = new CreateOrderRequest.Item();
        item.productId = productId;
        item.quantity = 1;
        request.items = List.of(item);
        return request;
    }
}
