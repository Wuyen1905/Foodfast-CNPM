package com.foodfast.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

@Entity
@Table(name = "restaurants")
public class Restaurant {

    @Id
    @Column(length = 50, nullable = false, unique = true)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private Double rating = 0.0;

    @Column(name = "primary_color", length = 20)
    private String primaryColor;

    @Column(name = "secondary_color", length = 20)
    private String secondaryColor;

    @Column(name = "accent_color", length = 20)
    private String accentColor;

    @Column(name = "owner_id", length = 50, nullable = false)
    private String ownerId;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "admin_status", length = 30, nullable = false)
    private RestaurantLifecycleStatus adminStatus = RestaurantLifecycleStatus.ACTIVE;

    @Column(name = "admin_status_reason", length = 500)
    private String adminStatusReason;

    @Column(name = "admin_status_updated_at")
    private Long adminStatusUpdatedAt;

    @Column(name = "accepting_orders", nullable = false)
    private Boolean acceptingOrders = true;

    @Column(name = "pause_reason", length = 300)
    private String pauseReason;

    @Column(name = "opening_time", nullable = false, length = 5)
    private String openingTime = "08:00";

    @Column(name = "closing_time", nullable = false, length = 5)
    private String closingTime = "22:00";

    @Column(name = "created_at")
    @JsonProperty("createdAt")
    private Long createdAt;

    public Restaurant() {
    }

    @PrePersist
    void prePersist() {
        if (adminStatus == null) {
            adminStatus = Boolean.FALSE.equals(isActive)
                    ? RestaurantLifecycleStatus.SUSPENDED
                    : RestaurantLifecycleStatus.ACTIVE;
        }
        if (adminStatusUpdatedAt == null) adminStatusUpdatedAt = System.currentTimeMillis();
    }

    // JSON getter that returns theme as object for frontend
    @JsonProperty("theme")
    public ThemeObject getTheme() {
        return new ThemeObject(primaryColor, secondaryColor, accentColor);
    }

    public void setTheme(ThemeObject theme) {
        if (theme != null) {
            this.primaryColor = theme.primary;
            this.secondaryColor = theme.secondary;
            this.accentColor = theme.accent;
        }
    }

    // Inner class for theme object
    public static class ThemeObject {
        public String primary;
        public String secondary;
        public String accent;

        public ThemeObject() {
        }

        public ThemeObject(String primary, String secondary, String accent) {
            this.primary = primary;
            this.secondary = secondary;
            this.accent = accent;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(String primaryColor) {
        this.primaryColor = primaryColor;
    }

    public String getSecondaryColor() {
        return secondaryColor;
    }

    public void setSecondaryColor(String secondaryColor) {
        this.secondaryColor = secondaryColor;
    }

    public String getAccentColor() {
        return accentColor;
    }

    public void setAccentColor(String accentColor) {
        this.accentColor = accentColor;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public RestaurantLifecycleStatus getAdminStatus() {
        if (adminStatus != null) return adminStatus;
        return Boolean.FALSE.equals(isActive)
                ? RestaurantLifecycleStatus.SUSPENDED
                : RestaurantLifecycleStatus.ACTIVE;
    }

    public void setAdminStatus(RestaurantLifecycleStatus adminStatus) {
        this.adminStatus = adminStatus;
        this.isActive = adminStatus == RestaurantLifecycleStatus.ACTIVE;
    }

    public String getAdminStatusReason() { return adminStatusReason; }
    public void setAdminStatusReason(String adminStatusReason) { this.adminStatusReason = adminStatusReason; }
    public Long getAdminStatusUpdatedAt() { return adminStatusUpdatedAt; }
    public void setAdminStatusUpdatedAt(Long adminStatusUpdatedAt) { this.adminStatusUpdatedAt = adminStatusUpdatedAt; }

    public Boolean getAcceptingOrders() {
        return acceptingOrders == null ? Boolean.TRUE : acceptingOrders;
    }

    public void setAcceptingOrders(Boolean acceptingOrders) {
        this.acceptingOrders = acceptingOrders == null ? Boolean.TRUE : acceptingOrders;
    }

    public String getPauseReason() {
        return pauseReason;
    }

    public void setPauseReason(String pauseReason) {
        this.pauseReason = pauseReason;
    }

    public String getOpeningTime() {
        return openingTime;
    }

    public void setOpeningTime(String openingTime) {
        this.openingTime = openingTime;
    }

    public String getClosingTime() {
        return closingTime;
    }

    public void setClosingTime(String closingTime) {
        this.closingTime = closingTime;
    }

    @JsonProperty("openNow")
    public boolean isOpenNow() {
        return isOpenAt(LocalTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
    }

    public boolean isOpenAt(LocalTime time) {
        if (time == null) return false;
        try {
            LocalTime opening = LocalTime.parse(openingTime);
            LocalTime closing = LocalTime.parse(closingTime);
            if (opening.equals(closing)) return true;
            if (opening.isBefore(closing)) {
                return !time.isBefore(opening) && time.isBefore(closing);
            }
            return !time.isBefore(opening) || time.isBefore(closing);
        } catch (DateTimeParseException | NullPointerException error) {
            return false;
        }
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
}

