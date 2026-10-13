package com.foodfast.controller;

import com.foodfast.dto.CreateVNPayPaymentRequest;
import com.foodfast.dto.VNPayIpnResponse;
import com.foodfast.dto.VNPayPaymentResult;
import com.foodfast.service.VNPayPaymentService;
import com.foodfast.security.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.NoSuchElementException;
import java.net.URI;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final VNPayPaymentService vnpayService;
    private final CurrentUserService currentUserService;
    private final String frontendReturnUrl;

    public PaymentController(
            VNPayPaymentService vnpayService,
            CurrentUserService currentUserService,
            @Value("${vnpay.frontendReturnUrl:http://localhost:5173/vnpay-return}") String frontendReturnUrl
    ) {
        this.vnpayService = vnpayService;
        this.currentUserService = currentUserService;
        this.frontendReturnUrl = frontendReturnUrl;
    }

    @GetMapping("/vnpay/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "enabled", vnpayService.isAvailable(),
                "message", vnpayService.availabilityMessage()
        ));
    }

    @PostMapping("/vnpay/create")
    public ResponseEntity<?> createPayment(
            @RequestBody @Valid CreateVNPayPaymentRequest body,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest request
    ) {
        if (!vnpayService.isAvailable()) {
            return ResponseEntity.status(503).body(Map.of(
                    "enabled", false,
                    "message", vnpayService.availabilityMessage()
            ));
        }
        try {
            String url = vnpayService.createPaymentUrl(
                    body.orderId(),
                    idempotencyKey,
                    request,
                    currentUserService.requireUser().getId()
            );
            return ResponseEntity.ok(Map.of("url", url));
        } catch (NoSuchElementException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", exception.getMessage()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "message", "Không thể tạo đường dẫn thanh toán VNPay"
            ));
        }
    }

    @GetMapping("/vnpay/return")
    public ResponseEntity<VNPayPaymentResult> handleReturn(@RequestParam Map<String, String> params) {
        return ResponseEntity.ok(vnpayService.handleReturn(params));
    }

    /**
     * VNPay redirects the customer's browser to this public endpoint. The backend
     * then returns the browser to the local frontend so its login/session storage
     * remains on the localhost origin. The frontend still calls /vnpay/return to
     * verify the signed result; this redirect never marks an order as paid.
     */
    @GetMapping("/vnpay/browser-return")
    public ResponseEntity<Void> handleBrowserReturn(@RequestParam MultiValueMap<String, String> params) {
        URI location = UriComponentsBuilder
                .fromUriString(frontendReturnUrl)
                .queryParams(params)
                .build()
                .encode()
                .toUri();
        return ResponseEntity.status(HttpStatus.FOUND).location(location).build();
    }

    @GetMapping("/vnpay/ipn")
    public ResponseEntity<VNPayIpnResponse> handleIpn(@RequestParam Map<String, String> params) {
        return ResponseEntity.ok(vnpayService.handleIpn(params));
    }

    @GetMapping("/vnpay/orders/{orderId}/status")
    public ResponseEntity<VNPayPaymentResult> reconcile(@PathVariable String orderId) {
        currentUserService.requireOrderAccess(vnpayService.requireOrder(orderId));
        return ResponseEntity.ok(vnpayService.reconcile(orderId));
    }
}
