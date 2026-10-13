package com.foodfast.service;

import com.foodfast.dto.SupportChatRequest;
import com.foodfast.dto.SupportChatResponse;
import com.foodfast.entity.Order;
import com.foodfast.entity.OrderStatus;
import com.foodfast.entity.SupportTicket;
import com.foodfast.entity.DeliveryAssignment;
import com.foodfast.entity.DeliveryStatus;
import com.foodfast.entity.RestaurantCourier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

class SupportChatServiceTest {

    private OrderService orderService;
    private SupportTicketService ticketService;
    private DeliveryAssignmentService deliveryService;
    private SupportChatService service;

    @BeforeEach
    void setUp() {
        orderService = mock(OrderService.class);
        ticketService = mock(SupportTicketService.class);
        deliveryService = mock(DeliveryAssignmentService.class);
        when(orderService.findById(anyString())).thenReturn(Optional.empty());
        when(deliveryService.findCurrentByOrderId(anyString())).thenReturn(Optional.empty());
        when(ticketService.create(any(), any(), any(), any(), anyString(), any(), any())).thenAnswer(invocation -> {
            SupportTicket ticket = new SupportTicket();
            ticket.setId("TICKET-TEST");
            return ticket;
        });
        service = new SupportChatService(
                orderService,
                new SupportIntentClassifier(),
                ticketService,
                deliveryService
        );
    }

    @Test
    void refundRequestsAreHandedToAdminWithoutMutation() {
        SupportChatResponse result = service.reply(
                new SupportChatRequest("Tôi muốn hoàn tiền", "ORDER-1"),
                "CUSTOMER-1",
                "Khách hàng",
                "0901234567"
        );

        assertThat(result.intent()).isEqualTo("REFUND_OR_PAYMENT");
        assertThat(result.handoffTarget()).isEqualTo("ADMIN");
        assertThat(result.supportTicketId()).isEqualTo("TICKET-TEST");
        assertThat(result.reply()).contains("không thể tự hoàn tiền");
    }

    @Test
    void foodIssuesAreHandedToRestaurant() {
        when(orderService.findById("ORDER-1"))
                .thenReturn(Optional.of(order("ORDER-1", "0901234567", OrderStatus.READY)));
        SupportChatResponse result = service.reply(
                new SupportChatRequest("Đơn bị thiếu món", "ORDER-1"),
                "CUSTOMER-1",
                "Khách hàng",
                "0901234567"
        );

        assertThat(result.handoffTarget()).isEqualTo("RESTAURANT");
        assertThat(result.supportTicketId()).isEqualTo("TICKET-TEST");
    }

    @Test
    void orderLookupRequiresAuthenticatedOwner() {
        Order order = order("ORDER-1", "0901234567", OrderStatus.READY);
        when(orderService.findById("ORDER-1")).thenReturn(Optional.of(order));

        SupportChatResponse result = service.reply(
                new SupportChatRequest("Đơn của tôi đâu?", "ORDER-1"),
                "CUSTOMER-2",
                "Khách hàng",
                "0900000000"
        );

        assertThat(result.reply()).contains("bảo vệ thông tin");
    }

    @Test
    void orderLookupReturnsActualStatusForOwner() {
        Order order = order("ORDER-1", "0901234567", OrderStatus.DELIVERING);
        when(orderService.findById("ORDER-1")).thenReturn(Optional.of(order));

        SupportChatResponse result = service.reply(
                new SupportChatRequest("Đơn của tôi đâu?", "ORDER-1"),
                "CUSTOMER-1",
                "Khách hàng",
                "0901234567"
        );

        assertThat(result.intent()).isEqualTo("ORDER_STATUS");
        assertThat(result.reply()).contains("đang trên đường giao tới bạn");
    }

    @Test
    void loggedInOwnerCanLookupOrderWhenAccountPhoneHasChanged() {
        Order order = order("ORDER-1", "0912345678", OrderStatus.DELIVERED);
        order.setUserId("CUSTOMER-1");
        DeliveryAssignment delivery = new DeliveryAssignment();
        delivery.setOrderId("ORDER-1");
        delivery.setCourierId("COURIER-1");
        delivery.setStatus(DeliveryStatus.DELIVERED);
        delivery.setDeliveredAt(1_790_000_000_000L);
        when(orderService.findById("ORDER-1")).thenReturn(Optional.of(order));
        when(deliveryService.findCurrentByOrderId("ORDER-1")).thenReturn(Optional.of(delivery));

        SupportChatResponse result = service.reply(
                new SupportChatRequest("trạng thái đơn", "ORDER-1"),
                "CUSTOMER-1",
                "Khách hàng",
                "0123456789"
        );

        assertThat(result.intent()).isEqualTo("ORDER_STATUS");
        assertThat(result.reply()).contains("đã được giao thành công");
        assertThat(result.reply()).doesNotContain("đăng nhập");
    }

    @Test
    void deliveryTimeUsesActualEtaAndCourierForOwner() {
        Order order = order("ORDER-1", "0912345678", OrderStatus.DELIVERING);
        order.setUserId("CUSTOMER-1");
        DeliveryAssignment delivery = new DeliveryAssignment();
        delivery.setOrderId("ORDER-1");
        delivery.setCourierId("COURIER-1");
        delivery.setStatus(DeliveryStatus.DELIVERING);
        delivery.setEstimatedDeliveryAt(1_790_000_000_000L);
        RestaurantCourier courier = new RestaurantCourier();
        courier.setId("COURIER-1");
        courier.setName("Nguyễn Minh Anh");

        when(orderService.findById("ORDER-1")).thenReturn(Optional.of(order));
        when(deliveryService.findCurrentByOrderId("ORDER-1")).thenReturn(Optional.of(delivery));
        when(deliveryService.findCourier("COURIER-1")).thenReturn(courier);

        SupportChatResponse result = service.reply(
                new SupportChatRequest("Khi nào giao tới?", "ORDER-1"),
                "CUSTOMER-1",
                "Khách hàng",
                null
        );

        assertThat(result.intent()).isEqualTo("DELIVERY_TIME");
        assertThat(result.reply()).contains("dự kiến tới lúc");
        assertThat(result.reply()).contains("Nguyễn Minh Anh");
    }

    @Test
    void vnpayQuestionReturnsSafeGuidanceWithoutOpeningTicket() {
        SupportChatResponse result = service.reply(
                new SupportChatRequest("Phiên thanh toán VNPay hết hạn thì làm sao?", null),
                "CUSTOMER-1",
                "Khách hàng",
                "0901234567"
        );

        assertThat(result.intent()).isEqualTo("PAYMENT_HELP");
        assertThat(result.handoffTarget()).isNull();
        assertThat(result.supportTicketId()).isNull();
        assertThat(result.reply()).contains("Thanh toán lại");
        assertThat(result.suggestions()).contains("Tôi bị trừ tiền");
    }

    @Test
    void voucherQuestionExplainsRealValidationRules() {
        SupportChatResponse result = service.reply(
                new SupportChatRequest("Mã giảm giá dùng ở đâu?", null),
                "CUSTOMER-1",
                "Khách hàng",
                "0901234567"
        );

        assertThat(result.intent()).isEqualTo("VOUCHER");
        assertThat(result.reply()).contains("giá trị đơn tối thiểu");
    }

    private Order order(String id, String phone, OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setCustomerName("Khách hàng");
        order.setCustomerPhone(phone);
        order.setUserId("CUSTOMER-1");
        order.setRestaurantId("aloha");
        order.setStatus(status);
        return order;
    }
}
