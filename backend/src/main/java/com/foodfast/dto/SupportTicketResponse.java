package com.foodfast.dto;

import com.foodfast.entity.SupportDepartment;
import com.foodfast.entity.SupportPriority;
import com.foodfast.entity.SupportTicket;
import com.foodfast.entity.SupportTicketHistory;
import com.foodfast.entity.SupportTicketStatus;
import com.foodfast.entity.SupportTicketTarget;

import java.util.List;

public record SupportTicketResponse(
        String id,
        String orderId,
        String restaurantId,
        String customerName,
        String intent,
        String message,
        SupportTicketTarget target,
        SupportTicketStatus status,
        SupportPriority priority,
        SupportDepartment department,
        String assignedAdminId,
        String assignedAdminName,
        Long slaDueAt,
        boolean overdue,
        String resolutionNote,
        long createdAt,
        long updatedAt,
        Long resolvedAt,
        List<HistoryItem> history
) {
    public static SupportTicketResponse from(SupportTicket ticket, List<SupportTicketHistory> history) {
        boolean overdue = ticket.getStatus() != SupportTicketStatus.RESOLVED
                && ticket.getSlaDueAt() != null
                && ticket.getSlaDueAt() < System.currentTimeMillis();
        return new SupportTicketResponse(
                ticket.getId(), ticket.getOrderId(), ticket.getRestaurantId(), ticket.getCustomerName(), ticket.getIntent(),
                ticket.getMessage(), ticket.getTarget(), ticket.getStatus(), ticket.getPriority(),
                ticket.getDepartment(), ticket.getAssignedAdminId(), ticket.getAssignedAdminName(),
                ticket.getSlaDueAt(), overdue, ticket.getResolutionNote(), ticket.getCreatedAt(),
                ticket.getUpdatedAt(), ticket.getResolvedAt(),
                history.stream().map(HistoryItem::from).toList()
        );
    }

    public record HistoryItem(
            Long id,
            String action,
            String actorRole,
            String actorId,
            String note,
            String fromStatus,
            String toStatus,
            long createdAt
    ) {
        static HistoryItem from(SupportTicketHistory item) {
            return new HistoryItem(item.getId(), item.getAction(), item.getActorRole(), item.getActorId(),
                    item.getNote(), item.getFromStatus(), item.getToStatus(), item.getCreatedAt());
        }
    }
}
