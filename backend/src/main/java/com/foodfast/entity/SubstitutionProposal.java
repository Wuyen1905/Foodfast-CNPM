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

import java.time.Instant;

@Entity
@Table(name = "substitution_proposals")
public class SubstitutionProposal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "order_id", nullable = false, length = 60)
    private String orderId;
    @Column(name = "order_item_id", nullable = false)
    private Long orderItemId;
    @Column(name = "restaurant_id", nullable = false, length = 60)
    private String restaurantId;
    @Column(name = "customer_id", nullable = false, length = 100)
    private String customerId;
    @Column(name = "original_product_id", length = 40)
    private String originalProductId;
    @Column(name = "original_product_name", nullable = false, length = 200)
    private String originalProductName;
    @Column(name = "original_unit_price", nullable = false)
    private int originalUnitPrice;
    @Column(name = "replacement_product_id", nullable = false, length = 40)
    private String replacementProductId;
    @Column(name = "replacement_product_name", nullable = false, length = 200)
    private String replacementProductName;
    @Column(name = "replacement_unit_price", nullable = false)
    private int replacementUnitPrice;
    @Column(length = 500)
    private String reason;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SubstitutionStatus status = SubstitutionStatus.PENDING_CUSTOMER;
    @Column(name = "proposed_at", nullable = false)
    private Instant proposedAt;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "responded_at")
    private Instant respondedAt;

    @PrePersist
    void prePersist() { if (proposedAt == null) proposedAt = Instant.now(); }
    public Long getId() { return id; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public Long getOrderItemId() { return orderItemId; }
    public void setOrderItemId(Long orderItemId) { this.orderItemId = orderItemId; }
    public String getRestaurantId() { return restaurantId; }
    public void setRestaurantId(String restaurantId) { this.restaurantId = restaurantId; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getOriginalProductId() { return originalProductId; }
    public void setOriginalProductId(String originalProductId) { this.originalProductId = originalProductId; }
    public String getOriginalProductName() { return originalProductName; }
    public void setOriginalProductName(String originalProductName) { this.originalProductName = originalProductName; }
    public int getOriginalUnitPrice() { return originalUnitPrice; }
    public void setOriginalUnitPrice(int originalUnitPrice) { this.originalUnitPrice = originalUnitPrice; }
    public String getReplacementProductId() { return replacementProductId; }
    public void setReplacementProductId(String replacementProductId) { this.replacementProductId = replacementProductId; }
    public String getReplacementProductName() { return replacementProductName; }
    public void setReplacementProductName(String replacementProductName) { this.replacementProductName = replacementProductName; }
    public int getReplacementUnitPrice() { return replacementUnitPrice; }
    public void setReplacementUnitPrice(int replacementUnitPrice) { this.replacementUnitPrice = replacementUnitPrice; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public SubstitutionStatus getStatus() { return status; }
    public void setStatus(SubstitutionStatus status) { this.status = status; }
    public Instant getProposedAt() { return proposedAt; }
    public void setProposedAt(Instant proposedAt) { this.proposedAt = proposedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getRespondedAt() { return respondedAt; }
    public void setRespondedAt(Instant respondedAt) { this.respondedAt = respondedAt; }
}
