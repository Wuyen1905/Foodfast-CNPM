package com.foodfast.service;

import com.foodfast.dto.CreateSubstitutionProposalRequest;
import com.foodfast.dto.RespondSubstitutionRequest;
import com.foodfast.entity.Order;
import com.foodfast.entity.OrderItem;
import com.foodfast.entity.OrderStatus;
import com.foodfast.entity.Product;
import com.foodfast.entity.SubstitutionProposal;
import com.foodfast.entity.SubstitutionStatus;
import com.foodfast.event.OrderEventPublisher;
import com.foodfast.exception.ForbiddenOperationException;
import com.foodfast.repository.OrderRepository;
import com.foodfast.repository.ProductRepository;
import com.foodfast.repository.SubstitutionProposalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubstitutionServiceTest {
    @Mock SubstitutionProposalRepository proposalRepository;
    @Mock OrderRepository orderRepository;
    @Mock ProductRepository productRepository;
    @Mock OrderEventPublisher orderEventPublisher;
    private SubstitutionService service;

    @BeforeEach
    void setUp() {
        service = new SubstitutionService(
                proposalRepository, orderRepository, productRepository, orderEventPublisher
        );
    }

    @Test
    void restaurantCanOnlyProposeForItsOwnOrder() {
        Order order = order();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        CreateSubstitutionProposalRequest request = request();

        assertThatThrownBy(() -> service.propose(order.getId(), "rest_2", request))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("đơn của mình");
        verify(proposalRepository, never()).save(any());
    }

    @Test
    void replacementCannotCostMoreThanOriginal() {
        Order order = order();
        Product expensive = product("ak-expensive", "Món đắt", 120_000);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(productRepository.findById(expensive.getId())).thenReturn(Optional.of(expensive));

        assertThatThrownBy(() -> service.propose(order.getId(), "restaurant_2", request()))
                .hasMessageContaining("không được đắt hơn");
    }

    @Test
    void customerAcceptanceReplacesSnapshotAndCreatesPartialRefund() {
        Order order = order();
        order.setPaymentMethod("vnpay");
        order.setPaymentStatus(PaymentStatusPolicy.PAID);
        Product rice = product("ak-3", "Cơm Chiên Hawaii", 69_000);
        SubstitutionProposal proposal = proposal(order, rice);
        when(proposalRepository.findById(10L)).thenReturn(Optional.of(proposal));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(productRepository.findById(rice.getId())).thenReturn(Optional.of(rice));
        when(orderRepository.save(order)).thenReturn(order);
        when(proposalRepository.save(proposal)).thenReturn(proposal);
        RespondSubstitutionRequest response = new RespondSubstitutionRequest();
        response.accept = true;

        SubstitutionProposal saved = service.respond(order.getId(), 10L, "u2", response);

        OrderItem item = order.getItems().get(0);
        assertThat(saved.getStatus()).isEqualTo(SubstitutionStatus.ACCEPTED);
        assertThat(item.getProductId()).isEqualTo("ak-3");
        assertThat(item.getOriginalProductName()).isEqualTo("Pizza Hawaii");
        assertThat(order.getTotal()).isEqualTo(84_000);
        assertThat(order.getPartialRefundAmount()).isEqualTo(20_000);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatusPolicy.PARTIAL_REFUND_PENDING);
        verify(orderEventPublisher).publishOrderUpdate(order);
    }

    @Test
    void customerRejectionDoesNotChangeOrder() {
        Order order = order();
        Product rice = product("ak-3", "Cơm Chiên Hawaii", 69_000);
        SubstitutionProposal proposal = proposal(order, rice);
        when(proposalRepository.findById(10L)).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(proposal)).thenReturn(proposal);
        RespondSubstitutionRequest response = new RespondSubstitutionRequest();
        response.accept = false;

        SubstitutionProposal saved = service.respond(order.getId(), 10L, "u2", response);

        assertThat(saved.getStatus()).isEqualTo(SubstitutionStatus.REJECTED);
        assertThat(order.getItems().get(0).getProductId()).isEqualTo("ak-2");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void customerCannotRespondThroughAnotherOrderUrl() {
        Order order = order();
        Product rice = product("ak-3", "Cơm Chiên Hawaii", 69_000);
        SubstitutionProposal proposal = proposal(order, rice);
        when(proposalRepository.findById(10L)).thenReturn(Optional.of(proposal));
        RespondSubstitutionRequest response = new RespondSubstitutionRequest();
        response.accept = true;

        assertThatThrownBy(() -> service.respond("ORDER-KHAC", 10L, "u2", response))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không thuộc đơn hàng");
        verify(orderRepository, never()).save(any());
    }

    private Order order() {
        Order order = new Order();
        order.setId("ORDER-1");
        order.setUserId("u2");
        order.setRestaurantId("restaurant_2");
        order.setStatus(OrderStatus.CONFIRMED);
        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setProductId("ak-2");
        item.setName("Pizza Hawaii");
        item.setProductName("Pizza Hawaii");
        item.setQty(1);
        item.setPrice(89_000);
        order.addItem(item);
        order.recalculateTotal();
        return order;
    }

    private Product product(String id, String name, int price) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setPrice(price);
        product.setRestaurant("Aloha");
        product.setAvailable(true);
        product.setCustomizable(false);
        return product;
    }

    private CreateSubstitutionProposalRequest request() {
        CreateSubstitutionProposalRequest request = new CreateSubstitutionProposalRequest();
        request.orderItemId = 1L;
        request.replacementProductId = "ak-expensive";
        request.reason = "Món ban đầu đã hết nguyên liệu";
        request.expiresInMinutes = 15;
        return request;
    }

    private SubstitutionProposal proposal(Order order, Product replacement) {
        SubstitutionProposal proposal = new SubstitutionProposal();
        proposal.setOrderId(order.getId());
        proposal.setOrderItemId(1L);
        proposal.setRestaurantId(order.getRestaurantId());
        proposal.setCustomerId(order.getUserId());
        proposal.setOriginalProductId("ak-2");
        proposal.setOriginalProductName("Pizza Hawaii");
        proposal.setOriginalUnitPrice(89_000);
        proposal.setReplacementProductId(replacement.getId());
        proposal.setReplacementProductName(replacement.getName());
        proposal.setReplacementUnitPrice(replacement.getPrice());
        proposal.setStatus(SubstitutionStatus.PENDING_CUSTOMER);
        proposal.setProposedAt(Instant.now());
        proposal.setExpiresAt(Instant.now().plusSeconds(900));
        try {
            var field = SubstitutionProposal.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(proposal, 10L);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
        return proposal;
    }
}
