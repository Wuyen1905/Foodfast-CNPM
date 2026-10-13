package com.foodfast.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class CreateOrderRequest {
    @JsonIgnore
    public String id;
    public String customerName;
    public String customerPhone;
    public String customerEmail;
    @NotBlank(message = "Vui lòng nhập địa chỉ giao hàng")
    public String address;
    public String note;
    public String restaurantId;
    @JsonIgnore
    public String userId;
    @JsonIgnore
    public String paymentSessionId;
    public String voucherCode;

    @JsonIgnore
    public long createdAt;
    @JsonIgnore
    public long updatedAt;
    public String orderTime;

    @JsonIgnore
    public long total;
    @JsonIgnore
    public String status;
    public String paymentMethod;
    @JsonIgnore
    public String paymentStatus;

    public List<Item> items;

    public static class Item {
        public String productId;
        @JsonIgnore
        public String name;
        public int quantity;
        public int qty; // Support both qty and quantity from frontend
        @JsonIgnore
        public long price;
        public List<String> selectedOptionIds;
        public String itemNote;
        
        // Getter to handle both qty and quantity
        public int getQuantity() {
            return quantity > 0 ? quantity : (qty > 0 ? qty : 1);
        }
    }
}

