package com.foodfast.controller;

import com.foodfast.dto.CreateOrderGroupRequest;
import com.foodfast.dto.OrderGroupQuoteResponse;
import com.foodfast.dto.OrderGroupResponse;
import com.foodfast.entity.User;
import com.foodfast.exception.ForbiddenOperationException;
import com.foodfast.security.CurrentUserService;
import com.foodfast.service.IdempotencyKeyReuseException;
import com.foodfast.service.OrderGroupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/order-groups")
public class OrderGroupController {

    private final OrderGroupService orderGroupService;
    private final CurrentUserService currentUserService;

    public OrderGroupController(
            OrderGroupService orderGroupService,
            CurrentUserService currentUserService
    ) {
        this.orderGroupService = orderGroupService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/quote")
    public ResponseEntity<?> quote(@RequestBody @Valid CreateOrderGroupRequest request) {
        try {
            applyCustomer(request);
            OrderGroupQuoteResponse quote = orderGroupService.quote(request);
            return ResponseEntity.ok(quote);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of(
                    "code", "INVALID_ORDER_GROUP_QUOTE",
                    "message", exception.getMessage()
            ));
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "code", "ORDER_GROUP_BUSINESS_RULE_VIOLATION",
                    "message", exception.getMessage()
            ));
        }
    }

    @PostMapping
    public ResponseEntity<?> create(
            @RequestBody @Valid CreateOrderGroupRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        try {
            applyCustomer(request);
            OrderGroupService.CreationResult result = orderGroupService.create(request, idempotencyKey);
            return ResponseEntity
                    .status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
                    .header("Idempotent-Replayed", String.valueOf(result.replayed()))
                    .body(result.group());
        } catch (IdempotencyKeyReuseException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "code", "IDEMPOTENCY_KEY_REUSED",
                    "message", exception.getMessage()
            ));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of(
                    "code", "INVALID_ORDER_GROUP",
                    "message", exception.getMessage()
            ));
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "code", "ORDER_GROUP_BUSINESS_RULE_VIOLATION",
                    "message", exception.getMessage()
            ));
        }
    }

    @GetMapping
    public ResponseEntity<List<OrderGroupResponse>> mine() {
        User user = currentUserService.requireUser();
        if (!currentUserService.isCustomer()) {
            throw new ForbiddenOperationException("Chỉ khách hàng được xem các nhóm đặt hàng của mình");
        }
        return ResponseEntity.ok(orderGroupService.findByUser(user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderGroupResponse> get(@PathVariable String id) {
        User user = currentUserService.requireUser();
        OrderGroupResponse group = orderGroupService.findById(id);
        if (!currentUserService.isAdmin() && !user.getId().equals(group.userId())) {
            throw new ForbiddenOperationException("Bạn không được truy cập nhóm đặt hàng này");
        }
        return ResponseEntity.ok(group);
    }

    private void applyCustomer(CreateOrderGroupRequest request) {
        User customer = currentUserService.requireUser();
        if (!currentUserService.isCustomer()) {
            throw new ForbiddenOperationException("Chỉ khách hàng được tạo nhóm đặt hàng");
        }
        request.userId = customer.getId();
        request.customerName = customer.getName();
        request.customerEmail = customer.getEmail();
    }
}
