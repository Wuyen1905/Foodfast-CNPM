package com.foodfast.dto;

import com.foodfast.entity.SupportIssueType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSupportContactRequest(
        @NotNull SupportIssueType issueType,
        @Size(max = 60) String orderId,
        @NotBlank @Size(min = 10, max = 1000) String message,
        @Size(max = 5000) String aiTranscript
) {
}
