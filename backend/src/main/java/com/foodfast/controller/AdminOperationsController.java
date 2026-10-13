package com.foodfast.controller;

import com.foodfast.dto.AdminRestaurantStatusRequest;
import com.foodfast.dto.AdminAnalyticsResponse;
import com.foodfast.dto.AdminSupportTicketUpdateRequest;
import com.foodfast.dto.SupportTicketResponse;
import com.foodfast.entity.Restaurant;
import com.foodfast.security.CurrentUserService;
import com.foodfast.service.AdminOperationsService;
import com.foodfast.service.AdminAnalyticsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminOperationsController {
    private final AdminOperationsService service;
    private final AdminAnalyticsService analyticsService;
    private final CurrentUserService currentUser;

    public AdminOperationsController(
            AdminOperationsService service,
            AdminAnalyticsService analyticsService,
            CurrentUserService currentUser
    ) {
        this.service = service;
        this.analyticsService = analyticsService;
        this.currentUser = currentUser;
    }

    @GetMapping("/overview")
    public Map<String, Object> overview() {
        return service.overview();
    }

    @GetMapping("/analytics")
    public AdminAnalyticsResponse analytics(@RequestParam(defaultValue = "30") int days) {
        return analyticsService.report(days);
    }

    @GetMapping("/restaurants/{restaurantId}/details")
    public Map<String, Object> restaurantDetails(@PathVariable String restaurantId) {
        return service.restaurantDetails(restaurantId);
    }

    @PatchMapping("/restaurants/{restaurantId}/lifecycle")
    public ResponseEntity<Restaurant> updateRestaurantLifecycle(
            @PathVariable String restaurantId,
            @RequestBody @Valid AdminRestaurantStatusRequest request
    ) {
        return ResponseEntity.ok(service.updateRestaurantLifecycle(
                restaurantId, request, currentUser.requireUser()));
    }

    @GetMapping("/customers/{customerId}/details")
    public Map<String, Object> customerDetails(@PathVariable String customerId) {
        return service.customerDetails(customerId);
    }

    @GetMapping("/support-queue")
    public List<Map<String, Object>> supportQueue() {
        return service.supportQueue();
    }

    @PatchMapping("/support-tickets/{ticketId}")
    public SupportTicketResponse updateSupportTicket(
            @PathVariable String ticketId,
            @RequestBody @Valid AdminSupportTicketUpdateRequest request
    ) {
        return service.updateSupportTicket(ticketId, request, currentUser.requireUser());
    }
}
