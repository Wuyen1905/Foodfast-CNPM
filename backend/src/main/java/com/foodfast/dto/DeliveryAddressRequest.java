package com.foodfast.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DeliveryAddressRequest(
        @NotBlank(message = "Vui lòng nhập tên gợi nhớ")
        @Size(max = 50, message = "Tên gợi nhớ tối đa 50 ký tự")
        String label,

        @NotBlank(message = "Vui lòng nhập tên người nhận")
        @Size(max = 120, message = "Tên người nhận tối đa 120 ký tự")
        String recipientName,

        @NotBlank(message = "Vui lòng nhập số điện thoại")
        @Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$", message = "Số điện thoại không hợp lệ")
        String phone,

        @NotBlank(message = "Vui lòng nhập địa chỉ giao hàng")
        @Size(min = 10, max = 500, message = "Địa chỉ phải từ 10 đến 500 ký tự")
        String addressLine,

        boolean defaultAddress
) { }
