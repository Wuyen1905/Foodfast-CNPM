package com.foodfast.controller;

import com.foodfast.entity.Notification;
import com.foodfast.service.NotificationService;
import com.foodfast.security.CurrentUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Notification Controller
 * Handles restaurant notifications
 */
@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://192.168.0.100:5173",
        "http://192.168.0.100:5174",
        "http://192.168.0.100:5175",
        "http://localhost:8081"
})
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;

    public NotificationController(
            NotificationService notificationService,
            CurrentUserService currentUserService
    ) {
        this.notificationService = notificationService;
        this.currentUserService = currentUserService;
    }

    /**
     * GET /api/notifications/{restaurantId}
     * Get all notifications for a restaurant (frontend-compatible path)
     * Supports optional query parameter: ?unreadOnly=true
     */
    @GetMapping("/{restaurantId}")
    public ResponseEntity<Map<String, Object>> getNotificationsByRestaurantId(
            @PathVariable String restaurantId,
            @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "false") boolean unreadOnly) {
        currentUserService.requireRestaurantAccess(restaurantId);
        List<Notification> notifications;
        if (unreadOnly) {
            notifications = notificationService.getUnreadNotifications(restaurantId);
        } else {
            notifications = notificationService.getNotifications(restaurantId);
        }
        
        long unreadCount = notificationService.getUnreadCount(restaurantId);
        
        // Return response matching frontend expectations
        Map<String, Object> response = new HashMap<>();
        response.put("notifications", notifications);
        response.put("unreadCount", unreadCount);
        response.put("total", notifications.size());
        
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/notifications/restaurant/{restaurantId}
     * Get all notifications for a restaurant (alternative path for backward compatibility)
     */
    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<Map<String, Object>> getNotifications(
            @PathVariable String restaurantId,
            @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "false") boolean unreadOnly) {
        currentUserService.requireRestaurantAccess(restaurantId);
        List<Notification> notifications;
        if (unreadOnly) {
            notifications = notificationService.getUnreadNotifications(restaurantId);
        } else {
            notifications = notificationService.getNotifications(restaurantId);
        }
        
        long unreadCount = notificationService.getUnreadCount(restaurantId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("notifications", notifications);
        response.put("unreadCount", unreadCount);
        response.put("total", notifications.size());
        
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/notifications/restaurant/{restaurantId}/unread
     * Get unread notifications for a restaurant
     */
    @GetMapping("/restaurant/{restaurantId}/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications(@PathVariable String restaurantId) {
        currentUserService.requireRestaurantAccess(restaurantId);
        List<Notification> notifications = notificationService.getUnreadNotifications(restaurantId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * GET /api/notifications/restaurant/{restaurantId}/count
     * Get unread notification count
     */
    @GetMapping("/restaurant/{restaurantId}/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable String restaurantId) {
        currentUserService.requireRestaurantAccess(restaurantId);
        long count = notificationService.getUnreadCount(restaurantId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * POST /api/notifications/{id}/read
     * Mark notification as read
     */
    @PostMapping("/{id}/read")
    public ResponseEntity<Map<String, String>> markAsRead(@PathVariable String id) {
        Notification notification = notificationService.findById(id);
        currentUserService.requireRestaurantAccess(notification.getRestaurantId());
        notificationService.markAsRead(id);
        return ResponseEntity.ok(Map.of("message", "Đã đánh dấu thông báo là đã đọc"));
    }
}

