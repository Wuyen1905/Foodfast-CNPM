package com.foodfast.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "voucher_redemptions", uniqueConstraints = @UniqueConstraint(columnNames = "order_id"))
public class VoucherRedemption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "voucher_code", nullable = false, length = 40)
    private String voucherCode;
    @Column(name = "order_id", nullable = false, length = 60)
    private String orderId;
    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VoucherRedemptionStatus status = VoucherRedemptionStatus.RESERVED;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() { if (createdAt == null) createdAt = Instant.now(); }
    public Long getId() { return id; }
    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public VoucherRedemptionStatus getStatus() { return status; }
    public void setStatus(VoucherRedemptionStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
}
