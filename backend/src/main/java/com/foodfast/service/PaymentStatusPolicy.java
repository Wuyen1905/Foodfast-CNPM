package com.foodfast.service;

import com.foodfast.entity.Order;

import java.util.Locale;

/**
 * Business rules for the payment lifecycle. Delivery/order status and payment
 * status are intentionally kept independent.
 */
public final class PaymentStatusPolicy {

    public static final String PENDING = "pending";
    public static final String PAID = "paid";
    public static final String FAILED = "failed";
    public static final String EXPIRED = "expired";
    public static final String REFUND_PENDING = "refund_pending";
    public static final String PARTIAL_REFUND_PENDING = "partial_refund_pending";
    public static final String REFUNDED = "refunded";
    public static final String PARTIAL_REFUNDED = "partial_refunded";

    private PaymentStatusPolicy() {
    }

    public static String initialStatus(String paymentMethod) {
        return PENDING;
    }

    public static String normalize(String status) {
        if (status == null || status.isBlank()) {
            return PENDING;
        }
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "completed", "success", "successful", "đã thanh toán" -> PAID;
            case "failed", "failure", "thất bại" -> FAILED;
            case "expired", "hết hạn" -> EXPIRED;
            case "refund_pending", "đang chờ hoàn tiền" -> REFUND_PENDING;
            case "refunded", "đã hoàn tiền" -> REFUNDED;
            case "partial_refund_pending", "đang chờ hoàn một phần" -> PARTIAL_REFUND_PENDING;
            case "partial_refunded", "đã hoàn một phần" -> PARTIAL_REFUNDED;
            case "đang chờ phê duyệt", "pending" -> PENDING;
            default -> normalized;
        };
    }

    public static boolean isVNPay(Order order) {
        return order != null
                && order.getPaymentMethod() != null
                && "vnpay".equalsIgnoreCase(order.getPaymentMethod().trim());
    }

    public static boolean isCashOnDelivery(Order order) {
        if (order == null || order.getPaymentMethod() == null) {
            return false;
        }
        String method = order.getPaymentMethod().trim().toLowerCase(Locale.ROOT);
        return "cod".equals(method) || "cash".equals(method) || "cash_on_delivery".equals(method);
    }

    public static String statusAfterSuccessfulDelivery(Order order) {
        String current = normalize(order == null ? null : order.getPaymentStatus());
        if (isCashOnDelivery(order) && PENDING.equals(current)) {
            return PAID;
        }
        return current;
    }

    public static boolean isPaid(Order order) {
        return order != null && PAID.equals(normalize(order.getPaymentStatus()));
    }

    public static boolean isRetryable(Order order) {
        if (order == null) return false;
        String status = normalize(order.getPaymentStatus());
        return PENDING.equals(status) || FAILED.equals(status) || EXPIRED.equals(status);
    }

    public static boolean isReadyForRestaurant(Order order) {
        return !isVNPay(order) || isPaid(order);
    }
}
