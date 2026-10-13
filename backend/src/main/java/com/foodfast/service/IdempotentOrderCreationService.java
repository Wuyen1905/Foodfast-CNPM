package com.foodfast.service;

import com.foodfast.dto.CreateOrderRequest;
import com.foodfast.entity.Order;
import com.foodfast.repository.OrderRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IdempotentOrderCreationService {

    private static final ConcurrentHashMap<String, Object> LOCKS = new ConcurrentHashMap<>();

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    public IdempotentOrderCreationService(OrderRepository orderRepository, OrderService orderService) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    public CreationResult create(CreateOrderRequest request, String rawKey) {
        String key = normalizeKey(rawKey);
        String fingerprint = fingerprint(request);
        Object lock = LOCKS.computeIfAbsent(key, ignored -> new Object());
        try {
            synchronized (lock) {
                Order existing = orderRepository.findByIdempotencyKey(key).orElse(null);
                if (existing != null) {
                    requireSameRequest(existing, fingerprint);
                    return new CreationResult(existing, true);
                }
                try {
                    Order created = orderService.createOrderFromRequest(request, key, fingerprint);
                    return new CreationResult(created, false);
                } catch (DataIntegrityViolationException conflict) {
                    Order concurrent = orderRepository.findByIdempotencyKey(key).orElseThrow(() -> conflict);
                    requireSameRequest(concurrent, fingerprint);
                    return new CreationResult(concurrent, true);
                }
            }
        } finally {
            LOCKS.remove(key, lock);
        }
    }

    private String normalizeKey(String rawKey) {
        if (!StringUtils.hasText(rawKey)) {
            throw new IllegalArgumentException("Thiếu Idempotency-Key cho yêu cầu tạo đơn");
        }
        String key = rawKey.trim();
        if (key.length() < 8 || key.length() > 100 || !key.matches("[A-Za-z0-9._:-]+")) {
            throw new IllegalArgumentException("Idempotency-Key phải dài 8–100 ký tự và không chứa khoảng trắng");
        }
        return key;
    }

    private void requireSameRequest(Order existing, String fingerprint) {
        if (!fingerprint.equals(existing.getIdempotencyRequestHash())) {
            throw new IdempotencyKeyReuseException(
                    "Idempotency-Key đã được dùng cho một nội dung đặt hàng khác"
            );
        }
    }

    private String fingerprint(CreateOrderRequest request) {
        if (request == null) throw new IllegalArgumentException("Yêu cầu tạo đơn không hợp lệ");
        StringBuilder canonical = new StringBuilder();
        append(canonical, request.userId);
        append(canonical, request.customerName);
        append(canonical, request.customerPhone);
        append(canonical, request.customerEmail);
        append(canonical, request.address);
        append(canonical, request.note);
        append(canonical, request.restaurantId);
        append(canonical, request.paymentMethod == null ? "cod" : request.paymentMethod.toLowerCase(Locale.ROOT));
        append(canonical, request.voucherCode == null ? "" : request.voucherCode.toUpperCase(Locale.ROOT));
        if (request.items != null) {
            for (CreateOrderRequest.Item item : request.items) {
                if (item == null) {
                    append(canonical, "null-item");
                    continue;
                }
                append(canonical, item.productId);
                append(canonical, String.valueOf(item.getQuantity()));
                List<String> optionIds = new ArrayList<>(
                        item.selectedOptionIds == null ? List.of() : item.selectedOptionIds
                );
                Collections.sort(optionIds);
                append(canonical, String.join(",", optionIds));
                append(canonical, item.itemNote);
            }
        }
        return sha256(canonical.toString());
    }

    private void append(StringBuilder target, String value) {
        String normalized = value == null ? "" : value.trim();
        target.append(normalized.length()).append(':').append(normalized).append('|');
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) result.append(String.format("%02x", item));
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Không thể tạo dấu vân tay yêu cầu", exception);
        }
    }

    public record CreationResult(Order order, boolean replayed) {
    }
}
