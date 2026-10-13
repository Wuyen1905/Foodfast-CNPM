package com.foodfast.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(length = 50, nullable = false, unique = true)
    private String id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(length = 100, unique = true)
    private String email;

    @Column(length = 20, unique = true)
    private String phone;

    @Column(nullable = false, length = 20)
    private String role; // 'admin', 'customer', 'restaurant'

    @Column(name = "restaurant_id", length = 50)
    private String restaurantId;

    @Column(name = "order_count")
    @JsonProperty("orderCount")
    private Integer orderCount = 0;

    @Column(name = "created_at")
    @JsonProperty("createdAt")
    private Long createdAt;

    @Column(name = "last_login_at")
    private Long lastLoginAt;

    @Column(name = "suspension_reason", length = 500)
    private String suspensionReason;

    @Column(name = "account_status_updated_at")
    private Long accountStatusUpdatedAt;

    public User() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
    }

    public Integer getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Integer orderCount) {
        this.orderCount = orderCount;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Long lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public String getSuspensionReason() { return suspensionReason; }
    public void setSuspensionReason(String suspensionReason) { this.suspensionReason = suspensionReason; }
    public Long getAccountStatusUpdatedAt() { return accountStatusUpdatedAt; }
    public void setAccountStatusUpdatedAt(Long accountStatusUpdatedAt) { this.accountStatusUpdatedAt = accountStatusUpdatedAt; }
}

