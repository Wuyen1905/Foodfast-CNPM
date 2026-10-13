package com.foodfast.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * A delivery employee managed by one restaurant.
 *
 * Couriers intentionally do not have login accounts. Restaurant staff assign
 * them to orders and update delivery progress from the restaurant dashboard.
 */
@Entity
@Table(name = "restaurant_couriers")
public class RestaurantCourier {

    @Id
    @Column(length = 50, nullable = false)
    private String id;

    @Column(name = "restaurant_id", length = 50, nullable = false)
    private String restaurantId;

    @Column(length = 150, nullable = false)
    private String name;

    @Column(length = 20, nullable = false)
    private String phone;

    @Column(name = "vehicle_type", length = 50)
    private String vehicleType;

    @Column(name = "license_plate", length = 30)
    private String licensePlate;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @PrePersist
    void prePersist() {
        long now = System.currentTimeMillis();
        if (id == null || id.isBlank()) {
            id = "COURIER-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        createdAt = createdAt == 0 ? now : createdAt;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRestaurantId() { return restaurantId; }
    public void setRestaurantId(String restaurantId) { this.restaurantId = restaurantId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
