package com.foodfast.service;

import com.foodfast.entity.CartItem;
import com.foodfast.entity.DeliveryAssignment;
import com.foodfast.entity.Order;
import com.foodfast.repository.OrderRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RealtimeService {

    private final SimpMessagingTemplate messagingTemplate;
    private final OrderRepository orderRepository;

    public RealtimeService(SimpMessagingTemplate messagingTemplate, OrderRepository orderRepository) {
        this.messagingTemplate = messagingTemplate;
        this.orderRepository = orderRepository;
    }

    public void sendOrderUpdate(Order order) {
        if (order.getUserId() != null && !order.getUserId().isBlank()) {
            messagingTemplate.convertAndSendToUser(order.getUserId(), "/queue/orders", order);
        }
        if (order.getRestaurantId() != null && !order.getRestaurantId().isBlank()) {
            messagingTemplate.convertAndSend(
                    "/topic/restaurants/" + order.getRestaurantId() + "/orders",
                    order
            );
        }
        messagingTemplate.convertAndSend("/topic/admin/orders", order);
    }

    public void sendDeliveryUpdate(DeliveryAssignment assignment) {
        messagingTemplate.convertAndSend(
                "/topic/restaurants/" + assignment.getRestaurantId() + "/deliveries",
                assignment
        );
        messagingTemplate.convertAndSend("/topic/admin/deliveries", assignment);

        orderRepository.findById(assignment.getOrderId())
                .map(Order::getUserId)
                .filter(userId -> userId != null && !userId.isBlank())
                .ifPresent(userId -> messagingTemplate.convertAndSendToUser(
                        userId,
                        "/queue/deliveries",
                        assignment
                ));
    }

    public void sendCartUpdate(String userId, List<CartItem> cartItems) {
        if (userId != null && !userId.isBlank()) {
            messagingTemplate.convertAndSendToUser(userId, "/queue/cart", cartItems);
        }
    }
}

