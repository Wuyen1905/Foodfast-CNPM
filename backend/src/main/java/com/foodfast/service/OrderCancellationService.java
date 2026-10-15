package com.foodfast.service;

import com.foodfast.dto.CancelOrderRequest;
import com.foodfast.entity.CancellationActor;
import com.foodfast.entity.CancellationReasonCode;
import com.foodfast.entity.Order;
import com.foodfast.entity.OrderStatus;
import com.foodfast.event.OrderEventPublisher;
import com.foodfast.exception.ForbiddenOperationException;
import com.foodfast.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.EnumSet;
import java.util.Set;

@Service
public class OrderCancellationService {

    private static final Set<CancellationReasonCode> CUSTOMER_REASONS = EnumSet.of(
            CancellationReasonCode.CUSTOMER_ORDERED_BY_MISTAKE,
            CancellationReasonCode.CUSTOMER_WRONG_ADDRESS,
            CancellationReasonCode.CUSTOMER_CHANGED_MIND,
            CancellationReasonCode.CUSTOMER_WAIT_TOO_LONG,
            CancellationReasonCode.OTHER
    );

    private static final Set<CancellationReasonCode> RESTAURANT_REASONS = EnumSet.of(
            CancellationReasonCode.RESTAURANT_OUT_OF_STOCK,
            CancellationReasonCode.RESTAURANT_OVERLOADED,
            CancellationReasonCode.RESTAURANT_CLOSED,
            CancellationReasonCode.RESTAURANT_CANNOT_DELIVER,
            CancellationReasonCode.OTHER
    );

    private final OrderRepository orderRepository;
    private final OrderEventPublisher orderEventPublisher;
    private final NotificationService notificationService;
    private final VoucherService voucherService;
    private final OrderStatusHistoryService orderStatusHistoryService;
    private final CustomerNotificationService customerNotificationService;
    private final OrderGroupPaymentService orderGroupPaymentService;

    public OrderCancellationService(
            OrderRepository orderRepository,
            OrderEventPublisher orderEventPublisher,
            NotificationService notificationService,
            VoucherService voucherService,
            OrderStatusHistoryService orderStatusHistoryService,
            CustomerNotificationService customerNotificationService,
            OrderGroupPaymentService orderGroupPaymentService
    ) {
        this.orderRepository = orderRepository;
        this.orderEventPublisher = orderEventPublisher;
        this.notificationService = notificationService;
        this.voucherService = voucherService;
        this.orderStatusHistoryService = orderStatusHistoryService;
        this.customerNotificationService = customerNotificationService;
        this.orderGroupPaymentService = orderGroupPaymentService;
    }

    @Transactional
    public Order cancel(
            String orderId,
            CancellationActor actor,
            String actorId,
            CancelOrderRequest request
    ) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng: " + orderId));

        validateRequest(order, actor, actorId, request);
        OrderStatusPolicy.requireTransition(order.getStatusEnum(), OrderStatus.CANCELLED);

        OrderStatus previousStatus = order.getStatusEnum();
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(System.currentTimeMillis());
        order.setCancellationActor(actor);
        order.setCancellationActorId(actorId.trim());
        order.setCancellationReasonCode(request.reasonCode);
        order.setCancellationReasonDetail(normalizeDetail(request.detail));

        if (PaymentStatusPolicy.isPaid(order)) {
            order.setPaymentStatus(PaymentStatusPolicy.REFUND_PENDING);
            order.setPartialRefundAmount(order.getTotal());
        }

        Order saved = orderRepository.save(order);
        orderGroupPaymentService.refreshForOrder(saved);
        String historyReason = StringUtils.hasText(request.detail)
                ? request.detail.trim() : request.reasonCode.name();
        orderStatusHistoryService.record(
                saved.getId(), previousStatus, OrderStatus.CANCELLED, actorId,
                actor.name(), "CANCELLATION", historyReason
        );
        customerNotificationService.orderStatusChanged(saved, previousStatus);
        voucherService.releaseForOrder(saved.getId());
        orderEventPublisher.publishOrderUpdate(saved);
        try {
            notificationService.createNotification(saved);
        } catch (RuntimeException error) {
            System.err.println("[OrderCancellationService] Không thể tạo thông báo: " + error.getMessage());
        }
        return saved;
    }

    private void validateRequest(
            Order order,
            CancellationActor actor,
            String actorId,
            CancelOrderRequest request
    ) {
        if (request == null || actor == null || request.reasonCode == null
                || !StringUtils.hasText(actorId)) {
            throw new IllegalArgumentException("Thiếu người thực hiện hoặc lý do hủy đơn");
        }

        if (request.reasonCode == CancellationReasonCode.OTHER
                && (!StringUtils.hasText(request.detail) || request.detail.trim().length() < 5)) {
            throw new IllegalArgumentException("Vui lòng mô tả lý do khác ít nhất 5 ký tự");
        }

        switch (actor) {
            case CUSTOMER -> validateCustomer(order, actorId, request);
            case RESTAURANT -> validateRestaurant(order, actorId, request);
            case ADMIN -> { }
            case SYSTEM -> throw new ForbiddenOperationException(
                    "Không thể giả lập thao tác hủy tự động từ trình duyệt"
            );
        }
    }

    private void validateCustomer(Order order, String actorId, CancelOrderRequest request) {
        if (!actorId.trim().equals(order.getUserId())) {
            throw new ForbiddenOperationException("Bạn chỉ được hủy đơn hàng của chính mình");
        }
        if (!CUSTOMER_REASONS.contains(request.reasonCode)) {
            throw new IllegalArgumentException("Lý do hủy không dành cho khách hàng");
        }
        if (order.getStatusEnum() != OrderStatus.PENDING) {
            throw new IllegalStateException(
                    "Nhà hàng đã tiếp nhận đơn. Vui lòng liên hệ hỗ trợ nếu cần hủy"
            );
        }
    }

    private void validateRestaurant(Order order, String actorId, CancelOrderRequest request) {
        if (!RestaurantIdentityPolicy.refersToSameRestaurant(
                actorId.trim(),
                order.getRestaurantId()
        )) {
            throw new ForbiddenOperationException("Nhà hàng chỉ được hủy đơn thuộc nhà hàng của mình");
        }
        if (!RESTAURANT_REASONS.contains(request.reasonCode)) {
            throw new IllegalArgumentException("Lý do từ chối không dành cho nhà hàng");
        }
    }

    private String normalizeDetail(String detail) {
        return StringUtils.hasText(detail) ? detail.trim() : null;
    }
}
