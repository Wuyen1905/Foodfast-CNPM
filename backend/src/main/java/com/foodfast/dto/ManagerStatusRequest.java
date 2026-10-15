package com.foodfast.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ManagerStatusRequest(
        @NotNull(message = "Vui lòng chọn trạng thái tài khoản") Boolean active,
        @Size(max = 500, message = "Lý do tối đa 500 ký tự") String reason
) {
}
