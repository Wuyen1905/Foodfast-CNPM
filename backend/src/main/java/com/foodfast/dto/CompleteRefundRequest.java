package com.foodfast.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompleteRefundRequest(
        @NotBlank @Size(max = 100) String transactionReference,
        @Size(max = 1200) String note
) {
}
