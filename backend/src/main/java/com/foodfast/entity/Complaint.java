package com.foodfast.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "complaints")
public class Complaint {
    @Id
    @Column(length = 50, nullable = false)
    private String id;

    @Column(name = "order_id", length = 60, nullable = false)
    private String orderId;

    @Column(name = "restaurant_id", length = 50, nullable = false)
    private String restaurantId;

    @Column(name = "customer_id", length = 60, nullable = false)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private ComplaintCategory category;

    @Column(length = 1200, nullable = false)
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "complaint_evidence_urls", joinColumns = @JoinColumn(name = "complaint_id"))
    @Column(name = "evidence_url", length = 500, nullable = false)
    private List<String> evidenceUrls = new ArrayList<>();

    @Column(name = "requested_refund_amount", nullable = false)
    private int requestedRefundAmount;

    @Column(name = "approved_refund_amount", nullable = false)
    private int approvedRefundAmount;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ComplaintStatus status = ComplaintStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_status", length = 20, nullable = false)
    private RefundStatus refundStatus = RefundStatus.NONE;

    @Column(name = "handler_role", length = 20)
    private String handlerRole;

    @Column(name = "handler_id", length = 60)
    private String handlerId;

    @Column(length = 1200)
    private String resolution;

    @Column(name = "refund_transaction_reference", length = 100)
    private String refundTransactionReference;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Column(name = "resolved_at")
    private Long resolvedAt;

    @PrePersist
    void prePersist() {
        long now = System.currentTimeMillis();
        if (id == null || id.isBlank()) {
            id = "COMPLAINT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        if (createdAt == 0) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() { updatedAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getRestaurantId() { return restaurantId; }
    public void setRestaurantId(String restaurantId) { this.restaurantId = restaurantId; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public ComplaintCategory getCategory() { return category; }
    public void setCategory(ComplaintCategory category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<String> getEvidenceUrls() { return evidenceUrls; }
    public void setEvidenceUrls(List<String> evidenceUrls) { this.evidenceUrls = evidenceUrls == null ? new ArrayList<>() : new ArrayList<>(evidenceUrls); }
    public int getRequestedRefundAmount() { return requestedRefundAmount; }
    public void setRequestedRefundAmount(int value) { requestedRefundAmount = Math.max(0, value); }
    public int getApprovedRefundAmount() { return approvedRefundAmount; }
    public void setApprovedRefundAmount(int value) { approvedRefundAmount = Math.max(0, value); }
    public ComplaintStatus getStatus() { return status; }
    public void setStatus(ComplaintStatus status) { this.status = status; }
    public RefundStatus getRefundStatus() { return refundStatus; }
    public void setRefundStatus(RefundStatus refundStatus) { this.refundStatus = refundStatus; }
    public String getHandlerRole() { return handlerRole; }
    public void setHandlerRole(String handlerRole) { this.handlerRole = handlerRole; }
    public String getHandlerId() { return handlerId; }
    public void setHandlerId(String handlerId) { this.handlerId = handlerId; }
    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }
    public String getRefundTransactionReference() { return refundTransactionReference; }
    public void setRefundTransactionReference(String value) { this.refundTransactionReference = value; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public Long getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Long resolvedAt) { this.resolvedAt = resolvedAt; }
}
