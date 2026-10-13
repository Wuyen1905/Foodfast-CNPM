package com.foodfast.controller;

import com.foodfast.dto.AssignDeliveryRequest;
import com.foodfast.dto.DeliveryDetailsResponse;
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

    @PostMapping("/deliveries/assign")
    public ResponseEntity<DeliveryDetailsResponse> assign(
            @RequestBody @Valid AssignDeliveryRequest request
    ) {
        currentUserService.requireOrderAccess(orderService.findById(request.orderId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng")));
        DeliveryAssignment assignment = deliveryService.assignDelivery(
                request.orderId(),
                request.courierId(),
                request.estimatedDeliveryAt(),
                request.deliveryNote()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(assignment));
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

    @PostMapping("/deliveries/{assignmentId}/start")
    public DeliveryDetailsResponse start(@PathVariable String assignmentId) {
        requireAssignmentAccess(assignmentId);
        return toResponse(deliveryService.startDelivery(assignmentId));
    }

    @PostMapping("/deliveries/{assignmentId}/customer-confirm")
    public DeliveryDetailsResponse confirmReceivedByCustomer(@PathVariable String assignmentId) {
        return toResponse(deliveryService.confirmReceivedByCustomer(
                assignmentId,
                currentUserService.requireUser().getId()
        ));
    }

    @PostMapping("/restaurants/{restaurantId}/deliveries/{assignmentId}/complete-override")
    public DeliveryDetailsResponse completeByRestaurantOverride(
            @PathVariable String restaurantId,
            @PathVariable String assignmentId,
            @RequestBody @Valid RestaurantCompleteDeliveryRequest request
    ) {
        currentUserService.requireRestaurantAccess(restaurantId);
        requireAssignmentAccess(assignmentId);
        return toResponse(deliveryService.completeByRestaurantOverride(
                restaurantId,
                assignmentId,
                request.reason()
        ));
    }

    @PostMapping("/deliveries/{assignmentId}/fail")
    public DeliveryDetailsResponse fail(
            @PathVariable String assignmentId,
            @RequestBody @Valid FailDeliveryRequest request
    ) {
        requireAssignmentAccess(assignmentId);
        return toResponse(deliveryService.failDelivery(assignmentId, request.reason()));
    }

    @PostMapping("/deliveries/{assignmentId}/cancel")
    public DeliveryDetailsResponse cancel(@PathVariable String assignmentId) {
        requireAssignmentAccess(assignmentId);
        return toResponse(deliveryService.cancelDelivery(assignmentId));
    }

    private void requireAssignmentAccess(String assignmentId) {
        currentUserService.requireRestaurantAccess(
                deliveryService.findById(assignmentId).getRestaurantId());
    }

    private DeliveryDetailsResponse toResponse(DeliveryAssignment assignment) {
        return DeliveryDetailsResponse.from(
                assignment,
                deliveryService.findCourier(assignment.getCourierId())
        );
    }
}
