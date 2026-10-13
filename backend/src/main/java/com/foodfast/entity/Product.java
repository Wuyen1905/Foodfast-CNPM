package com.foodfast.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;


@Entity
@Table(name = "products")
public class Product {

    @Id
    @Column(length = 40, nullable = false, unique = true)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name="price", nullable=false)
    private int price;

    @Column(nullable = false)
    private String category;

    @Column(name = "image_url", length = 1024)
    private String imageUrl;

    // JSON property alias for frontend compatibility
    @JsonProperty("image")
    public String getImage() {
        return imageUrl;
    }

    public void setImage(String image) {
        this.imageUrl = image;
    }

    @Column(name="restaurant", nullable=false)
    private String restaurant;

    @Column(nullable = false)
    private boolean available = true;

    @Column(nullable = false)
    private boolean customizable = false;

    /**
     * Maximum portions that can be reserved in one Vietnam business day.
     * A null value means that the restaurant does not apply a daily limit.
     */
    @Column(name = "daily_limit")
    private Integer dailyLimit;

    @Transient
    private int soldToday;

    public Product() {
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

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = Math.max(price, 0);
    }
    
    public void setPrice(long p) {
        try {
            this.price = Math.toIntExact(p);
        } catch (Exception e) {
            this.price = 0;
        }
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getRestaurant() {
        return restaurant;
    }

    public void setRestaurant(String restaurant) {
        this.restaurant = restaurant;
    }

    public boolean isAvailable() {
        return available && !isSoldOutToday();
    }

    @JsonProperty("manuallyAvailable")
    public boolean isManuallyAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean isCustomizable() {
        return customizable;
    }

    public void setCustomizable(boolean customizable) {
        this.customizable = customizable;
    }

    public Integer getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(Integer dailyLimit) {
        this.dailyLimit = dailyLimit == null || dailyLimit <= 0 ? null : dailyLimit;
    }

    public int getSoldToday() {
        return Math.max(0, soldToday);
    }

    public void setSoldToday(int soldToday) {
        this.soldToday = Math.max(0, soldToday);
    }

    public Integer getRemainingToday() {
        if (dailyLimit == null) return null;
        return Math.max(0, dailyLimit - getSoldToday());
    }

    public boolean isSoldOutToday() {
        return dailyLimit != null && getSoldToday() >= dailyLimit;
    }
}

