package com.foodfast.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.UUID;

/**
 * Connects one order to one restaurant courier and enforces delivery state
 * transitions independently from the order preparation lifecycle.
 */
@Entity
@Table(name = "delivery_assignments")
public class DeliveryAssignment {

    @Id
    @Column(length = 50, nullable = false)
    private String id;

    @Column(name = "order_id", length = 60, nullable = false)
    private String orderId;

    @Column(name = "restaurant_id", length = 50, nullable = false)
    private String restaurantId;

    @Column(name = "courier_id", length = 50, nullable = false)
    private String courierId;

    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    private DeliveryStatus status = DeliveryStatus.ASSIGNED;

    @Column(name = "estimated_delivery_at")
    private Long estimatedDeliveryAt;

    @Column(name = "delivery_note", length = 1000)
    private String deliveryNote;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "assigned_at", nullable = false)
    private long assignedAt;

    @Column(name = "started_at")
    private Long startedAt;

    @Column(name = "delivered_at")
    private Long deliveredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "completion_source", length = 30)
    private DeliveryCompletionSource completionSource;

    @Column(name = "completion_note", length = 1000)
    private String completionNote;

    @Column(name = "failed_at")
    private Long failedAt;

    @Column(name = "cancelled_at")
    private Long cancelledAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    private Long version;

    @PrePersist
    void prePersist() {
        long now = System.currentTimeMillis();
        if (id == null || id.isBlank()) {
            id = "DELIVERY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        assignedAt = assignedAt == 0 ? now : assignedAt;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = System.currentTimeMillis();
    }

    public void start() {
        requireStatus(DeliveryStatus.ASSIGNED, "Chỉ phân công mới có thể bắt đầu giao");
        status = DeliveryStatus.DELIVERING;
        startedAt = System.currentTimeMillis();
        updatedAt = startedAt;
    }

    public void complete(DeliveryCompletionSource source, String note) {
        requireStatus(DeliveryStatus.DELIVERING, "Chỉ đơn đang giao mới có thể hoàn thành");
        if (source == null) {
            throw new IllegalArgumentException("Phải xác định bên xác nhận đã nhận hàng");
        }
        if (source == DeliveryCompletionSource.RESTAURANT_OVERRIDE
                && (note == null || note.trim().length() < 10)) {
            throw new IllegalArgumentException("Lý do hoàn tất ngoại lệ phải có ít nhất 10 ký tự");
        }
        status = DeliveryStatus.DELIVERED;
        completionSource = source;
        completionNote = note == null || note.isBlank() ? null : note.trim();
        deliveredAt = System.currentTimeMillis();
        updatedAt = deliveredAt;
    }

    public void fail(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Phải nhập lý do giao hàng thất bại");
        }
        if (status != DeliveryStatus.ASSIGNED && status != DeliveryStatus.DELIVERING) {
            throw new IllegalStateException("Chỉ phân công đang hoạt động mới có thể báo thất bại");
        }
        status = DeliveryStatus.FAILED;
        failureReason = reason.trim();
        failedAt = System.currentTimeMillis();
        updatedAt = failedAt;
    }

    public void cancel() {
        requireStatus(DeliveryStatus.ASSIGNED, "Chỉ phân công chưa bắt đầu mới có thể hủy");
        status = DeliveryStatus.CANCELLED;
        cancelledAt = System.currentTimeMillis();
        updatedAt = cancelledAt;
    }

    private void requireStatus(DeliveryStatus required, String message) {
        if (status != required) {
            throw new IllegalStateException(message);
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getRestaurantId() { return restaurantId; }
    public void setRestaurantId(String restaurantId) { this.restaurantId = restaurantId; }
    public String getCourierId() { return courierId; }
    public void setCourierId(String courierId) { this.courierId = courierId; }
    public DeliveryStatus getStatus() { return status; }
    public void setStatus(DeliveryStatus status) { this.status = status; }
    public Long getEstimatedDeliveryAt() { return estimatedDeliveryAt; }
    public void setEstimatedDeliveryAt(Long estimatedDeliveryAt) { this.estimatedDeliveryAt = estimatedDeliveryAt; }
    public String getDeliveryNote() { return deliveryNote; }
    public void setDeliveryNote(String deliveryNote) { this.deliveryNote = deliveryNote; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public long getAssignedAt() { return assignedAt; }
    public void setAssignedAt(long assignedAt) { this.assignedAt = assignedAt; }
    public Long getStartedAt() { return startedAt; }
    public void setStartedAt(Long startedAt) { this.startedAt = startedAt; }
    public Long getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(Long deliveredAt) { this.deliveredAt = deliveredAt; }
    public DeliveryCompletionSource getCompletionSource() { return completionSource; }
    public void setCompletionSource(DeliveryCompletionSource completionSource) { this.completionSource = completionSource; }
    public String getCompletionNote() { return completionNote; }
    public void setCompletionNote(String completionNote) { this.completionNote = completionNote; }
    public Long getFailedAt() { return failedAt; }
    public void setFailedAt(Long failedAt) { this.failedAt = failedAt; }
    public Long getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Long cancelledAt) { this.cancelledAt = cancelledAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    public Long getVersion() { return version; }
}
