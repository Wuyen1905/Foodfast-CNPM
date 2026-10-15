package com.foodfast.service;

import com.foodfast.entity.Order;
import com.foodfast.entity.OrderGroup;
import com.foodfast.entity.OrderStatus;
import com.foodfast.repository.OrderGroupRepository;
import com.foodfast.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Đồng bộ sổ thanh toán của nhóm từ các đơn con. Tổng ban đầu được giữ lại để
 * truy vết, còn payableAmount là số tiền thực tế phải thu sau khi một nhà hàng
 * từ chối trước lúc thanh toán.
 */
@Service
public class OrderGroupPaymentService {

    private final OrderGroupRepository groupRepository;
    private final OrderRepository orderRepository;

    public OrderGroupPaymentService(
            OrderGroupRepository groupRepository,
            OrderRepository orderRepository
    ) {
        this.groupRepository = groupRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public void refreshForOrder(Order order) {
        if (order != null && order.getOrderGroupId() != null && !order.getOrderGroupId().isBlank()) {
            refresh(order.getOrderGroupId());
        }
    }

    @Transactional
    public OrderGroup refresh(String groupId) {
        OrderGroup group = groupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhóm đặt hàng: " + groupId));
        List<Order> orders = orderRepository.findByOrderGroupIdOrderByGroupPositionAsc(groupId);
        int activeTotal = orders.stream()
                .filter(order -> order.getStatusEnum() != OrderStatus.CANCELLED)
                .mapToInt(Order::getTotal)
                .sum();

        String current = PaymentStatusPolicy.normalize(group.getPaymentStatus());
        if (!isSettled(current)) {
            if (group.getPayableAmount() != activeTotal) {
                group.setPayableAmount(activeTotal);
                // Một phiên đã ký theo số tiền cũ không còn hợp lệ.
                group.setPaymentSessionId(null);
                group.setPaymentRequestKey(null);
                group.setPaymentRequestUrl(null);
                group.setPaymentRequestExpiresAt(null);
            }
            group.setPaymentStatus(activeTotal == 0
                    ? PaymentStatusPolicy.FAILED : PaymentStatusPolicy.PENDING);
        }

        int approvedRefund = orders.stream().mapToInt(Order::getPartialRefundAmount).sum();
        int completedRefund = orders.stream()
                .filter(order -> {
                    String status = PaymentStatusPolicy.normalize(order.getPaymentStatus());
                    return PaymentStatusPolicy.REFUNDED.equals(status)
                            || PaymentStatusPolicy.PARTIAL_REFUNDED.equals(status);
                })
                .mapToInt(Order::getPartialRefundAmount)
                .sum();
        boolean refundPending = orders.stream().anyMatch(order -> {
            String status = PaymentStatusPolicy.normalize(order.getPaymentStatus());
            return PaymentStatusPolicy.REFUND_PENDING.equals(status)
                    || PaymentStatusPolicy.PARTIAL_REFUND_PENDING.equals(status);
        });
        group.setPartialRefundAmount(Math.min(group.getPayableAmount(), approvedRefund));
        group.setRefundedAmount(Math.min(group.getPayableAmount(), completedRefund));

        if (refundPending) {
            group.setPaymentStatus(group.getPartialRefundAmount() >= group.getPayableAmount()
                    ? PaymentStatusPolicy.REFUND_PENDING
                    : PaymentStatusPolicy.PARTIAL_REFUND_PENDING);
        } else if (group.getRefundedAmount() > 0) {
            group.setPaymentStatus(group.getRefundedAmount() >= group.getPayableAmount()
                    ? PaymentStatusPolicy.REFUNDED
                    : PaymentStatusPolicy.PARTIAL_REFUNDED);
        } else if (isCashOnDelivery(group) && !orders.isEmpty()) {
            boolean allFinished = orders.stream()
                    .filter(order -> order.getStatusEnum() != OrderStatus.CANCELLED)
                    .allMatch(order -> order.getStatusEnum() == OrderStatus.DELIVERED
                            && PaymentStatusPolicy.isPaid(order));
            if (activeTotal > 0 && allFinished) {
                group.setPaymentStatus(PaymentStatusPolicy.PAID);
                if (group.getPaidAt() == null) group.setPaidAt(System.currentTimeMillis());
            }
        }
        return groupRepository.save(group);
    }

    private boolean isSettled(String status) {
        return PaymentStatusPolicy.PAID.equals(status)
                || PaymentStatusPolicy.REFUND_PENDING.equals(status)
                || PaymentStatusPolicy.PARTIAL_REFUND_PENDING.equals(status)
                || PaymentStatusPolicy.REFUNDED.equals(status)
                || PaymentStatusPolicy.PARTIAL_REFUNDED.equals(status);
    }

    private boolean isCashOnDelivery(OrderGroup group) {
        if (group.getPaymentMethod() == null) return false;
        String method = group.getPaymentMethod().trim().toLowerCase();
        return "cod".equals(method) || "cash".equals(method) || "cash_on_delivery".equals(method);
    }
}
