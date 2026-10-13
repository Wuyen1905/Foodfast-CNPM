package com.foodfast.event;

import com.foodfast.entity.Order;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public OrderEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishOrderUpdate(Order order) {
        if (order.getUserId() != null && !order.getUserId().isBlank()) {
            messagingTemplate.convertAndSendToUser(order.getUserId(), "/queue/orders", order);
        }

        if (order.getRestaurantId() != null) {
            messagingTemplate.convertAndSend(
                    "/topic/restaurants/" + order.getRestaurantId() + "/orders",
                    order
            );
        }

        messagingTemplate.convertAndSend("/topic/admin/orders", order);
    }
}
