package com.foodfast.dto;

import com.foodfast.entity.Order;
import com.foodfast.entity.OrderGroup;

import java.util.List;

public record OrderGroupResponse(
        String id,
        String userId,
        String customerName,
        String customerPhone,
        String customerEmail,
        String address,
        String note,
        String paymentMethod,
        String paymentStatus,
        int subtotal,
        int shippingFee,
        int taxAmount,
        int discountAmount,
        int total,
        int payableAmount,
        int partialRefundAmount,
        int refundedAmount,
        String vnpayTransactionId,
        Long paidAt,
        int orderCount,
        Long createdAt,
        Long updatedAt,
        List<Order> orders
) {
    public static OrderGroupResponse from(OrderGroup group, List<Order> orders) {
        return new OrderGroupResponse(
                group.getId(), group.getUserId(), group.getCustomerName(),
                group.getCustomerPhone(), group.getCustomerEmail(), group.getAddress(),
                group.getNote(), group.getPaymentMethod(), group.getPaymentStatus(),
                group.getSubtotal(), group.getShippingFee(), group.getTaxAmount(),
                group.getDiscountAmount(), group.getTotal(), group.getPayableAmount(),
                group.getPartialRefundAmount(), group.getRefundedAmount(),
                group.getVnpayTransactionId(), group.getPaidAt(), group.getOrderCount(),
                group.getCreatedAt(), group.getUpdatedAt(), List.copyOf(orders)
        );
    }
}
