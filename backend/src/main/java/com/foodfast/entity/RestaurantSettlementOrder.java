package com.foodfast.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "restaurant_settlement_orders", uniqueConstraints = {
        @UniqueConstraint(name = "uk_settlement_order", columnNames = "order_id")
})
public class RestaurantSettlementOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "settlement_id", length = 60, nullable = false)
    private String settlementId;

    @Column(name = "order_id", length = 60, nullable = false)
    private String orderId;

    @Column(name = "food_amount", nullable = false)
    private long foodAmount;

    @Column(name = "restaurant_tax_amount", nullable = false)
    private long restaurantTaxAmount;

    @Column(name = "restaurant_discount_amount", nullable = false)
    private long restaurantDiscountAmount;

    @Column(name = "platform_discount_amount", nullable = false)
    private long platformDiscountAmount;

    @Column(name = "commission_amount", nullable = false)
    private long commissionAmount;

    @Column(name = "refund_amount", nullable = false)
    private long refundAmount;

    @Column(name = "payable_amount", nullable = false)
    private long payableAmount;

    public Long getId() { return id; }
    public String getSettlementId() { return settlementId; }
    public void setSettlementId(String settlementId) { this.settlementId = settlementId; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public long getFoodAmount() { return foodAmount; }
    public void setFoodAmount(long foodAmount) { this.foodAmount = Math.max(0, foodAmount); }
    public long getRestaurantTaxAmount() { return restaurantTaxAmount; }
    public void setRestaurantTaxAmount(long restaurantTaxAmount) { this.restaurantTaxAmount = Math.max(0, restaurantTaxAmount); }
    public long getRestaurantDiscountAmount() { return restaurantDiscountAmount; }
    public void setRestaurantDiscountAmount(long restaurantDiscountAmount) { this.restaurantDiscountAmount = Math.max(0, restaurantDiscountAmount); }
    public long getPlatformDiscountAmount() { return platformDiscountAmount; }
    public void setPlatformDiscountAmount(long platformDiscountAmount) { this.platformDiscountAmount = Math.max(0, platformDiscountAmount); }
    public long getCommissionAmount() { return commissionAmount; }
    public void setCommissionAmount(long commissionAmount) { this.commissionAmount = Math.max(0, commissionAmount); }
    public long getRefundAmount() { return refundAmount; }
    public void setRefundAmount(long refundAmount) { this.refundAmount = Math.max(0, refundAmount); }
    public long getPayableAmount() { return payableAmount; }
    public void setPayableAmount(long payableAmount) { this.payableAmount = Math.max(0, payableAmount); }
}
