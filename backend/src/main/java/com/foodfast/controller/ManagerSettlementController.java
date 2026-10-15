package com.foodfast.controller;

import com.foodfast.dto.CreateSettlementRequest;
import com.foodfast.dto.PaySettlementRequest;
import com.foodfast.dto.SettlementDashboardResponse;
import com.foodfast.dto.SettlementPreviewResponse;
import com.foodfast.dto.SettlementResponse;
import com.foodfast.security.CurrentUserService;
import com.foodfast.service.RestaurantSettlementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/manager/settlements")
public class ManagerSettlementController {
    private final RestaurantSettlementService settlementService;
    private final CurrentUserService currentUserService;

    public ManagerSettlementController(
            RestaurantSettlementService settlementService,
            CurrentUserService currentUserService
    ) {
        this.settlementService = settlementService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/dashboard")
    public SettlementDashboardResponse dashboard(
            @RequestParam Instant periodStart,
            @RequestParam Instant periodEnd
    ) {
        return settlementService.dashboard(currentUserService.requireUser(), periodStart, periodEnd);
    }

    @GetMapping("/preview")
    public List<SettlementPreviewResponse> preview(
            @RequestParam Instant periodStart,
            @RequestParam Instant periodEnd
    ) {
        return settlementService.preview(currentUserService.requireUser(), periodStart, periodEnd);
    }

    @GetMapping("/{settlementId}")
    public SettlementResponse detail(@PathVariable String settlementId) {
        return settlementService.findOne(currentUserService.requireUser(), settlementId);
    }

    @PostMapping
    public ResponseEntity<SettlementResponse> create(
            @Valid @RequestBody CreateSettlementRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(settlementService.create(currentUserService.requireUser(), request));
    }

    @PatchMapping("/{settlementId}/submit")
    public SettlementResponse submit(@PathVariable String settlementId) {
        return settlementService.submit(currentUserService.requireUser(), settlementId);
    }

    @PatchMapping("/{settlementId}/pay")
    public SettlementResponse pay(
            @PathVariable String settlementId,
            @Valid @RequestBody PaySettlementRequest request
    ) {
        return settlementService.markPaid(
                currentUserService.requireUser(), settlementId, request.paymentReference());
    }
}
