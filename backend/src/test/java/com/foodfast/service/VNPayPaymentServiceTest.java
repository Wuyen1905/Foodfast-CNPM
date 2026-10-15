package com.foodfast.service;

import com.foodfast.dto.VNPayPaymentResult;
import com.foodfast.dto.VNPayIpnResponse;
import com.foodfast.entity.Order;
import com.foodfast.entity.OrderGroup;
import com.foodfast.entity.OrderStatus;
import com.foodfast.event.OrderEventPublisher;
import com.foodfast.repository.OrderRepository;
import com.foodfast.repository.OrderGroupRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VNPayPaymentServiceTest {

    private static final String TMN_CODE = "TESTCODE";
    private static final String HASH_SECRET = "test-secret-for-hmac-sha512";

    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final OrderGroupRepository orderGroupRepository = mock(OrderGroupRepository.class);
    private final OrderEventPublisher orderEventPublisher = mock(OrderEventPublisher.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final CustomerNotificationService customerNotificationService = mock(CustomerNotificationService.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);

    private VNPayPaymentService service;

    @BeforeEach
    void setUp() {
        service = new VNPayPaymentService(
                orderRepository,
                orderGroupRepository,
                orderEventPublisher,
                notificationService,
                customerNotificationService,
                true,
                TMN_CODE,
                HASH_SECRET,
                "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                "https://foodfast.example/vnpay-return",
                15
        );
    }

    @Test
    void createPaymentUsesPersistedOrderTotalAndStoresAttemptReference() throws Exception {
        Order order = pendingVNPayOrder();
        when(orderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        String paymentUrl = service.createPaymentUrl(order.getId(), request);
        Map<String, String> params = parseQuery(paymentUrl);

        assertThat(params.get("vnp_Amount")).isEqualTo("10000000");
        assertThat(params.get("vnp_TxnRef")).isEqualTo(order.getPaymentSessionId());
        assertThat(params.get("vnp_ReturnUrl")).isEqualTo("https://foodfast.example/vnpay-return");
        assertThat(params.get("vnp_ExpireDate")).isNotBlank();
        assertThat(params.get("vnp_SecureHash")).hasSize(128);
        verify(orderRepository).saveAndFlush(order);
    }

    @Test
    void retryPaymentReplacesExpiredAttemptWithANewReference() throws Exception {
        Order order = pendingVNPayOrder();
        order.setPaymentSessionId("EXPIRED-ATTEMPT");
        order.setPaymentRequestKey("pay-attempt-123");
        order.setPaymentRequestUrl("https://sandbox.example/expired");
        order.setPaymentRequestExpiresAt(Instant.now().minusSeconds(1));
        when(orderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        String paymentUrl = service.createPaymentUrl(order.getId(), "pay-attempt-123", request);
        String newReference = parseQuery(paymentUrl).get("vnp_TxnRef");

        assertThat(newReference).isNotBlank();
        assertThat(newReference).isNotEqualTo("EXPIRED-ATTEMPT");
        assertThat(order.getPaymentSessionId()).isEqualTo(newReference);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatusPolicy.PENDING);
    }

    @Test
    void reconcileMarksAnExpiredRequestAndAllowsTheCustomerToRetry() {
        Order order = pendingVNPayOrder();
        order.setPaymentRequestKey("pay-attempt-123");
        order.setPaymentRequestUrl("https://sandbox.example/expired");
        order.setPaymentRequestExpiresAt(Instant.now().minusSeconds(1));
        when(orderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));

        VNPayPaymentResult result = service.reconcile(order.getId());

        assertThat(result.success()).isFalse();
        assertThat(result.paymentStatus()).isEqualTo(PaymentStatusPolicy.EXPIRED);
        assertThat(order.getPaymentRequestUrl()).isNull();
        assertThat(order.getPaymentRequestKey()).isNull();
        verify(orderRepository).save(order);
    }

    @Test
    void validSuccessfulIpnMarksOrderPaidAndAnnouncesItOnce() {
        Order order = pendingVNPayOrder();
        order.setPaymentSessionId("FFATTEMPT123");
        when(orderRepository.findByPaymentSessionIdForUpdate("FFATTEMPT123"))
                .thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        Map<String, String> callback = signedCallback("10000000", "00", "00");
        VNPayIpnResponse result = service.handleIpn(callback);

        assertThat(result.RspCode()).isEqualTo("00");
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatusPolicy.PAID);
        assertThat(order.getPaidAt()).isNotNull();
        assertThat(order.getVnpayTransactionId()).isEqualTo("999999");
        verify(orderEventPublisher).publishOrderUpdate(order);
        verify(notificationService).createNotification(order);
    }

    @Test
    void validGroupIpnPaysOnceAndAllocatesExactAmountToEveryActiveChild() {
        OrderGroup group = new OrderGroup();
        group.setId("GROUP-TEST-1");
        group.setUserId("CUSTOMER-1");
        group.setPaymentMethod("vnpay");
        group.setPaymentStatus(PaymentStatusPolicy.PENDING);
        group.setTotal(180_000);
        group.setPayableAmount(180_000);
        group.setPaymentSessionId("FGATTEMPT123");
        Order first = pendingVNPayOrder();
        first.setId("ORDER-CHILD-1");
        first.setTotal(100_000);
        Order second = pendingVNPayOrder();
        second.setId("ORDER-CHILD-2");
        second.setTotal(80_000);
        when(orderGroupRepository.findByPaymentSessionId("FGATTEMPT123"))
                .thenReturn(Optional.of(group));
        when(orderGroupRepository.findByPaymentSessionIdForUpdate("FGATTEMPT123"))
                .thenReturn(Optional.of(group));
        when(orderRepository.findByOrderGroupIdOrderByGroupPositionAsc(group.getId()))
                .thenReturn(List.of(first, second));
        when(orderRepository.save(first)).thenReturn(first);
        when(orderRepository.save(second)).thenReturn(second);

        VNPayIpnResponse result = service.handleIpn(
                signedCallback("18000000", "00", "00", "FGATTEMPT123")
        );

        assertThat(result.RspCode()).isEqualTo("00");
        assertThat(group.getPaymentStatus()).isEqualTo(PaymentStatusPolicy.PAID);
        assertThat(group.getVnpayTransactionId()).isEqualTo("999999");
        assertThat(first.getPaymentAllocatedAmount()).isEqualTo(100_000);
        assertThat(second.getPaymentAllocatedAmount()).isEqualTo(80_000);
        assertThat(first.getPaymentStatus()).isEqualTo(PaymentStatusPolicy.PAID);
        assertThat(second.getPaymentStatus()).isEqualTo(PaymentStatusPolicy.PAID);
    }

    @Test
    void duplicateSuccessfulIpnDoesNotProcessThePaidOrderAgain() {
        Order order = pendingVNPayOrder();
        order.setPaymentSessionId("FFATTEMPT123");
        order.setPaymentStatus(PaymentStatusPolicy.PAID);
        order.setVnpayTransactionId("999999");
        when(orderRepository.findByPaymentSessionIdForUpdate("FFATTEMPT123"))
                .thenReturn(Optional.of(order));

        VNPayIpnResponse result = service.handleIpn(signedCallback("10000000", "00", "00"));

        assertThat(result.RspCode()).isEqualTo("02");
        verify(orderRepository, never()).save(order);
        verify(orderEventPublisher, never()).publishOrderUpdate(order);
        verify(notificationService, never()).createNotification(order);
    }

    @Test
    void successfulIpnRejectsATransactionNumberAlreadyUsedByAnotherOrder() {
        Order order = pendingVNPayOrder();
        order.setPaymentSessionId("FFATTEMPT123");
        Order otherOrder = pendingVNPayOrder();
        otherOrder.setId("ORDER-OTHER-456");
        otherOrder.setVnpayTransactionId("999999");
        when(orderRepository.findByPaymentSessionIdForUpdate("FFATTEMPT123"))
                .thenReturn(Optional.of(order));
        when(orderRepository.findByVnpayTransactionId("999999"))
                .thenReturn(Optional.of(otherOrder));

        VNPayIpnResponse result = service.handleIpn(signedCallback("10000000", "00", "00"));

        assertThat(result.RspCode()).isEqualTo("02");
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatusPolicy.PENDING);
        verify(orderRepository, never()).save(order);
        verify(orderEventPublisher, never()).publishOrderUpdate(order);
    }

    @Test
    void ipnWithWrongAmountIsRejectedWithoutChangingTheOrder() {
        Order order = pendingVNPayOrder();
        order.setPaymentSessionId("FFATTEMPT123");
        when(orderRepository.findByPaymentSessionIdForUpdate("FFATTEMPT123"))
                .thenReturn(Optional.of(order));

        VNPayIpnResponse result = service.handleIpn(signedCallback("9900000", "00", "00"));

        assertThat(result.RspCode()).isEqualTo("04");
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatusPolicy.PENDING);
        verify(orderRepository, never()).save(order);
        verify(orderEventPublisher, never()).publishOrderUpdate(order);
    }

    @Test
    void successfulReturnWaitsForIpnAndNeverMutatesPaymentState() {
        Order order = pendingVNPayOrder();
        order.setPaymentSessionId("FFATTEMPT123");
        when(orderRepository.findByPaymentSessionId("FFATTEMPT123")).thenReturn(List.of(order));

        VNPayPaymentResult result = service.handleReturn(signedCallback("10000000", "00", "00"));

        assertThat(result.verified()).isTrue();
        assertThat(result.success()).isFalse();
        assertThat(result.paymentStatus()).isEqualTo(PaymentStatusPolicy.PENDING);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatusPolicy.PENDING);
        verify(orderRepository, never()).save(order);
        verify(orderEventPublisher, never()).publishOrderUpdate(order);
    }

    @Test
    void samePaymentIdempotencyKeyReturnsExactlyTheSameGatewayUrl() throws Exception {
        Order order = pendingVNPayOrder();
        when(orderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        String first = service.createPaymentUrl(order.getId(), "pay-attempt-123", request);
        String second = service.createPaymentUrl(order.getId(), "pay-attempt-123", request);

        assertThat(second).isEqualTo(first);
        assertThat(parseQuery(second).get("vnp_TxnRef")).isEqualTo(order.getPaymentSessionId());
        verify(orderRepository).saveAndFlush(order);
    }

    @Test
    void amountMismatchNeverMarksOrderPaid() {
        Order order = pendingVNPayOrder();
        order.setPaymentSessionId("FFATTEMPT123");
        when(orderRepository.findByPaymentSessionId("FFATTEMPT123"))
                .thenReturn(List.of(order));

        VNPayPaymentResult result = service.handleReturn(signedCallback("9900000", "00", "00"));

        assertThat(result.verified()).isTrue();
        assertThat(result.success()).isFalse();
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatusPolicy.PENDING);
        verify(orderRepository, never()).save(order);
        verify(orderEventPublisher, never()).publishOrderUpdate(order);
    }

    @Test
    void invalidSignatureIsRejectedBeforeLookingUpOrder() {
        Map<String, String> callback = signedCallback("10000000", "00", "00");
        callback.put("vnp_SecureHash", "tampered");

        VNPayPaymentResult result = service.handleReturn(callback);

        assertThat(result.verified()).isFalse();
        assertThat(result.success()).isFalse();
        verify(orderRepository, never()).findByPaymentSessionIdForUpdate("FFATTEMPT123");
    }

    private Order pendingVNPayOrder() {
        Order order = new Order();
        order.setId("ORDER-TEST-123");
        order.setPaymentMethod("vnpay");
        order.setPaymentStatus(PaymentStatusPolicy.PENDING);
        order.setStatus(OrderStatus.PENDING);
        order.setTotal(100_000);
        order.setRestaurantId("restaurant_2");
        return order;
    }

    private Map<String, String> signedCallback(String amount, String responseCode, String transactionStatus) {
        return signedCallback(amount, responseCode, transactionStatus, "FFATTEMPT123");
    }

    private Map<String, String> signedCallback(
            String amount,
            String responseCode,
            String transactionStatus,
            String reference
    ) {
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Amount", amount);
        params.put("vnp_ResponseCode", responseCode);
        params.put("vnp_TmnCode", TMN_CODE);
        params.put("vnp_TransactionNo", "999999");
        params.put("vnp_TransactionStatus", transactionStatus);
        params.put("vnp_TxnRef", reference);
        params.put("vnp_SecureHash", hmac(canonical(params)));
        return params;
    }

    private String canonical(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(entry -> !"vnp_SecureHash".equals(entry.getKey()))
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String hmac(String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            hmac.init(new SecretKeySpec(HASH_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            StringBuilder result = new StringBuilder();
            for (byte value : hmac.doFinal(data.getBytes(StandardCharsets.UTF_8))) {
                result.append(String.format("%02x", value));
            }
            return result.toString();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private Map<String, String> parseQuery(String url) throws Exception {
        return Arrays.stream(new URI(url).getRawQuery().split("&"))
                .map(pair -> pair.split("=", 2))
                .collect(Collectors.toMap(
                        pair -> URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                        pair -> URLDecoder.decode(pair[1], StandardCharsets.UTF_8)
                ));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.US_ASCII);
    }
}
