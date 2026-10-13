package com.foodfast.dto;

import java.util.List;

public record SupportChatResponse(
        String reply,
        String source,
        String intent,
        String handoffTarget,
        String orderId,
        String supportTicketId,
        List<String> suggestions,
        boolean requiresOrder
) {
}
