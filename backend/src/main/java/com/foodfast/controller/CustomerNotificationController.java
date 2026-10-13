package com.foodfast.controller;

import com.foodfast.entity.CustomerNotification;
import com.foodfast.security.CurrentUserService;
import com.foodfast.service.CustomerNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customer-notifications")
public class CustomerNotificationController {

    private final CustomerNotificationService notificationService;
    private final CurrentUserService currentUserService;

    public CustomerNotificationController(
            CustomerNotificationService notificationService,
            CurrentUserService currentUserService
    ) {
        this.notificationService = notificationService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public Map<String, Object> mine() {
        String userId = currentUserService.requireUser().getId();
        List<CustomerNotification> notifications = notificationService.mine(userId);
        return Map.of(
                "notifications", notifications,
                "unreadCount", notificationService.unreadCount(userId),
                "total", notifications.size()
        );
    }

    @PatchMapping("/{id}/read")
    public CustomerNotification markRead(@PathVariable String id) {
        return notificationService.markRead(currentUserService.requireUser().getId(), id);
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllRead() {
        int updated = notificationService.markAllRead(currentUserService.requireUser().getId());
        return ResponseEntity.ok(Map.of("updated", updated));
    }
}
