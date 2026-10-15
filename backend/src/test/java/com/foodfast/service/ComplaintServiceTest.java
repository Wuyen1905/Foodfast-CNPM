package com.foodfast.service;

import com.foodfast.dto.CreateComplaintRequest;
import com.foodfast.dto.ReviewComplaintRequest;
import com.foodfast.entity.Complaint;
import com.foodfast.entity.ComplaintAction;
import com.foodfast.entity.ComplaintCategory;
import com.foodfast.entity.ComplaintStatus;
import com.foodfast.entity.Order;
import com.foodfast.entity.OrderStatus;
import com.foodfast.exception.ForbiddenOperationException;
import com.foodfast.repository.ComplaintHistoryRepository;
import com.foodfast.repository.ComplaintRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComplaintServiceTest {
    @Mock ComplaintRepository complaintRepository;
    @Mock ComplaintHistoryRepository historyRepository;
    @Mock OrderRepository orderRepository;
    private ComplaintService service;

    @BeforeEach
    void setUp() {
        service = new ComplaintService(complaintRepository, historyRepository, orderRepository);
    }

    @Test
    void customerCannotComplainAboutAnotherCustomersOrder() {
        Order order = deliveredOrder();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.create("customer-2", new CreateComplaintRequest(
                order.getId(), ComplaintCategory.MISSING_ITEM,
                "Tôi bị thiếu một món trong đơn", List.of()
        ))).isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void restaurantCanResolveIncidentWithoutChangingOrderPayment() {
        Order order = deliveredOrder();
        Complaint complaint = complaint(order);
        when(complaintRepository.findById(complaint.getId())).thenReturn(Optional.of(complaint));
        when(complaintRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(historyRepository.findByComplaintIdOrderByCreatedAtAsc(complaint.getId())).thenReturn(List.of());

        var result = service.review(
                complaint.getId(),
                "RESTAURANT",
                "aloha",
                new ReviewComplaintRequest(ComplaintAction.RESOLVE,
                "Đã xác minh thiếu món và xin lỗi khách hàng"
        ));

        assertThat(result.status()).isEqualTo(ComplaintStatus.RESOLVED);
        assertThat(result.resolution()).contains("Đã xác minh thiếu món");
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatusPolicy.PAID);
        assertThat(order.getPartialRefundAmount()).isZero();
    }

    @Test
    void acceptsUploadedEvidencePathForWrongItemComplaint() {
        Order order = deliveredOrder();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(complaintRepository.existsByOrderId(any())).thenReturn(false);
        when(complaintRepository.save(any())).thenAnswer(invocation -> {
            Complaint complaint = invocation.getArgument(0);
            complaint.setId("COMPLAINT-UPLOAD");
            return complaint;
        });
        when(historyRepository.findByComplaintIdOrderByCreatedAtAsc("COMPLAINT-UPLOAD"))
                .thenReturn(List.of());

        var result = service.create("customer-1", new CreateComplaintRequest(
                order.getId(), ComplaintCategory.WRONG_ITEM,
                "Nhà hàng đã giao nhầm món cho tôi",
                List.of("/api/complaints/evidence/123e4567-e89b-12d3-a456-426614174000.jpg")
        ));

        assertThat(result.evidenceUrls()).hasSize(1);
    }

    @Test
    void customerCannotCreateAnotherComplaintAfterPreviousOneWasResolved() {
        Order order = deliveredOrder();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(complaintRepository.existsByOrderId(order.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.create("customer-1", new CreateComplaintRequest(
                order.getId(), ComplaintCategory.MISSING_ITEM,
                "Tôi muốn gửi thêm một báo cáo cho cùng đơn", List.of()
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Mỗi đơn hàng chỉ được tạo một báo cáo sự cố");
    }

    private Order deliveredOrder() {
        Order order = new Order();
        order.setId("ORDER-1");
        order.setUserId("customer-1");
        order.setRestaurantId("aloha");
        order.setStatus(OrderStatus.DELIVERED);
        order.setTotal(100_000);
        order.setPaymentMethod("vnpay");
        order.setPaymentStatus(PaymentStatusPolicy.PAID);
        return order;
    }

    private Complaint complaint(Order order) {
        Complaint complaint = new Complaint();
        complaint.setId("COMPLAINT-1");
        complaint.setOrderId(order.getId());
        complaint.setRestaurantId(order.getRestaurantId());
        complaint.setCustomerId(order.getUserId());
        complaint.setCategory(ComplaintCategory.MISSING_ITEM);
        complaint.setDescription("Thiếu một món trong đơn hàng");
        return complaint;
    }
}
