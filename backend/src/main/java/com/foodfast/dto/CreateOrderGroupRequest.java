package com.foodfast.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Map;

public class CreateOrderGroupRequest {
    @JsonIgnore
    public String userId;
    @JsonIgnore
    public String customerName;
    @JsonIgnore
    public String customerEmail;
    public String customerPhone;
    @NotBlank(message = "Vui lòng nhập địa chỉ giao hàng")
    public String address;
    public String note;
    public String paymentMethod;
    @NotEmpty(message = "Giỏ hàng phải có ít nhất một món")
    public List<CreateOrderRequest.Item> items;
    public Map<String, String> voucherCodesByRestaurant;
}
