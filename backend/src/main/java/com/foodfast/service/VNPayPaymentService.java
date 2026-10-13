package com.foodfast.service;

import com.foodfast.dto.VNPayIpnResponse;
import com.foodfast.dto.VNPayPaymentResult;
import com.foodfast.entity.Order;
import com.foodfast.entity.OrderStatus;
import com.foodfast.event.OrderEventPublisher;
import com.foodfast.repository.OrderRepository;
import com.foodfast.exception.ForbiddenOperationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class VNPayPaymentService {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNPAY_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(VIETNAM_ZONE);

    private final OrderRepository orderRepository;
    private final OrderEventPublisher orderEventPublisher;
    private final NotificationService notificationService;
    private final CustomerNotificationService customerNotificationService;
    private final boolean enabled;
    private final String tmnCode;
    private final String hashSecret;
    private final String payUrl;
    private final String returnUrl;
    private final int expireMinutes;

    public VNPayPaymentService(
            OrderRepository orderRepository,
            OrderEventPublisher orderEventPublisher,
            NotificationService notificationService,
            CustomerNotificationService customerNotificationService,
            @Value("${vnpay.enabled:false}") boolean enabled,
            @Value("${vnpay.tmnCode:}") String tmnCode,
            @Value("${vnpay.hashSecret:}") String hashSecret,
            @Value("${vnpay.payUrl:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}") String payUrl,
            @Value("${vnpay.returnUrl:http://localhost:5173/vnpay-return}") String returnUrl,
            @Value("${vnpay.expireMinutes:15}") int expireMinutes
    ) {
        this.orderRepository = orderRepository;
        this.orderEventPublisher = orderEventPublisher;
        this.notificationService = notificationService;
        this.customerNotificationService = customerNotificationService;
        this.enabled = enabled;
        this.tmnCode = valueOrEmpty(tmnCode);
        this.hashSecret = valueOrEmpty(hashSecret);
        this.payUrl = payUrl;
        this.returnUrl = returnUrl;
        this.expireMinutes = Math.max(5, expireMinutes);
    }

    public boolean isAvailable() {
        return enabled
                && !tmnCode.isBlank()
                && !hashSecret.isBlank()
                && !"YOUR_TMN_CODE".equalsIgnoreCase(tmnCode)
                && !"YOUR_HASH_SECRET".equalsIgnoreCase(hashSecret);
    }

    public String availabilityMessage() {
        if (!enabled) {
            return "VNPay đang tắt trong cấu hình. Vui lòng chọn thanh toán khi nhận hàng.";
        }
        if (!isAvailable()) {
            return "VNPay chưa có TmnCode hoặc HashSecret hợp lệ.";
        }
        return "VNPay Sandbox đã sẵn sàng.";
    }

    @Transactional
    public String createPaymentUrl(String orderId, HttpServletRequest request) {
        return createPaymentUrl(orderId, "legacy-" + UUID.randomUUID(), request);
    }

    @Transactional
    public String createPaymentUrl(String orderId, String rawIdempotencyKey, HttpServletRequest request) {
        return createPaymentUrl(orderId, rawIdempotencyKey, request, null);
    }

    @Transactional
    public String createPaymentUrl(
            String orderId,
            String rawIdempotencyKey,
            HttpServletRequest request,
            String authenticatedCustomerId
    ) {
        requireAvailable();

        String idempotencyKey = normalizeIdempotencyKey(rawIdempotencyKey);
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn hàng: " + orderId));

        if (!PaymentStatusPolicy.isVNPay(order)) {
            throw new IllegalArgumentException("Đơn hàng không sử dụng phương thức VNPay");
        }
        if (PaymentStatusPolicy.isPaid(order)) {
            throw new IllegalStateException("Đơn hàng đã được thanh toán");
        }
        if (authenticatedCustomerId != null
                && !authenticatedCustomerId.equals(order.getUserId())) {
            throw new ForbiddenOperationException("Bạn không được thanh toán đơn hàng này");
        }
        if (order.getStatusEnum() != OrderStatus.PENDING) {
            throw new IllegalStateException("Chỉ đơn đang chờ thanh toán mới được chuyển sang VNPay");
        }
        if (order.getTotal() <= 0) {
            throw new IllegalArgumentException("Số tiền đơn hàng không hợp lệ");
        }

        if (!PaymentStatusPolicy.isRetryable(order)) {
            throw new IllegalStateException("Trạng thái thanh toán hiện tại không cho phép tạo giao dịch mới");
        }

        Instant now = Instant.now();
        boolean activeRequest = order.getPaymentRequestUrl() != null
                && !order.getPaymentRequestUrl().isBlank()
                && order.getPaymentRequestExpiresAt() != null
                && now.isBefore(order.getPaymentRequestExpiresAt());
        // Chỉ duy trì một phiên VNPay đang hoạt động cho mỗi đơn, kể cả khi trình duyệt gửi lại key khác.
        if (activeRequest) {
            return order.getPaymentRequestUrl();
        }

        String transactionReference = createTransactionReference();
        order.setPaymentSessionId(transactionReference);
        order.setPaymentStatus(PaymentStatusPolicy.PENDING);
        order.setVnpayTransactionId(null);
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", tmnCode);
        params.put("vnp_Amount", String.valueOf(Math.multiplyExact((long) order.getTotal(), 100L)));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_IpAddr", extractClientIp(request));
        params.put("vnp_TxnRef", transactionReference);
        params.put("vnp_OrderInfo", "Thanh toan don hang " + order.getId());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", returnUrl);
        params.put("vnp_CreateDate", VNPAY_DATE_FORMAT.format(now));
        params.put("vnp_ExpireDate", VNPAY_DATE_FORMAT.format(now.plusSeconds(expireMinutes * 60L)));

        String canonicalData = canonicalData(params);
        String secureHash = hmacSHA512(hashSecret, canonicalData);
        String paymentUrl = payUrl + "?" + canonicalData + "&vnp_SecureHash=" + secureHash;
        order.setPaymentRequestKey(idempotencyKey);
        order.setPaymentRequestUrl(paymentUrl);
        order.setPaymentRequestExpiresAt(now.plusSeconds(expireMinutes * 60L));
        orderRepository.saveAndFlush(order);
        return paymentUrl;
    }

    @Transactional
    public VNPayPaymentResult handleReturn(Map<String, String> params) {
        return inspectReturn(params);
    }

    @Transactional
    public VNPayIpnResponse handleIpn(Map<String, String> params) {
        CallbackOutcome outcome = processIpn(params);
        return new VNPayIpnResponse(outcome.ipnCode(), outcome.ipnMessage());
    }

    @Transactional
    public VNPayPaymentResult reconcile(String orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn hàng: " + orderId));
        if (PaymentStatusPolicy.isVNPay(order)
                && PaymentStatusPolicy.PENDING.equals(PaymentStatusPolicy.normalize(order.getPaymentStatus()))
                && order.getPaymentRequestExpiresAt() != null
                && !Instant.now().isBefore(order.getPaymentRequestExpiresAt())) {
            order.setPaymentStatus(PaymentStatusPolicy.EXPIRED);
            order.setPaymentRequestUrl(null);
            order.setPaymentRequestKey(null);
            orderRepository.save(order);
        }
        boolean paid = PaymentStatusPolicy.isPaid(order);
        return new VNPayPaymentResult(
                true,
                paid,
                order.getId(),
                PaymentStatusPolicy.normalize(order.getPaymentStatus()),
                order.getVnpayTransactionId(),
                (long) order.getTotal(),
                null,
                paid
                        ? "Thanh toán đã được IPN của VNPay xác nhận."
                        : paymentStatusMessage(order)
        );
    }

    @Transactional(readOnly = true)
    public Order requireOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn hàng: " + orderId));
    }

    public boolean verifySignature(Map<String, String> params) {
        if (!isAvailable()) {
            return false;
        }
        String providedHash = valueOrEmpty(params.get("vnp_SecureHash"));
        if (providedHash.isBlank()) {
            return false;
        }
        String expectedHash = hmacSHA512(hashSecret, canonicalCallbackData(params));
        return MessageDigest.isEqual(
                expectedHash.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII),
                providedHash.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII)
        );
    }

    private VNPayPaymentResult inspectReturn(Map<String, String> params) {
        CallbackValidation validation = validateCallback(params, false);
        if (validation.failure() != null) {
            return validation.failure().result();
        }
        Order order = validation.order();
        boolean paid = PaymentStatusPolicy.isPaid(order);
        boolean gatewayReportedSuccess = isGatewaySuccess(params);
        String message;
        if (paid) {
            message = "Thanh toán đã được IPN của VNPay xác nhận.";
        } else if (gatewayReportedSuccess) {
            message = "VNPay đã trả kết quả thành công. FoodFast đang chờ IPN xác nhận chính thức.";
        } else {
            message = "Giao dịch không thành công hoặc đã bị hủy. Bạn có thể tạo phiên thanh toán mới.";
        }
        return new VNPayPaymentResult(
                true,
                paid,
                order.getId(),
                PaymentStatusPolicy.normalize(order.getPaymentStatus()),
                order.getVnpayTransactionId(),
                (long) order.getTotal(),
                params.get("vnp_ResponseCode"),
                message
        );
    }

    private CallbackOutcome processIpn(Map<String, String> params) {
        CallbackValidation validation = validateCallback(params, true);
        if (validation.failure() != null) {
            return validation.failure();
        }
        Order order = validation.order();
        String transactionId = blankToNull(params.get("vnp_TransactionNo"));

        if (PaymentStatusPolicy.isPaid(order)) {
            return new CallbackOutcome(
                    successResult(order, transactionId, params.get("vnp_ResponseCode"),
                            "Đơn hàng đã được xác nhận thanh toán."),
                    "02",
                    "Đơn hàng đã được xác nhận"
            );
        }
        if (PaymentStatusPolicy.FAILED.equals(PaymentStatusPolicy.normalize(order.getPaymentStatus()))) {
            return failure(true, order.getId(), order.getPaymentStatus(), (long) order.getTotal(),
                    params.get("vnp_ResponseCode"), "Kết quả giao dịch đã được ghi nhận.",
                    "02", "Giao dịch đã được xử lý");
        }
        if (order.getStatusEnum() != OrderStatus.PENDING) {
            return failure(true, order.getId(), order.getPaymentStatus(), (long) order.getTotal(),
                    params.get("vnp_ResponseCode"), "Đơn hàng không còn chờ thanh toán.",
                    "02", "Đơn hàng đã được xử lý");
        }

        boolean successful = isGatewaySuccess(params);
        if (successful) {
            if (transactionId == null) {
                return failure(true, order.getId(), order.getPaymentStatus(), (long) order.getTotal(),
                        params.get("vnp_ResponseCode"), "Thiếu mã giao dịch VNPay.",
                        "99", "Thiếu mã giao dịch");
            }
            Order transactionOwner = orderRepository.findByVnpayTransactionId(transactionId).orElse(null);
            if (transactionOwner != null && !transactionOwner.getId().equals(order.getId())) {
                return failure(true, order.getId(), order.getPaymentStatus(), (long) order.getTotal(),
                        params.get("vnp_ResponseCode"), "Mã giao dịch đã được dùng cho đơn khác.",
                        "02", "Giao dịch đã được xử lý");
            }
            order.setPaymentStatus(PaymentStatusPolicy.PAID);
            if (order.getPaidAt() == null) {
                order.setPaidAt(System.currentTimeMillis());
            }
            order.setVnpayTransactionId(transactionId);
            order.setPaymentRequestUrl(null);
            Order saved = orderRepository.save(order);
            orderEventPublisher.publishOrderUpdate(saved);
            customerNotificationService.paymentSucceeded(saved);
            try {
                notificationService.createNotification(saved);
            } catch (RuntimeException exception) {
                System.err.println("[VNPay] Không thể tạo thông báo nhà hàng: " + exception.getMessage());
            }
            return new CallbackOutcome(
                    successResult(saved, transactionId, params.get("vnp_ResponseCode"),
                            "Thanh toán VNPay thành công."),
                    "00",
                    "Xác nhận thành công"
            );
        }

        order.setPaymentStatus(PaymentStatusPolicy.FAILED);
        order.setVnpayTransactionId(transactionId);
        order.setPaymentRequestUrl(null);
        Order saved = orderRepository.save(order);
        customerNotificationService.paymentFailed(saved);
        return failure(true, order.getId(), PaymentStatusPolicy.FAILED, (long) order.getTotal(),
                params.get("vnp_ResponseCode"), "Giao dịch không thành công hoặc đã bị hủy.",
                "00", "Đã ghi nhận kết quả giao dịch");
    }

    private CallbackValidation validateCallback(Map<String, String> params, boolean forUpdate) {
        if (!isAvailable()) {
            return invalid(failure(false, null, null, null, "99", availabilityMessage(), "99", "VNPay chưa được cấu hình"));
        }
        if (!verifySignature(params)) {
            return invalid(failure(false, null, null, null, "97", "Chữ ký VNPay không hợp lệ.", "97", "Chữ ký không hợp lệ"));
        }
        if (!tmnCode.equals(params.get("vnp_TmnCode"))) {
            return invalid(failure(false, null, null, null, "97", "Mã website VNPay không hợp lệ.", "97", "Mã website không hợp lệ"));
        }

        String transactionReference = valueOrEmpty(params.get("vnp_TxnRef"));
        Order order = forUpdate
                ? orderRepository.findByPaymentSessionIdForUpdate(transactionReference).orElse(null)
                : orderRepository.findByPaymentSessionId(transactionReference).stream().findFirst().orElse(null);
        if (order == null) {
            return invalid(failure(true, null, null, null, params.get("vnp_ResponseCode"),
                    "Không tìm thấy đơn hàng tương ứng.", "01", "Không tìm thấy đơn hàng"));
        }

        long callbackAmount;
        try {
            callbackAmount = Long.parseLong(valueOrEmpty(params.get("vnp_Amount")));
        } catch (NumberFormatException exception) {
            return invalid(failure(true, order.getId(), order.getPaymentStatus(), null, params.get("vnp_ResponseCode"),
                    "Số tiền VNPay trả về không hợp lệ.", "04", "Số tiền không hợp lệ"));
        }

        long expectedAmount = Math.multiplyExact((long) order.getTotal(), 100L);
        if (callbackAmount != expectedAmount) {
            return invalid(failure(true, order.getId(), order.getPaymentStatus(), (long) order.getTotal(),
                    params.get("vnp_ResponseCode"), "Số tiền thanh toán không khớp với đơn hàng.",
                    "04", "Số tiền không hợp lệ"));
        }
        return new CallbackValidation(order, null);
    }

    private boolean isGatewaySuccess(Map<String, String> params) {
        return "00".equals(params.get("vnp_ResponseCode"))
                && "00".equals(params.get("vnp_TransactionStatus"));
    }

    private CallbackValidation invalid(CallbackOutcome failure) {
        return new CallbackValidation(null, failure);
    }

    private String paymentStatusMessage(Order order) {
        String status = PaymentStatusPolicy.normalize(order.getPaymentStatus());
        return switch (status) {
            case PaymentStatusPolicy.EXPIRED -> "Phiên thanh toán đã hết hạn. Vui lòng tạo phiên mới.";
            case PaymentStatusPolicy.FAILED -> "Thanh toán chưa thành công. Bạn có thể thử lại.";
            default -> "Đang chờ IPN của VNPay xác nhận thanh toán.";
        };
    }

    private VNPayPaymentResult successResult(Order order, String transactionId, String responseCode, String message) {
        return new VNPayPaymentResult(
                true,
                true,
                order.getId(),
                PaymentStatusPolicy.PAID,
                transactionId != null ? transactionId : order.getVnpayTransactionId(),
                (long) order.getTotal(),
                responseCode,
                message
        );
    }

    private CallbackOutcome failure(
            boolean verified,
            String orderId,
            String paymentStatus,
            Long amount,
            String responseCode,
            String message,
            String ipnCode,
            String ipnMessage
    ) {
        return new CallbackOutcome(
                new VNPayPaymentResult(
                        verified,
                        false,
                        orderId,
                        paymentStatus,
                        null,
                        amount,
                        responseCode,
                        message
                ),
                ipnCode,
                ipnMessage
        );
    }

    private String canonicalCallbackData(Map<String, String> params) {
        Map<String, String> filtered = new TreeMap<>();
        params.forEach((key, value) -> {
            if (key != null
                    && key.startsWith("vnp_")
                    && !"vnp_SecureHash".equals(key)
                    && !"vnp_SecureHashType".equals(key)
                    && value != null
                    && !value.isBlank()) {
                filtered.put(key, value);
            }
        });
        return canonicalData(filtered);
    }

    private String canonicalData(Map<String, String> params) {
        StringBuilder data = new StringBuilder();
        for (Map.Entry<String, String> entry : new TreeMap<>(params).entrySet()) {
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            if (data.length() > 0) {
                data.append('&');
            }
            data.append(urlEncode(entry.getKey()))
                    .append('=')
                    .append(urlEncode(entry.getValue()));
        }
        return data.toString();
    }

    private String createTransactionReference() {
        String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
        return "FF" + System.currentTimeMillis() + randomPart;
    }

    private String extractClientIp(HttpServletRequest request) {
        String ip = firstNonBlank(
                request.getHeader("CF-Connecting-IP"),
                request.getHeader("X-Forwarded-For"),
                request.getRemoteAddr()
        );
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip == null || ip.isBlank() ? "127.0.0.1" : ip;
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            hmac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            return bytesToHex(hmac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể tạo chữ ký VNPay", exception);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            result.append(String.format("%02x", value));
        }
        return result.toString();
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.US_ASCII);
    }

    private void requireAvailable() {
        if (!isAvailable()) {
            throw new IllegalStateException(availabilityMessage());
        }
    }

    private String normalizeIdempotencyKey(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            throw new IllegalArgumentException("Thiếu Idempotency-Key khi tạo phiên VNPay");
        }
        String key = rawKey.trim();
        if (key.length() < 8 || key.length() > 100 || !key.matches("[A-Za-z0-9._:-]+")) {
            throw new IllegalArgumentException("Idempotency-Key thanh toán không hợp lệ");
        }
        return key;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank() && !"unknown".equalsIgnoreCase(value)) {
                return value;
            }
        }
        return null;
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private record CallbackOutcome(VNPayPaymentResult result, String ipnCode, String ipnMessage) {
    }

    private record CallbackValidation(Order order, CallbackOutcome failure) { }
}
