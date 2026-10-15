package com.foodfast.dto;

import com.foodfast.entity.ComplaintAction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewComplaintRequest(
        @NotNull ComplaintAction action,
        @Size(max = 1200) String note
) {
}
