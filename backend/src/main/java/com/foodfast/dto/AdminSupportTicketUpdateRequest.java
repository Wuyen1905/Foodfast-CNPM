package com.foodfast.dto;

import com.foodfast.entity.SupportDepartment;
import com.foodfast.entity.SupportPriority;
import com.foodfast.entity.SupportTicketStatus;
import jakarta.validation.constraints.Size;

public record AdminSupportTicketUpdateRequest(
        SupportTicketStatus status,
        SupportPriority priority,
        SupportDepartment department,
        Boolean assignToMe,
        @Size(max = 1200) String note
) {}
