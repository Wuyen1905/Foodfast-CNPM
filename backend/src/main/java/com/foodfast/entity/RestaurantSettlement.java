package com.foodfast.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "restaurant_settlements")
public class RestaurantSettlement {
    @Id
    @Column(length = 60, nullable = false)
    private String id;

    @Column(name = "restaurant_id", length = 50, nullable = false)
    private String restaurantId;

    @Column(name = "restaurant_name", length = 160, nullable = false)
    private String restaurantName;

    @Column(name = "period_start", nullable = false)
    private Instant periodStart;

    @Column(name = "period_end", nullable = false)
    private Instant periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SettlementStatus status = SettlementStatus.PENDING_REVIEW;

    @Column(name = "order_count", nullable = false)
    private int orderCount;

    @Column(name = "food_amount", nullable = false)
    private long foodAmount;

    @Column(name = "restaurant_tax_amount", nullable = false)
    private long restaurantTaxAmount;

    @Column(name = "restaurant_discount_amount", nullable = false)
    private long restaurantDiscountAmount;

    @Column(name = "platform_discount_amount", nullable = false)
    private long platformDiscountAmount;

    @Column(name = "commission_rate_bps", nullable = false)
    private int commissionRateBps;

    @Column(name = "commission_amount", nullable = false)
    private long commissionAmount;

    @Column(name = "refund_amount", nullable = false)
    private long refundAmount;

    @Column(name = "adjustment_amount", nullable = false)
    private long adjustmentAmount;

    @Column(name = "payable_amount", nullable = false)
    private long payableAmount;

    @Column(name = "created_by", length = 60, nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "submitted_by", length = 60)
    private String submittedBy;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "paid_by", length = 60)
    private String paidBy;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "payment_reference", length = 120)
    private String paymentReference;

    @Version
    @Column(nullable = false)
    private long version;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = SettlementStatus.PENDING_REVIEW;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRestaurantId() { return restaurantId; }
    public void setRestaurantId(String restaurantId) { this.restaurantId = restaurantId; }
    public String getRestaurantName() { return restaurantName; }
    public void setRestaurantName(String restaurantName) { this.restaurantName = restaurantName; }
    public Instant getPeriodStart() { return periodStart; }
    public void setPeriodStart(Instant periodStart) { this.periodStart = periodStart; }
    public Instant getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(Instant periodEnd) { this.periodEnd = periodEnd; }
    public SettlementStatus getStatus() { return status; }
    public void setStatus(SettlementStatus status) { this.status = status; }
    public int getOrderCount() { return orderCount; }
    public void setOrderCount(int orderCount) { this.orderCount = Math.max(0, orderCount); }
    public long getFoodAmount() { return foodAmount; }
    public void setFoodAmount(long foodAmount) { this.foodAmount = Math.max(0, foodAmount); }
    public long getRestaurantTaxAmount() { return restaurantTaxAmount; }
    public void setRestaurantTaxAmount(long restaurantTaxAmount) { this.restaurantTaxAmount = Math.max(0, restaurantTaxAmount); }
    public long getRestaurantDiscountAmount() { return restaurantDiscountAmount; }
    public void setRestaurantDiscountAmount(long restaurantDiscountAmount) { this.restaurantDiscountAmount = Math.max(0, restaurantDiscountAmount); }
    public long getPlatformDiscountAmount() { return platformDiscountAmount; }
    public void setPlatformDiscountAmount(long platformDiscountAmount) { this.platformDiscountAmount = Math.max(0, platformDiscountAmount); }
    public int getCommissionRateBps() { return commissionRateBps; }
    public void setCommissionRateBps(int commissionRateBps) { this.commissionRateBps = Math.max(0, commissionRateBps); }
    public long getCommissionAmount() { return commissionAmount; }
    public void setCommissionAmount(long commissionAmount) { this.commissionAmount = Math.max(0, commissionAmount); }
    public long getRefundAmount() { return refundAmount; }
    public void setRefundAmount(long refundAmount) { this.refundAmount = Math.max(0, refundAmount); }
    public long getAdjustmentAmount() { return adjustmentAmount; }
    public void setAdjustmentAmount(long adjustmentAmount) { this.adjustmentAmount = adjustmentAmount; }
    public long getPayableAmount() { return payableAmount; }
    public void setPayableAmount(long payableAmount) { this.payableAmount = Math.max(0, payableAmount); }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
    public String getPaidBy() { return paidBy; }
    public void setPaidBy(String paidBy) { this.paidBy = paidBy; }
    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }
    public long getVersion() { return version; }
}
