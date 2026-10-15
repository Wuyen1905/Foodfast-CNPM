package com.foodfast.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @Column(length = 60, nullable = false)
    private String id;

    @Column(nullable = false, name = "customer_name")
    @JsonProperty("name")
    private String customerName;

    @Column(nullable = false, name = "customer_phone")
    @JsonProperty("phone")
    private String customerPhone;

    private String customerEmail;

    @Lob
    @Column(length = Integer.MAX_VALUE)
    private String address;

    private String restaurant;

    private String restaurantId;

    private String userId;

    @Column(name = "order_group_id", length = 60)
    private String orderGroupId;

    @Column(name = "group_position")
    private Integer groupPosition;

    @Column(length = 80)
    private String paymentSessionId;

    @Column(name = "idempotency_key", length = 100, unique = true)
    private String idempotencyKey;

    @Column(name = "idempotency_request_hash", length = 64)
    @JsonIgnore
    private String idempotencyRequestHash;

    @Column(name = "payment_request_key", length = 100)
    @JsonIgnore
    private String paymentRequestKey;

    @Column(name = "payment_request_url", length = 2048)
    @JsonIgnore
    private String paymentRequestUrl;

    @Column(name = "payment_request_expires_at")
    @JsonIgnore
    private Instant paymentRequestExpiresAt;

    private String paymentMethod;

    private String paymentStatus;

    @Column(length = 1000)
    private String note;

    @Column(name = "internal_notes", length = 2000)
    @JsonProperty("internalNotes")
    private String internalNotes;

    @Column(name = "vnpay_transaction_id", length = 100, unique = true)
    @JsonProperty("vnpayTransactionId")
    private String vnpayTransactionId;

    @Column(name = "confirmed_at")
    @JsonProperty("confirmedAt")
    private Long confirmedAt;

    @Column(name = "preparing_at")
    @JsonProperty("preparingAt")
    private Long preparingAt;

    @Column(name = "ready_at")
    @JsonProperty("readyAt")
    private Long readyAt;

    @Column(name = "delivering_at")
    @JsonProperty("deliveringAt")
    private Long deliveringAt;

    @Column(name = "delivered_at")
    @JsonProperty("deliveredAt")
    private Long deliveredAt;

    @Column(name = "cancelled_at")
    @JsonProperty("cancelledAt")
    private Long cancelledAt;

    @Column(name = "paid_at")
    @JsonProperty("paidAt")
    private Long paidAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_actor", length = 20)
    private CancellationActor cancellationActor;

    @Column(name = "cancellation_actor_id", length = 100)
    private String cancellationActorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_reason_code", length = 50)
    private CancellationReasonCode cancellationReasonCode;

    @Column(name = "cancellation_reason_detail", length = 500)
    private String cancellationReasonDetail;

    @Column(name = "confirmed_by", length = 100)
    @JsonProperty("confirmedBy")
    private String confirmedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status = OrderStatus.PENDING;

    // Getter that returns status as string matching frontend expectations
    @JsonProperty("status")
    public String getStatusString() {
        return status == null ? OrderStatus.PENDING.apiValue() : status.apiValue();
    }

    @Column(nullable = false)
    private int total = 0;

    private int subtotal = 0;

    @Column(name = "shipping_fee")
    private int shippingFee = 15000;

    @Column(name = "tax_amount")
    private int taxAmount = 0;

    @Column(name = "discount_amount")
    private int discountAmount = 0;

    @Column(name = "voucher_code", length = 40)
    private String voucherCode;

    @Column(name = "voucher_name", length = 120)
    private String voucherName;

    /** Phạm vi ưu đãi được chụp lại tại thời điểm đặt đơn để đối soát không bị đổi theo cấu hình mới. */
    @Enumerated(EnumType.STRING)
    @Column(name = "voucher_scope", length = 20)
    private VoucherScope voucherScope;

    /** Phần giảm giá nhà hàng chịu; mã toàn hệ thống do FoodFast chịu nên bằng 0. */
    @Column(name = "restaurant_discount_amount", nullable = false)
    private int restaurantDiscountAmount;

    @Column(name = "partial_refund_amount")
    private int partialRefundAmount;

    @Column(name = "refund_transaction_reference", length = 100)
    private String refundTransactionReference;

    @Column(name = "refund_completed_at")
    private Long refundCompletedAt;

    @Column(name = "refund_note", length = 1200)
    private String refundNote;

    /** Phần tiền của đơn con đã được phân bổ từ giao dịch thanh toán nhóm. */
    @Column(name = "payment_allocated_amount", nullable = false)
    private int paymentAllocatedAmount;

    // Database columns (stored as Instant in DB)
    @Column(nullable = false, name = "created_at")
    @JsonIgnore
    private Instant createdAt;

    @Column(nullable = false, name = "updated_at")
    @JsonIgnore
    private Instant updatedAt;

    @JsonManagedReference
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = jakarta.persistence.FetchType.EAGER)
    private List<OrderItem> items = new ArrayList<>();

    public Order() {
    }

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public void recalculateTotal() {
        int subtotal = 0;
        for (OrderItem item : items) {
            if (item != null && item.getQty() > 0 && item.getPrice() > 0) {
                subtotal += item.getQty() * item.getPrice();
            }
        }
        this.subtotal = subtotal;
        this.shippingFee = 15000;
        // Giá món đã là giá bán cho khách; FoodFast không tự cộng thêm thuế 10%.
        this.taxAmount = 0;
        this.discountAmount = Math.min(Math.max(0, discountAmount), subtotal);
        this.total = Math.max(0, subtotal + shippingFee - discountAmount);
    }

    public void setItems(List<OrderItem> items) {
        this.items.clear();
        if (items != null) {
            items.forEach(this::addItem);
        }
    }

    public void addItem(OrderItem item) {
        if (item == null) {
            return;
        }
        item.setOrder(this);
        this.items.add(item);
    }

    public void removeItem(OrderItem item) {
        if (item == null) {
            return;
        }
        item.setOrder(null);
        this.items.remove(item);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRestaurant() {
        return restaurant;
    }

    public void setRestaurant(String restaurant) {
        this.restaurant = restaurant;
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getOrderGroupId() { return orderGroupId; }
    public void setOrderGroupId(String orderGroupId) { this.orderGroupId = orderGroupId; }
    public Integer getGroupPosition() { return groupPosition; }
    public void setGroupPosition(Integer groupPosition) { this.groupPosition = groupPosition; }

    public String getPaymentSessionId() {
        return paymentSessionId;
    }

    public void setPaymentSessionId(String paymentSessionId) {
        this.paymentSessionId = paymentSessionId;
    }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getIdempotencyRequestHash() { return idempotencyRequestHash; }
    public void setIdempotencyRequestHash(String idempotencyRequestHash) {
        this.idempotencyRequestHash = idempotencyRequestHash;
    }
    public String getPaymentRequestKey() { return paymentRequestKey; }
    public void setPaymentRequestKey(String paymentRequestKey) { this.paymentRequestKey = paymentRequestKey; }
    public String getPaymentRequestUrl() { return paymentRequestUrl; }
    public void setPaymentRequestUrl(String paymentRequestUrl) { this.paymentRequestUrl = paymentRequestUrl; }
    public Instant getPaymentRequestExpiresAt() { return paymentRequestExpiresAt; }
    public void setPaymentRequestExpiresAt(Instant paymentRequestExpiresAt) {
        this.paymentRequestExpiresAt = paymentRequestExpiresAt;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getInternalNotes() {
        return internalNotes;
    }

    public void setInternalNotes(String internalNotes) {
        this.internalNotes = internalNotes;
    }

    public String getVnpayTransactionId() {
        return vnpayTransactionId;
    }

    public void setVnpayTransactionId(String vnpayTransactionId) {
        this.vnpayTransactionId = vnpayTransactionId;
    }

    public Long getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Long confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public Long getPreparingAt() { return preparingAt; }
    public void setPreparingAt(Long preparingAt) { this.preparingAt = preparingAt; }
    public Long getReadyAt() { return readyAt; }
    public void setReadyAt(Long readyAt) { this.readyAt = readyAt; }
    public Long getDeliveringAt() { return deliveringAt; }
    public void setDeliveringAt(Long deliveringAt) { this.deliveringAt = deliveringAt; }
    public Long getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(Long deliveredAt) { this.deliveredAt = deliveredAt; }

    public Long getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Long cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public Long getPaidAt() { return paidAt; }
    public void setPaidAt(Long paidAt) { this.paidAt = paidAt; }

    public CancellationActor getCancellationActor() {
        return cancellationActor;
    }

    public void setCancellationActor(CancellationActor cancellationActor) {
        this.cancellationActor = cancellationActor;
    }

    public String getCancellationActorId() {
        return cancellationActorId;
    }

    public void setCancellationActorId(String cancellationActorId) {
        this.cancellationActorId = cancellationActorId;
    }

    public CancellationReasonCode getCancellationReasonCode() {
        return cancellationReasonCode;
    }

    public void setCancellationReasonCode(CancellationReasonCode cancellationReasonCode) {
        this.cancellationReasonCode = cancellationReasonCode;
    }

    public String getCancellationReasonDetail() {
        return cancellationReasonDetail;
    }

    public void setCancellationReasonDetail(String cancellationReasonDetail) {
        this.cancellationReasonDetail = cancellationReasonDetail;
    }

    public String getConfirmedBy() {
        return confirmedBy;
    }

    public void setConfirmedBy(String confirmedBy) {
        this.confirmedBy = confirmedBy;
    }

    @JsonIgnore
    public OrderStatus getStatusEnum() {
        return status;
    }

    @JsonIgnore
    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    // Setter that accepts string status from frontend
    public void setStatus(String statusString) {
        if (statusString == null || statusString.trim().isEmpty()) {
            this.status = OrderStatus.PENDING;
            return;
        }
        // Map frontend string to enum
        String normalized = statusString.trim();
        this.status = switch (normalized.toLowerCase()) {
            case "pending" -> OrderStatus.PENDING;
            case "confirmed" -> OrderStatus.CONFIRMED;
            case "in progress", "preparing" -> OrderStatus.PREPARING;
            case "ready" -> OrderStatus.READY;
            case "delivering" -> OrderStatus.DELIVERING;
            case "delivered" -> OrderStatus.DELIVERED;
            case "cancelled", "canceled" -> OrderStatus.CANCELLED;
            default -> OrderStatus.PENDING;
        };
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = Math.max(total, 0);
    }

    public int getSubtotal() { return subtotal; }
    public int getShippingFee() { return shippingFee; }
    public int getTaxAmount() { return taxAmount; }
    public int getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(int discountAmount) { this.discountAmount = Math.max(0, discountAmount); }
    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }
    public String getVoucherName() { return voucherName; }
    public void setVoucherName(String voucherName) { this.voucherName = voucherName; }
    public VoucherScope getVoucherScope() { return voucherScope; }
    public void setVoucherScope(VoucherScope voucherScope) { this.voucherScope = voucherScope; }
    public int getRestaurantDiscountAmount() { return restaurantDiscountAmount; }
    public void setRestaurantDiscountAmount(int restaurantDiscountAmount) {
        this.restaurantDiscountAmount = Math.max(0, restaurantDiscountAmount);
    }
    public int getPartialRefundAmount() { return partialRefundAmount; }
    public void setPartialRefundAmount(int partialRefundAmount) { this.partialRefundAmount = Math.max(0, partialRefundAmount); }
    public String getRefundTransactionReference() { return refundTransactionReference; }
    public void setRefundTransactionReference(String refundTransactionReference) { this.refundTransactionReference = refundTransactionReference; }
    public Long getRefundCompletedAt() { return refundCompletedAt; }
    public void setRefundCompletedAt(Long refundCompletedAt) { this.refundCompletedAt = refundCompletedAt; }
    public String getRefundNote() { return refundNote; }
    public void setRefundNote(String refundNote) { this.refundNote = refundNote; }
    public int getPaymentAllocatedAmount() { return paymentAllocatedAmount; }
    public void setPaymentAllocatedAmount(int paymentAllocatedAmount) { this.paymentAllocatedAmount = Math.max(0, paymentAllocatedAmount); }
    
    // Backward compatibility getter
    public BigDecimal getTotalAmount() {
        return BigDecimal.valueOf(total);
    }

    // JSON getters (return milliseconds as Long for frontend)
    @JsonProperty("createdAt")
    public Long getCreatedAt() {
        if (createdAt != null) {
            return createdAt.toEpochMilli();
        }
        return System.currentTimeMillis();
    }

    @JsonProperty("updatedAt")
    public Long getUpdatedAt() {
        if (updatedAt != null) {
            return updatedAt.toEpochMilli();
        }
        return System.currentTimeMillis();
    }

    // Internal getters for Instant (for database operations)
    @JsonIgnore
    public Instant getCreatedAtInstant() {
        return createdAt;
    }

    @JsonIgnore
    public Instant getUpdatedAtInstant() {
        return updatedAt;
    }

    // Setters that accept both Instant and Long
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setCreatedAt(Long createdAtMillis) {
        if (createdAtMillis != null) {
            this.createdAt = Instant.ofEpochMilli(createdAtMillis);
        }
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setUpdatedAt(Long updatedAtMillis) {
        if (updatedAtMillis != null) {
            this.updatedAt = Instant.ofEpochMilli(updatedAtMillis);
        }
    }

    public List<OrderItem> getItems() {
        if (items == null) {
            items = new ArrayList<>();
        }
        return items;
    }

    public void setCustomerAndRestaurantDefaults() {
        if (Objects.nonNull(this.restaurant)) {
            this.restaurant = this.restaurant.trim();
        }
        if (Objects.nonNull(this.restaurantId)) {
            this.restaurantId = this.restaurantId.trim();
        }
    }
}

