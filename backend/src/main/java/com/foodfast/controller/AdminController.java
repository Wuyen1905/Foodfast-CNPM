package com.foodfast.controller;

import com.foodfast.dto.AdminActionRequest;
import com.foodfast.entity.AdminAuditLog;
import com.foodfast.entity.DeliveryAssignment;
import com.foodfast.entity.DeliveryStatus;
import com.foodfast.entity.Order;
import com.foodfast.entity.Restaurant;
import com.foodfast.entity.RestaurantCourier;
import com.foodfast.entity.RestaurantLifecycleStatus;
import com.foodfast.entity.User;
import com.foodfast.repository.DeliveryAssignmentRepository;
import com.foodfast.repository.OrderRepository;
import com.foodfast.repository.RestaurantCourierRepository;
import com.foodfast.repository.RestaurantRepository;
import com.foodfast.repository.UserRepository;
import com.foodfast.security.CurrentUserService;
import com.foodfast.service.AdminAuditLogService;
import com.foodfast.service.RevenuePolicy;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Admin Controller
 * Provides admin-specific endpoints matching frontend AdminApi expectations
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://192.168.0.100:5173",
        "http://192.168.0.100:5174",
        "http://192.168.0.100:5175",
        "http://localhost:8081"
})
public class AdminController {

    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;
    private final RestaurantCourierRepository courierRepository;
    private final DeliveryAssignmentRepository deliveryRepository;
    private final OrderRepository orderRepository;
    private final CurrentUserService currentUserService;
    private final AdminAuditLogService auditLogService;

    public AdminController(
            RestaurantRepository restaurantRepository,
            UserRepository userRepository,
            RestaurantCourierRepository courierRepository,
            DeliveryAssignmentRepository deliveryRepository,
            OrderRepository orderRepository,
            CurrentUserService currentUserService,
            AdminAuditLogService auditLogService) {
        this.restaurantRepository = restaurantRepository;
        this.userRepository = userRepository;
        this.courierRepository = courierRepository;
        this.deliveryRepository = deliveryRepository;
        this.orderRepository = orderRepository;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    /**
     * GET /api/admin/stats
     * Get admin statistics matching AdminStats interface
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAdminStats() {
        List<Restaurant> restaurants = restaurantRepository.findAll();
        List<User> users = userRepository.findAll();
        List<RestaurantCourier> couriers = courierRepository.findAll();
        List<DeliveryAssignment> deliveries = deliveryRepository.findAll();
        List<Order> orders = orderRepository.findAll();

        List<User> customers = users.stream()
                .filter(u -> "customer".equals(u.getRole()) || "suspended".equals(u.getRole()))
                .collect(Collectors.toList());

        List<Restaurant> activeRestaurants = restaurants.stream()
                .filter(r -> r.getAdminStatus() == RestaurantLifecycleStatus.ACTIVE)
                .collect(Collectors.toList());

        long totalRevenue = orders.stream()
                .mapToLong(RevenuePolicy::netRevenue)
                .sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCustomers", (long) customers.size());
        stats.put("totalRestaurants", (long) restaurants.size());
        stats.put("activeRestaurants", (long) activeRestaurants.size());
        stats.put("pendingRestaurants", restaurants.stream()
                .filter(restaurant -> restaurant.getAdminStatus() == RestaurantLifecycleStatus.PENDING)
                .count());
        stats.put("totalOrders", (long) orders.size());
        stats.put("totalRevenue", totalRevenue);
        stats.put("totalCouriers", (long) couriers.size());
        stats.put("activeCouriers", couriers.stream().filter(RestaurantCourier::isActive).count());
        stats.put("activeDeliveries", deliveries.stream()
                .filter(delivery -> delivery.getStatus() == DeliveryStatus.ASSIGNED
                        || delivery.getStatus() == DeliveryStatus.DELIVERING)
                .count());
        stats.put("failedDeliveries", deliveries.stream()
                .filter(delivery -> delivery.getStatus() == DeliveryStatus.FAILED)
                .count());

        return ResponseEntity.ok(stats);
    }

    /**
     * PATCH /api/admin/restaurants/{id}/status
     * Update restaurant status
     */
    @PatchMapping("/restaurants/{id}/status")
    public ResponseEntity<?> updateRestaurantStatus(
            @PathVariable String id,
            @RequestBody @Valid AdminActionRequest request) {
        Optional<Restaurant> restaurantOpt = restaurantRepository.findById(id);
        if (restaurantOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Restaurant restaurant = restaurantOpt.get();
        if (request.getIsActive() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Thiếu trạng thái hoạt động của nhà hàng"));
        }

        RestaurantLifecycleStatus targetStatus = request.getIsActive()
                ? RestaurantLifecycleStatus.ACTIVE
                : RestaurantLifecycleStatus.SUSPENDED;
        restaurant.setAdminStatus(targetStatus);
        restaurant.setAdminStatusReason(request.getReason().trim());
        restaurant.setAdminStatusUpdatedAt(System.currentTimeMillis());
        if (!request.getIsActive()) restaurant.setAcceptingOrders(false);
        restaurantRepository.save(restaurant);
        String action = request.getIsActive()
                ? "restaurant_activated"
                : "restaurant_suspended";
        auditLogService.record(
                currentUserService.requireUser(),
                action,
                "restaurant",
                restaurant.getId(),
                restaurant.getName(),
                request.getReason().trim(),
                request.getIsActive() ? "info" : "warning"
        );
        return ResponseEntity.ok(restaurant);
    }

    /**
     * PATCH /api/admin/users/{id}/suspend
     * Suspend a customer by changing role to "suspended"
     */
    @PatchMapping("/users/{id}/suspend")
    public ResponseEntity<?> suspendCustomer(
            @PathVariable String id,
            @RequestBody @Valid AdminActionRequest request) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty() || !"customer".equals(userOpt.get().getRole())) {
            return ResponseEntity.notFound().build();
        }
        User user = userOpt.get();
        user.setRole("suspended");
        user.setSuspensionReason(request.getReason().trim());
        user.setAccountStatusUpdatedAt(System.currentTimeMillis());
        userRepository.save(user);
        auditLogService.record(
                currentUserService.requireUser(), "customer_suspended", "customer",
                user.getId(), user.getName(), request.getReason().trim(), "warning"
        );
        return ResponseEntity.ok(Map.of("message", "Đã tạm ngưng tài khoản khách hàng"));
    }

    /**
     * PATCH /api/admin/users/{id}/reactivate
     * Reactivate a customer by changing role back to "customer"
     */
    @PatchMapping("/users/{id}/reactivate")
    public ResponseEntity<?> reactivateCustomer(
            @PathVariable String id,
            @RequestBody @Valid AdminActionRequest request) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty() || !"suspended".equals(userOpt.get().getRole())) {
            return ResponseEntity.notFound().build();
        }
        User user = userOpt.get();
        user.setRole("customer");
        user.setAccountStatusUpdatedAt(System.currentTimeMillis());
        userRepository.save(user);
        auditLogService.record(
                currentUserService.requireUser(), "customer_activated", "customer",
                user.getId(), user.getName(), request.getReason().trim(), "info"
        );
        return ResponseEntity.ok(Map.of("message", "Đã kích hoạt lại tài khoản khách hàng"));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<List<AdminAuditLog>> getAuditLogs(
            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(auditLogService.recent(limit));
    }

    /**
     * GET /api/admin/restaurants
     * Get all restaurants with admin-specific enriched data
     */
    @GetMapping("/restaurants")
    public ResponseEntity<List<Map<String, Object>>> getAllAdminRestaurants() {
        List<Restaurant> restaurants = restaurantRepository.findAll();
        List<Order> orders = orderRepository.findAll();
        List<RestaurantCourier> couriers = courierRepository.findAll();
        List<User> users = userRepository.findAll();

        List<Map<String, Object>> adminRestaurants = restaurants.stream().map(restaurant -> {
            // Calculate restaurant-specific stats
            List<Order> restaurantOrders = orders.stream()
                    .filter(o -> restaurant.getId().equals(o.getRestaurantId()) || restaurant.getId().equals(o.getRestaurant()))
                    .collect(Collectors.toList());

            List<RestaurantCourier> restaurantCouriers = couriers.stream()
                    .filter(courier -> restaurant.getId().equals(courier.getRestaurantId()))
                    .collect(Collectors.toList());

            Optional<User> owner = users.stream()
                    .filter(u -> restaurant.getOwnerId().equals(u.getId()))
                    .findFirst();

            Map<String, Object> adminRestaurant = new HashMap<>();
            adminRestaurant.put("id", restaurant.getId());
            adminRestaurant.put("name", restaurant.getName());
            adminRestaurant.put("category", restaurant.getCategory() != null ? restaurant.getCategory() : "Chưa phân loại");
            adminRestaurant.put("status", restaurant.getAdminStatus().name());
            adminRestaurant.put("statusReason", restaurant.getAdminStatusReason());
            adminRestaurant.put("statusUpdatedAt", restaurant.getAdminStatusUpdatedAt());
            adminRestaurant.put("ownerId", restaurant.getOwnerId());
            adminRestaurant.put("ownerName", owner.map(User::getName).orElse("Không xác định"));
            adminRestaurant.put("totalOrders", restaurantOrders.size());
            adminRestaurant.put(
                    "totalRevenue",
                    restaurantOrders.stream().mapToLong(RevenuePolicy::netRevenue).sum()
            );
            adminRestaurant.put("rating", restaurant.getRating() != null ? restaurant.getRating() : 0.0);
            adminRestaurant.put("courierCount", restaurantCouriers.size());
            adminRestaurant.put("location", restaurant.getLocation() != null ? restaurant.getLocation() : "Chưa cập nhật");
            adminRestaurant.put("createdAt", restaurant.getCreatedAt() != null ? restaurant.getCreatedAt() : System.currentTimeMillis());

            return adminRestaurant;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(adminRestaurants);
    }

    /**
     * GET /api/admin/customers
     * Get all customers with admin-specific enriched data
     */
    @GetMapping("/customers")
    public ResponseEntity<List<Map<String, Object>>> getAllAdminCustomers() {
        List<User> users = userRepository.findAll();
        List<Order> orders = orderRepository.findAll();

        List<Map<String, Object>> adminCustomers = users.stream()
                .filter(u -> "customer".equals(u.getRole()) || "suspended".equals(u.getRole()))
                .map(user -> {
                    List<Order> customerOrders = orders.stream()
                            .filter(o -> user.getId().equals(o.getUserId()))
                            .collect(Collectors.toList());

                    Optional<Order> lastOrder = customerOrders.stream()
                            .max(Comparator.comparing(o -> o.getCreatedAtInstant() != null ? o.getCreatedAtInstant().toEpochMilli() : 0L));

                    Map<String, Object> adminCustomer = new HashMap<>();
                    adminCustomer.put("id", user.getId());
                    adminCustomer.put("name", user.getName());
                    adminCustomer.put("phone", user.getPhone() != null ? user.getPhone() : "");
                    adminCustomer.put("email", user.getEmail() != null ? user.getEmail() : "");
                    adminCustomer.put("totalOrders", customerOrders.size());
                    adminCustomer.put(
                            "totalSpend",
                            customerOrders.stream().mapToLong(RevenuePolicy::netRevenue).sum()
                    );
                    // Map role to accountStatus: "customer" -> "Active", "suspended" -> "Suspended"
                    String accountStatus = "suspended".equals(user.getRole()) ? "Suspended" : "Active";
                    adminCustomer.put("accountStatus", accountStatus);
                    adminCustomer.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt() : System.currentTimeMillis());
                    // Convert Instant to Long timestamp for frontend
                    adminCustomer.put("lastOrderDate", lastOrder
                            .map(o -> o.getCreatedAtInstant() != null ? o.getCreatedAtInstant().toEpochMilli() : null)
                            .orElse(null));
                    adminCustomer.put("lastLoginAt", user.getLastLoginAt());
                    adminCustomer.put("suspensionReason", user.getSuspensionReason());
                    adminCustomer.put("accountStatusUpdatedAt", user.getAccountStatusUpdatedAt());

                    return adminCustomer;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(adminCustomers);
    }

}

