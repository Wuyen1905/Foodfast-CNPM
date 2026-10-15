package com.foodfast.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "vouchers")
public class Voucher {
    @Id
    @Column(length = 40)
    private String code;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(name = "restaurant_id", length = 60)
    private String restaurantId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VoucherScope scope = VoucherScope.RESTAURANT;
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private VoucherDiscountType discountType;
    @Column(name = "discount_value", nullable = false)
    private int discountValue;
    @Column(name = "min_order_amount", nullable = false)
    private int minOrderAmount;
    @Column(name = "max_discount", nullable = false)
    private int maxDiscount;
    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;
    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;
    @Column(name = "usage_limit", nullable = false)
    private int usageLimit;
    @Column(name = "per_user_limit", nullable = false)
    private int perUserLimit = 1;
    @Column(nullable = false)
    private boolean active = true;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRestaurantId() { return restaurantId; }
    public void setRestaurantId(String restaurantId) { this.restaurantId = restaurantId; }
    public VoucherScope getScope() { return scope; }
    public void setScope(VoucherScope scope) { this.scope = scope == null ? VoucherScope.RESTAURANT : scope; }
    public VoucherDiscountType getDiscountType() { return discountType; }
    public void setDiscountType(VoucherDiscountType discountType) { this.discountType = discountType; }
    public int getDiscountValue() { return discountValue; }
    public void setDiscountValue(int discountValue) { this.discountValue = Math.max(0, discountValue); }
    public int getMinOrderAmount() { return minOrderAmount; }
    public void setMinOrderAmount(int minOrderAmount) { this.minOrderAmount = Math.max(0, minOrderAmount); }
    public int getMaxDiscount() { return maxDiscount; }
    public void setMaxDiscount(int maxDiscount) { this.maxDiscount = Math.max(0, maxDiscount); }
    public Instant getStartsAt() { return startsAt; }
    public void setStartsAt(Instant startsAt) { this.startsAt = startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public void setEndsAt(Instant endsAt) { this.endsAt = endsAt; }
    public int getUsageLimit() { return usageLimit; }
    public void setUsageLimit(int usageLimit) { this.usageLimit = Math.max(0, usageLimit); }
    public int getPerUserLimit() { return perUserLimit; }
    public void setPerUserLimit(int perUserLimit) { this.perUserLimit = Math.max(1, perUserLimit); }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
