package com.foodfast.dto;

import com.foodfast.entity.ComplaintCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateComplaintRequest(
        @NotBlank String orderId,
        @NotNull ComplaintCategory category,
        @NotBlank @Size(min = 10, max = 1200) String description,
        @Size(max = 5) List<@Size(max = 500) String> evidenceUrls,
        @Min(0) int requestedRefundAmount
) {
}
