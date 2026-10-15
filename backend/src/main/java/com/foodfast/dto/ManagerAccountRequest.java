package com.foodfast.dto;

import com.foodfast.entity.ManagerPermission;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record ManagerAccountRequest(
        @NotBlank(message = "Vui lòng nhập tên đăng nhập")
        @Size(min = 4, max = 50, message = "Tên đăng nhập phải từ 4 đến 50 ký tự")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Tên đăng nhập chỉ gồm chữ, số, dấu chấm, gạch dưới hoặc gạch nối")
        String username,

        @Size(min = 8, max = 64, message = "Mật khẩu phải từ 8 đến 64 ký tự")
        @Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d)\\S+$", message = "Mật khẩu phải có ít nhất một chữ và một số, không chứa khoảng trắng")
        String password,

        @NotBlank(message = "Vui lòng nhập họ tên")
        @Size(min = 2, max = 100, message = "Họ tên phải từ 2 đến 100 ký tự")
        String name,

        @Email(message = "Email không hợp lệ")
        @Size(max = 100, message = "Email tối đa 100 ký tự")
        String email,

        @Pattern(regexp = "^$|0(?:3|5|7|8|9)\\d{8}$", message = "Số điện thoại không hợp lệ")
        String phone,

        @NotNull(message = "Danh sách quyền không được để trống")
        Set<ManagerPermission> permissions
) {
}
