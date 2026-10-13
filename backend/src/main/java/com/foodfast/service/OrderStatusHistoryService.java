package com.foodfast.service;

import com.foodfast.entity.OrderStatus;
import com.foodfast.entity.OrderStatusHistory;
import com.foodfast.repository.OrderStatusHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class OrderStatusHistoryService {

    private final OrderStatusHistoryRepository repository;

    public OrderStatusHistoryService(OrderStatusHistoryRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public OrderStatusHistory record(
            String orderId,
            OrderStatus from,
            OrderStatus to,
            String actorId,
            String actorRole,
            String source,
            String reason
    ) {
        if (!StringUtils.hasText(orderId) || to == null) {
            throw new IllegalArgumentException("Thiếu đơn hàng hoặc trạng thái cần ghi lịch sử");
        }
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderId(orderId.trim());
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setActorId(normalize(actorId));
        history.setActorRole(StringUtils.hasText(actorRole) ? actorRole.trim().toUpperCase() : "SYSTEM");
        history.setSource(StringUtils.hasText(source) ? source.trim().toUpperCase() : "SYSTEM");
        history.setReason(normalize(reason));
        history.setCreatedAt(System.currentTimeMillis());
        return repository.save(history);
    }

    @Transactional(readOnly = true)
    public List<OrderStatusHistory> findByOrder(String orderId) {
        return repository.findByOrderIdOrderByCreatedAtAscIdAsc(orderId);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
