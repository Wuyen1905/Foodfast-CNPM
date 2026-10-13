package com.foodfast.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.Transient;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "cart_items")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productId;

    private String productName;

    @Column(name = "product_image", length = 1024)
    private String productImage;

    private Double unitPrice;

    private Integer quantity;

    private String restaurantCode;

    private String userId;

    @Column(name = "variant_key", length = 1024, nullable = false)
    private String variantKey;

    @Column(name = "selected_option_ids", length = 1000)
    private String selectedOptionIdsCsv;

    @Column(name = "option_summary", length = 1000)
    private String optionSummary;

    @Column(name = "item_note", length = 200)
    private String itemNote;

    public CartItem() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductImage() { return productImage; }
    public void setProductImage(String productImage) { this.productImage = productImage; }

    public Double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getRestaurantCode() {
        return restaurantCode;
    }

    public void setRestaurantCode(String restaurantCode) {
        this.restaurantCode = restaurantCode;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getVariantKey() { return variantKey; }
    public void setVariantKey(String variantKey) { this.variantKey = variantKey; }
    @JsonIgnore
    public String getSelectedOptionIdsCsv() { return selectedOptionIdsCsv; }
    public void setSelectedOptionIdsCsv(String selectedOptionIdsCsv) { this.selectedOptionIdsCsv = selectedOptionIdsCsv; }
    @Transient
    public List<String> getSelectedOptionIds() {
        if (selectedOptionIdsCsv == null || selectedOptionIdsCsv.isBlank()) return List.of();
        return Arrays.stream(selectedOptionIdsCsv.split(","))
                .map(String::trim).filter(value -> !value.isEmpty()).toList();
    }
    public void setSelectedOptionIds(List<String> optionIds) {
        this.selectedOptionIdsCsv = optionIds == null ? null : String.join(",", optionIds);
    }
    public String getOptionSummary() { return optionSummary; }
    public void setOptionSummary(String optionSummary) { this.optionSummary = optionSummary; }
    public String getItemNote() { return itemNote; }
    public void setItemNote(String itemNote) { this.itemNote = itemNote; }
}

