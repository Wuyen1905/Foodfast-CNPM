package com.foodfast.controller;

import com.foodfast.dto.CreateOrderRequest;
import com.foodfast.dto.CancelOrderRequest;
import com.foodfast.dto.OrderQuoteResponse;
import com.foodfast.entity.Order;
import com.foodfast.entity.OrderStatus;
import com.foodfast.entity.OrderStatusHistory;
import com.foodfast.entity.CancellationActor;
import com.foodfast.entity.User;
import com.foodfast.exception.ForbiddenOperationException;
import com.foodfast.security.CurrentUserService;
import com.foodfast.service.OrderService;
import com.foodfast.service.OrderCancellationService;
import com.foodfast.service.AdminAuditLogService;
import com.foodfast.service.IdempotentOrderCreationService;
import com.foodfast.service.IdempotencyKeyReuseException;
import com.foodfast.service.OrderStatusHistoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping({"/api/orders", "/orders"})
public class OrderController {

    private final OrderService orderService;
    private final OrderCancellationService orderCancellationService;
    private final IdempotentOrderCreationService idempotentOrderCreationService;
    private final CurrentUserService currentUserService;
    private final AdminAuditLogService auditLogService;
    private final OrderStatusHistoryService orderStatusHistoryService;

    public OrderController(
            OrderService orderService,
            OrderCancellationService orderCancellationService,
            IdempotentOrderCreationService idempotentOrderCreationService,
            CurrentUserService currentUserService,
            AdminAuditLogService auditLogService,
            OrderStatusHistoryService orderStatusHistoryService
    ) {
        this.orderService = orderService;
        this.orderCancellationService = orderCancellationService;
        this.idempotentOrderCreationService = idempotentOrderCreationService;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
        this.orderStatusHistoryService = orderStatusHistoryService;
    }

    @GetMapping
    public ResponseEntity<?> getOrders(@RequestParam(required = false) String paymentSessionId,
                                       @RequestParam(required = false) String phone,
                                       @RequestParam(required = false) String restaurant,
                                       @RequestParam(required = false) String restaurantId,
                                       @RequestParam(required = false) String userId) {
        try {
            User viewer = currentUserService.requireUser();
            String finalRestaurant;
            String finalUserId;
            String finalPaymentSessionId = paymentSessionId;
            String finalPhone = phone;
            if (currentUserService.isAdmin()) {
                finalRestaurant = (restaurantId != null && !restaurantId.isBlank())
                        ? restaurantId : restaurant;
                finalUserId = userId;
            } else if (currentUserService.isRestaurant()) {
                finalRestaurant = currentUserService.requireRestaurantId();
                finalUserId = null;
                finalPaymentSessionId = null;
            } else {
                finalRestaurant = null;
                finalUserId = viewer.getId();
                finalPaymentSessionId = null;
                finalPhone = null;
            }

            System.out.println("[OrderController] GET /api/orders - psId="
                    + paymentSessionId + ", phone=" + phone
                    + ", restaurantId=" + finalRestaurant + ", userId=" + finalUserId);

            List<Order> orders = orderService.findOrders(
                    finalPaymentSessionId, finalPhone, finalRestaurant, finalUserId);

            return ResponseEntity.ok(orders != null ? orders : List.of());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Không thể tải danh sách đơn hàng: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable String id) {
        Optional<Order> order = orderService.findById(id);
        order.ifPresent(currentUserService::requireOrderAccess);
        return order.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping
    public ResponseEntity<?> createOrder(
            @RequestBody @Valid CreateOrderRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        try {
            User customer = currentUserService.requireUser();
            request.userId = customer.getId();
            request.customerName = customer.getName();
            request.customerEmail = customer.getEmail();
            IdempotentOrderCreationService.CreationResult result =
                    idempotentOrderCreationService.create(request, idempotencyKey);
            return ResponseEntity
                    .status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
                    .header("Idempotent-Replayed", String.valueOf(result.replayed()))
                    .body(result.order());
        } catch (IllegalArgumentException e) {
            String code = e.getMessage() != null && e.getMessage().contains("Idempotency-Key")
                    ? "INVALID_IDEMPOTENCY_KEY"
                    : "INVALID_ORDER";
            return ResponseEntity.badRequest().body(Map.of(
                    "code", code,
                    "message", e.getMessage()
            ));
        } catch (IdempotencyKeyReuseException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "code", "IDEMPOTENCY_KEY_REUSED",
                    "message", e.getMessage()
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "code", "ORDER_BUSINESS_RULE_VIOLATION",
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<OrderStatusHistory>> getOrderHistory(@PathVariable String id) {
        Order order = orderService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng: " + id));
        currentUserService.requireOrderAccess(order);
        return ResponseEntity.ok(orderStatusHistoryService.findByOrder(id));
    }

    @PostMapping("/quote")
    public ResponseEntity<?> quote(@RequestBody @Valid CreateOrderRequest request) {
        try {
            User customer = currentUserService.requireUser();
            request.userId = customer.getId();
            request.customerName = customer.getName();
            request.customerEmail = customer.getEmail();
            OrderQuoteResponse quote = orderService.quote(request);
            return ResponseEntity.ok(quote);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of(
                    "code", "INVALID_ORDER_QUOTE",
                    "message", exception.getMessage()
            ));
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Order> cancelOrder(
            @PathVariable String id,
            @RequestBody @Valid CancelOrderRequest request
    ) {
        User actor = currentUserService.requireUser();
        CancellationActor cancellationActor;
        String cancellationActorId;
        if (currentUserService.isCustomer()) {
            Order order = orderService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));
            currentUserService.requireOrderAccess(order);
            cancellationActor = CancellationActor.CUSTOMER;
            cancellationActorId = actor.getId();
        } else if (currentUserService.isRestaurant()) {
            currentUserService.requireOrderAccess(orderService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng")));
            cancellationActor = CancellationActor.RESTAURANT;
            cancellationActorId = currentUserService.requireRestaurantId();
        } else if (currentUserService.isAdmin()) {
            cancellationActor = CancellationActor.ADMIN;
            cancellationActorId = actor.getId();
        } else {
            throw new ForbiddenOperationException("Bạn không được hủy đơn hàng này");
        }
        Order cancelledOrder = orderCancellationService.cancel(
                id,
                cancellationActor,
                cancellationActorId,
                request
        );
        if (cancellationActor == CancellationActor.ADMIN) {
            auditLogService.record(
                    actor,
                    "order_status_changed",
                    "order",
                    cancelledOrder.getId(),
                    "Đơn " + cancelledOrder.getId(),
                    "Hủy đơn ngoại lệ: " + (StringUtils.hasText(request.detail)
                            ? request.detail.trim() : request.reasonCode.name()),
                    "critical"
            );
        }
        return ResponseEntity.ok(cancelledOrder);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> patchOrder(
            @PathVariable String id,
            @RequestBody Map<String, Object> body
    ) {
        try {
            Optional<Order> orderOpt = orderService.findById(id);
            if (orderOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Order order = orderOpt.get();
            if (!currentUserService.isAdmin()) {
                if (!currentUserService.isRestaurant()) {
                    throw new ForbiddenOperationException("Chỉ nhà hàng hoặc quản trị viên được cập nhật đơn");
                }
                currentUserService.requireOrderAccess(order);
            }
            Set<String> allowedFields = Set.of("status", "internalNotes");
            Optional<String> forbiddenField = body.keySet().stream()
                    .filter(field -> !allowedFields.contains(field))
                    .findFirst();
            if (forbiddenField.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "code", "READ_ONLY_ORDER_FIELD",
                        "message", "Không được cập nhật trực tiếp trường hệ thống: " + forbiddenField.get()
                ));
            }

            boolean updated = false;
            boolean requiresSave = false;

            // Process status
            if (body.containsKey("status")) {
                String rawStatus = String.valueOf(body.get("status"));
                OrderStatus status = OrderStatus.valueOf(
                        rawStatus.trim().toUpperCase(Locale.ROOT)
                );
                if (status == OrderStatus.CANCELLED) {
                    throw new IllegalStateException(
                            "Hủy hoặc từ chối đơn phải dùng quy trình có lý do"
                    );
                }
                if (status == OrderStatus.DELIVERING || status == OrderStatus.DELIVERED
                        || order.getStatusEnum() == OrderStatus.DELIVERING
                        || order.getStatusEnum() == OrderStatus.DELIVERED) {
                    throw new IllegalStateException(
                            "Trạng thái giao hàng chỉ được cập nhật qua quy trình phân công giao hàng"
                    );
                }
                order = orderService.updateStatus(
                        id,
                        status,
                        currentUserService.businessActorId(),
                        currentUserService.role(),
                        currentUserService.isAdmin() ? "ADMIN_PANEL" : "RESTAURANT_DASHBOARD",
                        null
                );
                updated = true;
            }

            if (body.containsKey("internalNotes")) {
                order.setInternalNotes(String.valueOf(body.get("internalNotes")));
                updated = true;
                requiresSave = true;
            }

            if (updated) {
                if (requiresSave) {
                    order = orderService.save(order);
                }
                return ResponseEntity.ok(order);
            }

            // No changes applied
            return ResponseEntity.ok(
                Map.of("message", "Không có thay đổi nào được áp dụng")
            );

        } catch (ForbiddenOperationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            // Invalid status value
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Trạng thái không hợp lệ: " + e.getMessage())
            );
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

}
