package com.foodfast.dto;

import com.foodfast.entity.SupportTicketMessage;

public record SupportMessageResponse(
        Long id,
        String ticketId,
        String senderRole,
        String senderName,
        String message,
        long createdAt
) {
    public static SupportMessageResponse from(SupportTicketMessage item) {
        return new SupportMessageResponse(
                item.getId(), item.getTicketId(), item.getSenderRole(), item.getSenderName(),
                item.getMessage(), item.getCreatedAt()
        );
    }
}
