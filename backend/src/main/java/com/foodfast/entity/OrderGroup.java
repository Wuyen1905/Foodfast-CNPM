package com.foodfast.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Một lần xác nhận giỏ hàng của khách. Mỗi nhà hàng trong giỏ tạo thành một
 * {@link Order} con độc lập, còn thực thể này dùng để khách xem và đối soát
 * toàn bộ lần đặt hàng.
 */
@Entity
@Table(name = "order_groups")
public class OrderGroup {

    @Id
    @Column(length = 60, nullable = false)
    private String id;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_phone", nullable = false, length = 30)
    private String customerPhone;

    @Column(name = "customer_email")
    private String customerEmail;

    @Lob
    @Column(nullable = false, length = Integer.MAX_VALUE)
    private String address;

    @Column(length = 1000)
    private String note;

    @Column(name = "payment_method", nullable = false, length = 30)
    private String paymentMethod;

    @Column(name = "payment_status", nullable = false, length = 30)
    private String paymentStatus;

    @Column(nullable = false)
    private int subtotal;

    @Column(name = "shipping_fee", nullable = false)
    private int shippingFee;

    @Column(name = "tax_amount", nullable = false)
    private int taxAmount;

    @Column(name = "discount_amount", nullable = false)
    private int discountAmount;

    @Column(nullable = false)
    private int total;

    /** Số tiền còn phải thu sau khi loại các đơn con bị từ chối trước thanh toán. */
    @Column(name = "payable_amount", nullable = false)
    private int payableAmount;

    @Column(name = "payment_session_id", length = 80, unique = true)
    private String paymentSessionId;

    @Column(name = "payment_request_key", length = 100)
    private String paymentRequestKey;

    @Column(name = "payment_request_url", length = 2048)
    private String paymentRequestUrl;

    @Column(name = "payment_request_expires_at")
    private Instant paymentRequestExpiresAt;

    @Column(name = "vnpay_transaction_id", length = 100, unique = true)
    private String vnpayTransactionId;

    @Column(name = "paid_at")
    private Long paidAt;

    @Column(name = "partial_refund_amount", nullable = false)
    private int partialRefundAmount;

    @Column(name = "refunded_amount", nullable = false)
    private int refundedAmount;

    @Column(name = "order_count", nullable = false)
    private int orderCount;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public int getSubtotal() { return subtotal; }
    public void setSubtotal(int subtotal) { this.subtotal = subtotal; }
    public int getShippingFee() { return shippingFee; }
    public void setShippingFee(int shippingFee) { this.shippingFee = shippingFee; }
    public int getTaxAmount() { return taxAmount; }
    public void setTaxAmount(int taxAmount) { this.taxAmount = taxAmount; }
    public int getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(int discountAmount) { this.discountAmount = discountAmount; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public int getPayableAmount() { return payableAmount; }
    public void setPayableAmount(int payableAmount) { this.payableAmount = Math.max(0, payableAmount); }
    public String getPaymentSessionId() { return paymentSessionId; }
    public void setPaymentSessionId(String paymentSessionId) { this.paymentSessionId = paymentSessionId; }
    public String getPaymentRequestKey() { return paymentRequestKey; }
    public void setPaymentRequestKey(String paymentRequestKey) { this.paymentRequestKey = paymentRequestKey; }
    public String getPaymentRequestUrl() { return paymentRequestUrl; }
    public void setPaymentRequestUrl(String paymentRequestUrl) { this.paymentRequestUrl = paymentRequestUrl; }
    public Instant getPaymentRequestExpiresAt() { return paymentRequestExpiresAt; }
    public void setPaymentRequestExpiresAt(Instant paymentRequestExpiresAt) { this.paymentRequestExpiresAt = paymentRequestExpiresAt; }
    public String getVnpayTransactionId() { return vnpayTransactionId; }
    public void setVnpayTransactionId(String vnpayTransactionId) { this.vnpayTransactionId = vnpayTransactionId; }
    public Long getPaidAt() { return paidAt; }
    public void setPaidAt(Long paidAt) { this.paidAt = paidAt; }
    public int getPartialRefundAmount() { return partialRefundAmount; }
    public void setPartialRefundAmount(int partialRefundAmount) { this.partialRefundAmount = Math.max(0, partialRefundAmount); }
    public int getRefundedAmount() { return refundedAmount; }
    public void setRefundedAmount(int refundedAmount) { this.refundedAmount = Math.max(0, refundedAmount); }
    public int getOrderCount() { return orderCount; }
    public void setOrderCount(int orderCount) { this.orderCount = orderCount; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getRequestHash() { return requestHash; }
    public void setRequestHash(String requestHash) { this.requestHash = requestHash; }
    public Long getCreatedAt() { return createdAt == null ? null : createdAt.toEpochMilli(); }
    public Instant getCreatedAtInstant() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Long getUpdatedAt() { return updatedAt == null ? null : updatedAt.toEpochMilli(); }
    public Instant getUpdatedAtInstant() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
