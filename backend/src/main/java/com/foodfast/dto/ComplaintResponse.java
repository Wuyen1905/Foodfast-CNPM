package com.foodfast.dto;

import com.foodfast.entity.Complaint;
import com.foodfast.entity.ComplaintCategory;
import com.foodfast.entity.ComplaintHistory;
import com.foodfast.entity.ComplaintStatus;

import java.util.List;

public record ComplaintResponse(
        String id,
        String orderId,
        String restaurantId,
        String customerId,
        ComplaintCategory category,
        String description,
        List<String> evidenceUrls,
        ComplaintStatus status,
        String handlerRole,
        String handlerId,
        String resolution,
        long createdAt,
        long updatedAt,
        Long resolvedAt,
        List<HistoryItem> history
) {
    public static ComplaintResponse from(Complaint item, List<ComplaintHistory> history) {
        return new ComplaintResponse(
                item.getId(), item.getOrderId(), item.getRestaurantId(), item.getCustomerId(),
                item.getCategory(), item.getDescription(), item.getEvidenceUrls(),
                item.getStatus(), item.getHandlerRole(), item.getHandlerId(), item.getResolution(),
                item.getCreatedAt(), item.getUpdatedAt(),
                item.getResolvedAt(), history.stream().map(HistoryItem::from).toList()
        );
    }

    public record HistoryItem(
            Long id,
            String action,
            String actorRole,
            String actorId,
            String fromStatus,
            String toStatus,
            String note,
            long createdAt
    ) {
        static HistoryItem from(ComplaintHistory item) {
            return new HistoryItem(item.getId(), item.getAction(), item.getActorRole(), item.getActorId(),
                    item.getFromStatus(), item.getToStatus(), item.getNote(), item.getCreatedAt());
        }
    }
}
