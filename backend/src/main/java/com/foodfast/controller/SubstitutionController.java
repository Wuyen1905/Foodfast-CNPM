package com.foodfast.controller;

import com.foodfast.dto.CreateSubstitutionProposalRequest;
import com.foodfast.dto.RespondSubstitutionRequest;
import com.foodfast.entity.Order;
import com.foodfast.exception.ForbiddenOperationException;
import com.foodfast.security.CurrentUserService;
import com.foodfast.service.OrderService;
import com.foodfast.service.SubstitutionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/orders/{orderId}/substitutions")
public class SubstitutionController {
    private final SubstitutionService service;
    private final OrderService orderService;
    private final CurrentUserService currentUser;

    public SubstitutionController(
            SubstitutionService service,
            OrderService orderService,
            CurrentUserService currentUser
    ) {
        this.service = service;
        this.orderService = orderService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public ResponseEntity<?> list(@PathVariable String orderId) {
        try {
            requireAccessibleOrder(orderId);
            return ResponseEntity.ok(service.list(orderId));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> propose(
            @PathVariable String orderId,
            @RequestBody CreateSubstitutionProposalRequest request
    ) {
        try {
            Order order = requireAccessibleOrder(orderId);
            String authenticatedRestaurantId = currentUser.isAdmin()
                    ? order.getRestaurantId()
                    : currentUser.requireRestaurantId();
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    service.propose(orderId, authenticatedRestaurantId, request)
            );
        } catch (ForbiddenOperationException error) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", error.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException error) {
            return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
        }
    }

    @PostMapping("/{proposalId}/respond")
    public ResponseEntity<?> respond(
            @PathVariable String orderId,
            @PathVariable Long proposalId,
            @RequestBody RespondSubstitutionRequest request
    ) {
        try {
            requireAccessibleOrder(orderId);
            return ResponseEntity.ok(service.respond(
                    orderId,
                    proposalId,
                    currentUser.requireUser().getId(),
                    request
            ));
        } catch (ForbiddenOperationException error) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", error.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException error) {
            return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
        }
    }

    private Order requireAccessibleOrder(String orderId) {
        Order order = orderService.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng: " + orderId));
        currentUser.requireOrderAccess(order);
        return order;
    }
}
