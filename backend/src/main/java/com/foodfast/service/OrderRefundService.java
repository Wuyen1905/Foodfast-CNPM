package com.foodfast.service;

import com.foodfast.dto.CompleteRefundRequest;
import com.foodfast.entity.Order;
import com.foodfast.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Completes manual refund reconciliation for cancellations or price adjustments. */
@Service
public class OrderRefundService {

    private final OrderRepository orderRepository;
    private final OrderGroupPaymentService orderGroupPaymentService;

    public OrderRefundService(
            OrderRepository orderRepository,
            OrderGroupPaymentService orderGroupPaymentService
    ) {
        this.orderRepository = orderRepository;
        this.orderGroupPaymentService = orderGroupPaymentService;
    }

    @Transactional
    public Order complete(String orderId, CompleteRefundRequest request) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng: " + orderId));
        String current = PaymentStatusPolicy.normalize(order.getPaymentStatus());
        if (!PaymentStatusPolicy.REFUND_PENDING.equals(current)
                && !PaymentStatusPolicy.PARTIAL_REFUND_PENDING.equals(current)) {
            throw new IllegalStateException("Đơn hàng không có khoản hoàn tiền đang chờ đối soát");
        }
        int refundAmount = order.getPartialRefundAmount();
        if (refundAmount <= 0 || refundAmount > order.getTotal()) {
            throw new IllegalStateException("Số tiền hoàn của đơn hàng không hợp lệ");
        }

        order.setPaymentStatus(refundAmount >= order.getTotal()
                ? PaymentStatusPolicy.REFUNDED
                : PaymentStatusPolicy.PARTIAL_REFUNDED);
        order.setRefundTransactionReference(request.transactionReference().trim());
        order.setRefundCompletedAt(System.currentTimeMillis());
        order.setRefundNote(request.note() == null || request.note().isBlank()
                ? null : request.note().trim());
        Order saved = orderRepository.save(order);
        orderGroupPaymentService.refreshForOrder(saved);
        return saved;
    }
}
