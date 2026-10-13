package com.foodfast.controller;

import com.foodfast.dto.ValidateVoucherRequest;
import com.foodfast.dto.VoucherStatusRequest;
import com.foodfast.dto.VoucherUpsertRequest;
import com.foodfast.security.CurrentUserService;
import com.foodfast.service.VoucherManagementService;
import com.foodfast.service.VoucherService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/vouchers")
public class VoucherController {
    private final VoucherService voucherService;
    private final VoucherManagementService managementService;
    private final CurrentUserService currentUser;

    public VoucherController(
            VoucherService voucherService,
            VoucherManagementService managementService,
            CurrentUserService currentUser
    ) {
        this.voucherService = voucherService;
        this.managementService = managementService;
        this.currentUser = currentUser;
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validate(@RequestBody ValidateVoucherRequest request) {
        try {
            return ResponseEntity.ok(voucherService.preview(
                    request.code,
                    request.restaurantId,
                    currentUser.requireUser().getId(),
                    request.subtotal,
                    Instant.now()
            ));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
        }
    }

    @GetMapping("/available")
    public ResponseEntity<?> available(
            @RequestParam String restaurantId,
            @RequestParam(defaultValue = "0") int subtotal
    ) {
        return ResponseEntity.ok(voucherService.listAvailable(
                restaurantId,
                currentUser.requireUser().getId(),
                subtotal,
                Instant.now()
        ));
    }

    @GetMapping("/manage")
    public ResponseEntity<?> listManaged(
            @RequestParam(required = false) String restaurantId
    ) {
        boolean admin = currentUser.isAdmin();
        String target = admin ? restaurantId : currentUser.requireRestaurantId();
        return ResponseEntity.ok(managementService.list(target, admin));
    }

    @PostMapping("/manage")
    public ResponseEntity<?> create(@RequestBody VoucherUpsertRequest request) {
        boolean admin = currentUser.isAdmin();
        String restaurantId = admin ? null : currentUser.requireRestaurantId();
        return ResponseEntity.status(201).body(managementService.create(request, restaurantId, admin));
    }

    @PutMapping("/manage/{code}")
    public ResponseEntity<?> update(
            @PathVariable String code,
            @RequestBody VoucherUpsertRequest request
    ) {
        boolean admin = currentUser.isAdmin();
        String restaurantId = admin ? null : currentUser.requireRestaurantId();
        return ResponseEntity.ok(managementService.update(code, request, restaurantId, admin));
    }

    @PatchMapping("/manage/{code}/active")
    public ResponseEntity<?> setActive(
            @PathVariable String code,
            @RequestBody VoucherStatusRequest request
    ) {
        boolean admin = currentUser.isAdmin();
        String restaurantId = admin ? null : currentUser.requireRestaurantId();
        return ResponseEntity.ok(managementService.setActive(
                code, request.active(), restaurantId, admin
        ));
    }
}
