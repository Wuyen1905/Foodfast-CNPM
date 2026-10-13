package com.foodfast.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AdminActionRequest {

    private Boolean isActive;

    @NotBlank(message = "Vui lòng nhập lý do thực hiện")
    @Size(min = 5, max = 500, message = "Lý do phải có từ 5 đến 500 ký tự")
    private String reason;

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
