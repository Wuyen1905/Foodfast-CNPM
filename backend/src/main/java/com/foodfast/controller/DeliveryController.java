package com.foodfast.controller;

import com.foodfast.dto.AssignDeliveryRequest;
import com.foodfast.dto.DeliveryDetailsResponse;
import com.foodfast.dto.AdminDeliveryQueueResponse;
import com.foodfast.dto.FailDeliveryRequest;
import com.foodfast.dto.RestaurantCompleteDeliveryRequest;
import com.foodfast.entity.DeliveryAssignment;
import com.foodfast.service.DeliveryAssignmentService;
import com.foodfast.service.OrderService;
import com.foodfast.security.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class DeliveryController {

    private final DeliveryAssignmentService deliveryService;
    private final OrderService orderService;
    private final CurrentUserService currentUserService;

    public DeliveryController(
            DeliveryAssignmentService deliveryService,
            OrderService orderService,
            CurrentUserService currentUserService
    ) {
        this.deliveryService = deliveryService;
        this.orderService = orderService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/admin/deliveries/assign")
    public ResponseEntity<DeliveryDetailsResponse> assign(
            @RequestBody @Valid AssignDeliveryRequest request
    ) {
        if (!currentUserService.isAdmin()) throw new com.foodfast.exception.ForbiddenOperationException("Chỉ Admin được phân phối shipper");
        DeliveryAssignment assignment = deliveryService.assignDelivery(
                request.orderId(),
                request.courierId(),
                request.estimatedDeliveryAt(),
                request.deliveryNote()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(assignment));
    }

    @GetMapping("/admin/deliveries/queue")
    public List<AdminDeliveryQueueResponse> adminQueue() {
        if (!currentUserService.isAdmin()) throw new com.foodfast.exception.ForbiddenOperationException("Chỉ Admin được xem hàng đợi giao hàng");
        return deliveryService.findAdminQueue().stream().map(assignment -> {
            var order = orderService.findById(assignment.getOrderId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));
            return AdminDeliveryQueueResponse.from(
                    order, assignment, deliveryService.findCourier(assignment.getCourierId()));
        }).toList();
    }

    @GetMapping("/deliveries/order/{orderId}")
    public ResponseEntity<DeliveryDetailsResponse> getForOrder(@PathVariable String orderId) {
        currentUserService.requireOrderAccess(orderService.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng")));
        return deliveryService.findCurrentByOrderId(orderId)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/restaurants/{restaurantId}/deliveries")
    public List<DeliveryDetailsResponse> listForRestaurant(@PathVariable String restaurantId) {
        currentUserService.requireRestaurantAccess(restaurantId);
        return deliveryService.findByRestaurant(restaurantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping("/admin/deliveries/{assignmentId}/start")
    public DeliveryDetailsResponse start(@PathVariable String assignmentId) {
        if (!currentUserService.isAdmin()) throw new com.foodfast.exception.ForbiddenOperationException("Chỉ Admin được điều phối giao hàng");
        return toResponse(deliveryService.startDelivery(
                assignmentId, currentUserService.requireUser().getId()));
    }

    @PostMapping("/deliveries/{assignmentId}/customer-confirm")
    public DeliveryDetailsResponse confirmReceivedByCustomer(@PathVariable String assignmentId) {
        return toResponse(deliveryService.confirmReceivedByCustomer(
                assignmentId,
                currentUserService.requireUser().getId()
        ));
    }

    @PostMapping("/admin/deliveries/{assignmentId}/fail")
    public DeliveryDetailsResponse fail(
            @PathVariable String assignmentId,
            @RequestBody @Valid FailDeliveryRequest request
    ) {
        if (!currentUserService.isAdmin()) throw new com.foodfast.exception.ForbiddenOperationException("Chỉ Admin được điều phối giao hàng");
        return toResponse(deliveryService.failDelivery(
                assignmentId, request.reason(), currentUserService.requireUser().getId()));
    }

    @PostMapping("/admin/deliveries/{assignmentId}/cancel")
    public DeliveryDetailsResponse cancel(@PathVariable String assignmentId) {
        if (!currentUserService.isAdmin()) throw new com.foodfast.exception.ForbiddenOperationException("Chỉ Admin được điều phối giao hàng");
        return toResponse(deliveryService.cancelDelivery(assignmentId));
    }

    private DeliveryDetailsResponse toResponse(DeliveryAssignment assignment) {
        return DeliveryDetailsResponse.from(
                assignment,
                deliveryService.findCourier(assignment.getCourierId())
        );
    }
}
