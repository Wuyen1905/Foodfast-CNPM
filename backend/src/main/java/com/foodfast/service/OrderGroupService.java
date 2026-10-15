package com.foodfast.service;

import com.foodfast.dto.CreateOrderGroupRequest;
import com.foodfast.dto.CreateOrderRequest;
import com.foodfast.dto.OrderGroupQuoteResponse;
import com.foodfast.dto.OrderGroupResponse;
import com.foodfast.dto.OrderQuoteResponse;
import com.foodfast.entity.Order;
import com.foodfast.entity.OrderGroup;
import com.foodfast.entity.Product;
import com.foodfast.repository.OrderGroupRepository;
import com.foodfast.repository.OrderRepository;
import com.foodfast.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OrderGroupService {

    private static final ConcurrentHashMap<String, Object> LOCKS = new ConcurrentHashMap<>();

    private final OrderGroupRepository orderGroupRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderService orderService;
    private final TransactionTemplate transactionTemplate;

    public OrderGroupService(
            OrderGroupRepository orderGroupRepository,
            OrderRepository orderRepository,
            ProductRepository productRepository,
            OrderService orderService,
            PlatformTransactionManager transactionManager
    ) {
        this.orderGroupRepository = orderGroupRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.orderService = orderService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Transactional(readOnly = true)
    public OrderGroupQuoteResponse quote(CreateOrderGroupRequest request) {
        List<ChildRequest> children = split(request);
        List<OrderQuoteResponse> quotes = children.stream()
                .map(child -> orderService.quote(child.request()))
                .toList();
        return summarize(quotes);
    }

    public CreationResult create(CreateOrderGroupRequest request, String rawKey) {
        String key = normalizeKey(rawKey);
        String fingerprint = fingerprint(request);
        Object lock = LOCKS.computeIfAbsent(key, ignored -> new Object());
        try {
            synchronized (lock) {
                CreationResult result = transactionTemplate.execute(status -> {
                    OrderGroup existing = orderGroupRepository.findByIdempotencyKey(key).orElse(null);
                    if (existing != null) {
                        requireSameRequest(existing, fingerprint);
                        return new CreationResult(response(existing), true);
                    }
                    return new CreationResult(createTransactional(request, key, fingerprint), false);
                });
                if (result == null) {
                    throw new IllegalStateException("Không thể tạo nhóm đặt hàng");
                }
                return result;
            }
        } finally {
            LOCKS.remove(key, lock);
        }
    }

    protected OrderGroupResponse createTransactional(
            CreateOrderGroupRequest request,
            String idempotencyKey,
            String fingerprint
    ) {
        List<ChildRequest> children = split(request);

        // Báo giá toàn bộ trước khi ghi dữ liệu để không tạo nhóm dở dang nếu
        // một nhà hàng đóng cửa, hết món hoặc mã ưu đãi không còn hợp lệ.
        List<OrderQuoteResponse> previews = children.stream()
                .map(child -> orderService.quote(child.request()))
                .toList();
        OrderGroupQuoteResponse preview = summarize(previews);

        OrderGroup group = new OrderGroup();
        group.setId(generateGroupId());
        group.setUserId(requireText(request.userId, "Không xác định được khách hàng"));
        group.setCustomerName(requireText(request.customerName, "Không xác định được tên khách hàng"));
        group.setCustomerPhone(requireText(request.customerPhone, "Vui lòng nhập số điện thoại"));
        group.setCustomerEmail(request.customerEmail);
        group.setAddress(requireText(request.address, "Vui lòng nhập địa chỉ giao hàng"));
        group.setNote(request.note);
        group.setPaymentMethod(normalizePaymentMethod(request.paymentMethod));
        group.setPaymentStatus(PaymentStatusPolicy.PENDING);
        group.setSubtotal(preview.subtotal());
        group.setShippingFee(preview.shippingFee());
        group.setTaxAmount(preview.taxAmount());
        group.setDiscountAmount(preview.discountAmount());
        group.setTotal(preview.total());
        group.setPayableAmount(preview.total());
        group.setOrderCount(children.size());
        group.setIdempotencyKey(idempotencyKey);
        group.setRequestHash(fingerprint);
        group.setCreatedAt(Instant.now());
        group.setUpdatedAt(Instant.now());
        orderGroupRepository.save(group);

        List<Order> orders = new ArrayList<>();
        for (int index = 0; index < children.size(); index++) {
            ChildRequest child = children.get(index);
            Order order = orderService.createOrderFromRequest(
                    child.request(),
                    idempotencyKey + ":" + (index + 1),
                    sha256(fingerprint + "|" + child.restaurantId()),
                    group.getId(),
                    index + 1,
                    false
            );
            orders.add(order);
        }

        // Lấy số tiền đã lưu ở từng đơn con làm nguồn sự thật cho tổng nhóm.
        group.setSubtotal(sum(orders, Order::getSubtotal));
        group.setShippingFee(sum(orders, Order::getShippingFee));
        group.setTaxAmount(sum(orders, Order::getTaxAmount));
        group.setDiscountAmount(sum(orders, Order::getDiscountAmount));
        group.setTotal(sum(orders, Order::getTotal));
        group.setPayableAmount(group.getTotal());
        orderGroupRepository.save(group);
        orders.forEach(orderService::announceCreatedOrder);
        return OrderGroupResponse.from(group, orders);
    }

    @Transactional(readOnly = true)
    public OrderGroupResponse findById(String groupId) {
        OrderGroup group = orderGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhóm đặt hàng"));
        return response(group);
    }

    @Transactional(readOnly = true)
    public List<OrderGroupResponse> findByUser(String userId) {
        return orderGroupRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::response)
                .toList();
    }

    private OrderGroupResponse response(OrderGroup group) {
        return OrderGroupResponse.from(
                group,
                orderRepository.findByOrderGroupIdOrderByGroupPositionAsc(group.getId())
        );
    }

    private List<ChildRequest> split(CreateOrderGroupRequest request) {
        if (request == null || request.items == null || request.items.isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng phải có ít nhất một món");
        }
        Map<String, List<CreateOrderRequest.Item>> byRestaurant = new LinkedHashMap<>();
        for (CreateOrderRequest.Item item : request.items) {
            if (item == null || !StringUtils.hasText(item.productId)) {
                throw new IllegalArgumentException("Mỗi món phải có mã sản phẩm hợp lệ");
            }
            Product product = productRepository.findById(item.productId.trim())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy món ăn: " + item.productId));
            String restaurantId = RestaurantIdentityPolicy.canonicalize(product.getRestaurant());
            if (!StringUtils.hasText(restaurantId)) {
                throw new IllegalArgumentException("Món " + product.getName() + " chưa thuộc nhà hàng hợp lệ");
            }
            byRestaurant.computeIfAbsent(restaurantId, ignored -> new ArrayList<>()).add(item);
        }
        List<ChildRequest> result = new ArrayList<>();
        byRestaurant.forEach((restaurantId, items) -> {
            CreateOrderRequest child = new CreateOrderRequest();
            child.customerName = request.customerName;
            child.customerPhone = request.customerPhone;
            child.customerEmail = request.customerEmail;
            child.address = request.address;
            child.note = request.note;
            child.userId = request.userId;
            child.paymentMethod = normalizePaymentMethod(request.paymentMethod);
            child.restaurantId = restaurantId;
            child.voucherCode = voucherFor(request.voucherCodesByRestaurant, restaurantId);
            child.items = List.copyOf(items);
            result.add(new ChildRequest(restaurantId, child));
        });
        return List.copyOf(result);
    }

    private String voucherFor(Map<String, String> vouchers, String restaurantId) {
        if (vouchers == null || vouchers.isEmpty()) return null;
        return vouchers.entrySet().stream()
                .filter(entry -> RestaurantIdentityPolicy.refersToSameRestaurant(entry.getKey(), restaurantId))
                .map(Map.Entry::getValue)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .map(value -> value.toUpperCase(Locale.ROOT))
                .findFirst()
                .orElse(null);
    }

    private OrderGroupQuoteResponse summarize(List<OrderQuoteResponse> quotes) {
        return new OrderGroupQuoteResponse(
                List.copyOf(quotes),
                quotes.stream().mapToInt(OrderQuoteResponse::subtotal).sum(),
                quotes.stream().mapToInt(OrderQuoteResponse::shippingFee).sum(),
                quotes.stream().mapToInt(OrderQuoteResponse::taxAmount).sum(),
                quotes.stream().mapToInt(OrderQuoteResponse::discountAmount).sum(),
                quotes.stream().mapToInt(OrderQuoteResponse::total).sum()
        );
    }

    private int sum(List<Order> orders, java.util.function.ToIntFunction<Order> value) {
        return orders.stream().mapToInt(value).sum();
    }

    private String normalizePaymentMethod(String paymentMethod) {
        return StringUtils.hasText(paymentMethod)
                ? paymentMethod.trim().toLowerCase(Locale.ROOT)
                : "cod";
    }

    private String normalizeKey(String rawKey) {
        if (!StringUtils.hasText(rawKey)) {
            throw new IllegalArgumentException("Thiếu Idempotency-Key cho lần đặt hàng");
        }
        String key = rawKey.trim();
        if (key.length() < 8 || key.length() > 80 || !key.matches("[A-Za-z0-9._:-]+")) {
            throw new IllegalArgumentException("Idempotency-Key phải dài 8–80 ký tự và không chứa khoảng trắng");
        }
        return key;
    }

    private void requireSameRequest(OrderGroup existing, String fingerprint) {
        if (!fingerprint.equals(existing.getRequestHash())) {
            throw new IdempotencyKeyReuseException(
                    "Idempotency-Key đã được dùng cho một lần đặt hàng khác"
            );
        }
    }

    private String fingerprint(CreateOrderGroupRequest request) {
        StringBuilder value = new StringBuilder();
        append(value, request.userId);
        append(value, request.customerName);
        append(value, request.customerPhone);
        append(value, request.customerEmail);
        append(value, request.address);
        append(value, request.note);
        append(value, normalizePaymentMethod(request.paymentMethod));
        if (request.items != null) {
            for (CreateOrderRequest.Item item : request.items) {
                if (item == null) {
                    append(value, "null-item");
                    continue;
                }
                append(value, item.productId);
                append(value, String.valueOf(item.getQuantity()));
                List<String> options = new ArrayList<>(
                        item.selectedOptionIds == null ? List.of() : item.selectedOptionIds
                );
                options.sort(Comparator.naturalOrder());
                append(value, String.join(",", options));
                append(value, item.itemNote);
            }
        }
        if (request.voucherCodesByRestaurant != null) {
            request.voucherCodesByRestaurant.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        append(value, RestaurantIdentityPolicy.canonicalize(entry.getKey()));
                        append(value, entry.getValue());
                    });
        }
        return sha256(value.toString());
    }

    private void append(StringBuilder target, String raw) {
        String value = raw == null ? "" : raw.trim();
        target.append(value.length()).append(':').append(value).append('|');
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

    private String generateGroupId() {
        return "GROUP-" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 12).toUpperCase(Locale.ROOT);
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) throw new IllegalArgumentException(message);
        return value.trim();
    }

    private record ChildRequest(String restaurantId, CreateOrderRequest request) { }
    public record CreationResult(OrderGroupResponse group, boolean replayed) { }
}
