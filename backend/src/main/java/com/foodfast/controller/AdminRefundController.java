package com.foodfast.controller;

import com.foodfast.dto.CompleteRefundRequest;
import com.foodfast.entity.Order;
import com.foodfast.security.CurrentUserService;
import com.foodfast.service.AdminAuditLogService;
import com.foodfast.service.OrderRefundService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/payments")
public class AdminRefundController {

    private final OrderRefundService refundService;
    private final CurrentUserService currentUserService;
    private final AdminAuditLogService auditLogService;

    public AdminRefundController(
            OrderRefundService refundService,
            CurrentUserService currentUserService,
            AdminAuditLogService auditLogService
    ) {
        this.refundService = refundService;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    @PatchMapping("/orders/{orderId}/refund/complete")
    public Order completeRefund(
            @PathVariable String orderId,
            @RequestBody @Valid CompleteRefundRequest request
    ) {
        var admin = currentUserService.requireUser();
        Order order = refundService.complete(orderId, request);
        auditLogService.record(
                admin,
                "refund_completed",
                "order",
                order.getId(),
                "Hoàn tiền đơn " + order.getId(),
                "Đã đối soát " + order.getPartialRefundAmount()
                        + "đ; mã giao dịch " + order.getRefundTransactionReference(),
                "warning"
        );
        return order;
    }
}
