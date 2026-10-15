package com.foodfast.service;

import com.foodfast.dto.CreateOrderRequest;
import com.foodfast.dto.OrderQuoteResponse;
import com.foodfast.entity.Order;
import com.foodfast.entity.OrderItem;
import com.foodfast.entity.OrderItemOption;
import com.foodfast.entity.OrderStatus;
import com.foodfast.entity.Product;
import com.foodfast.entity.VoucherScope;
import com.foodfast.event.OrderEventPublisher;
import com.foodfast.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductAvailabilityService productAvailabilityService;
    private final OrderEventPublisher orderEventPublisher;
    private final NotificationService notificationService;
    private final RestaurantService restaurantService;
    private final ProductOptionPricingService optionPricingService;
    private final VoucherService voucherService;
    private final OrderStatusHistoryService orderStatusHistoryService;
    private final CustomerNotificationService customerNotificationService;
    private final DeliveryTaskService deliveryTaskService;
    private final OrderGroupPaymentService orderGroupPaymentService;

    public OrderService(
            OrderRepository orderRepository,
            ProductAvailabilityService productAvailabilityService,
            OrderEventPublisher orderEventPublisher,
            NotificationService notificationService,
            RestaurantService restaurantService,
            ProductOptionPricingService optionPricingService,
            VoucherService voucherService,
            OrderStatusHistoryService orderStatusHistoryService,
            CustomerNotificationService customerNotificationService,
            DeliveryTaskService deliveryTaskService,
            OrderGroupPaymentService orderGroupPaymentService) {
        this.orderRepository = orderRepository;
        this.productAvailabilityService = productAvailabilityService;
        this.orderEventPublisher = orderEventPublisher;
        this.notificationService = notificationService;
        this.restaurantService = restaurantService;
        this.optionPricingService = optionPricingService;
        this.voucherService = voucherService;
        this.orderStatusHistoryService = orderStatusHistoryService;
        this.customerNotificationService = customerNotificationService;
        this.deliveryTaskService = deliveryTaskService;
        this.orderGroupPaymentService = orderGroupPaymentService;
    }

    @Transactional(readOnly = true)
    public List<Order> findOrders(String paymentSessionId, String phone, String restaurant, String userId) {
        try {
            System.out.println("[OrderService] findOrders - paymentSessionId: "
                    + paymentSessionId + ", phone: " + phone + ", restaurant: " + restaurant
                    + ", userId: " + userId);

            List<Order> orders;

            // Check paymentSessionId first (highest priority)
            if (StringUtils.hasText(paymentSessionId)) {
                orders = orderRepository.findByPaymentSessionId(paymentSessionId);
            } else {
                // Fall back to phone/restaurant/userId filters
                boolean hasPhone = StringUtils.hasText(phone);
                boolean hasRestaurantId = StringUtils.hasText(restaurant);
                boolean hasUserId = StringUtils.hasText(userId);

                if (hasPhone && hasRestaurantId) {
                    orders = orderRepository.findByCustomerPhoneContainingIgnoreCaseAndRestaurantIdIn(
                            phone, RestaurantIdentityPolicy.equivalentIds(restaurant)
                    );
                } else if (hasPhone) {
                    orders = orderRepository.findByCustomerPhoneContainingIgnoreCase(phone);
                } else if (hasRestaurantId) {
                    orders = orderRepository.findByRestaurantIdIn(
                            RestaurantIdentityPolicy.equivalentIds(restaurant)
                    );
                } else if (hasUserId) {
                    orders = orderRepository.findByUserId(userId);
                } else {
                    orders = orderRepository.findAll();
                }
            }

            // Ensure items are initialized (safety check even with EAGER fetch)
            if (orders != null) {
                if (StringUtils.hasText(restaurant)) {
                    orders = orders.stream()
                            .filter(PaymentStatusPolicy::isReadyForRestaurant)
                            .toList();
                }
                for (Order order : orders) {
                    if (order != null) {
                        // Ensure items list is initialized
                        List<OrderItem> items = order.getItems();
                        if (items != null) {
                            // Access items to ensure they're loaded
                            items.size();
                        }
                    }
                }
            }

            System.out.println("[OrderService] Returning " + (orders != null ? orders.size() : 0) + " orders");
            return orders != null ? orders : new ArrayList<>();
        } catch (Exception e) {
            System.err.println("[OrderService] ERROR in findOrders: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public Optional<Order> findById(String id) {
        return orderRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public OrderQuoteResponse quote(CreateOrderRequest request) {
        ResolvedPricing pricing = resolvePricing(request, null, false);
        List<OrderQuoteResponse.QuotedItem> quotedItems = pricing.lines().stream()
                .map(line -> new OrderQuoteResponse.QuotedItem(
                        line.product().getId(),
                        line.product().getName(),
                        line.quantity(),
                        line.pricing().unitPrice(),
                        Math.multiplyExact(line.quantity(), line.pricing().unitPrice()),
                        line.pricing().selectedOptions().stream()
                                .map(option -> new OrderQuoteResponse.QuotedOption(
                                        option.optionId(),
                                        option.groupName(),
                                        option.optionName(),
                                        option.priceDelta()
                                ))
                                .toList(),
                        line.pricing().itemNote()
                ))
                .toList();
        return new OrderQuoteResponse(
                pricing.restaurantId(),
                pricing.restaurantName(),
                quotedItems,
                pricing.subtotal(),
                pricing.shippingFee(),
                pricing.taxAmount(),
                pricing.voucher().discountAmount(),
                pricing.total(),
                pricing.voucher().code(),
                pricing.voucher().name()
        );
    }

    @Transactional
    public Order createOrderFromRequest(CreateOrderRequest req) {
        return createOrderFromRequest(req, null, null);
    }

    @Transactional
    public Order createOrderFromRequest(
            CreateOrderRequest req,
            String idempotencyKey,
            String idempotencyRequestHash
    ) {
        return createOrderFromRequest(req, idempotencyKey, idempotencyRequestHash, null, null, true);
    }

    @Transactional
    public Order createOrderFromRequest(
            CreateOrderRequest req,
            String idempotencyKey,
            String idempotencyRequestHash,
            String orderGroupId,
            Integer groupPosition,
            boolean announceImmediately
    ) {
        Order order = new Order();
        // Public clients never choose the persistent order id.
        order.setId(generateOrderId());
        order.setIdempotencyKey(idempotencyKey);
        order.setIdempotencyRequestHash(idempotencyRequestHash);
        order.setOrderGroupId(orderGroupId);
        order.setGroupPosition(groupPosition);
        
        order.setCustomerName(req.customerName);
        order.setCustomerPhone(req.customerPhone);
        order.setCustomerEmail(req.customerEmail);
        order.setAddress(req.address);
        order.setNote(req.note);
        order.setUserId(req.userId);
        order.setPaymentSessionId(req.paymentSessionId);
        
        Instant now = Instant.now();
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        // Trạng thái khởi tạo là quy tắc của backend, không nhận từ trình duyệt.
        order.setStatus(OrderStatus.PENDING);
        
        order.setPaymentMethod(req.paymentMethod == null ? "cod" : req.paymentMethod.trim().toLowerCase(Locale.ROOT));
        order.setPaymentStatus(PaymentStatusPolicy.initialStatus(order.getPaymentMethod()));
        
        ResolvedPricing pricing = resolvePricing(req, order.getId(), true);
        List<OrderItem> items = new ArrayList<>();
        for (ResolvedLine line : pricing.lines()) {
            Product product = line.product();
            OrderItem item = new OrderItem();
            item.setProductId(product.getId());
            item.setName(product.getName());
            item.setProductName(product.getName());
            item.setProductImage(product.getImageUrl());
            item.setQty(line.quantity());
            item.setPrice(line.pricing().unitPrice());
            item.setItemNote(line.pricing().itemNote());
            for (ProductOptionPricingService.SelectedOption selected : line.pricing().selectedOptions()) {
                OrderItemOption snapshot = new OrderItemOption();
                snapshot.setOptionId(selected.optionId());
                snapshot.setGroupName(selected.groupName());
                snapshot.setOptionName(selected.optionName());
                snapshot.setPriceDelta(selected.priceDelta());
                item.addSelectedOption(snapshot);
            }
            item.setOrder(order);
            items.add(item);
        }

        order.setRestaurantId(pricing.restaurantId());
        order.setRestaurant(pricing.restaurantName());
        order.setItems(items);

        order.setVoucherCode(pricing.voucher().code());
        order.setVoucherName(pricing.voucher().name());
        order.setVoucherScope(pricing.voucher().scope());
        order.setDiscountAmount(pricing.voucher().discountAmount());
        order.setRestaurantDiscountAmount(pricing.voucher().scope() == VoucherScope.RESTAURANT
                ? pricing.voucher().discountAmount() : 0);

        // Always recalculate total from items
        order.recalculateTotal();

        Order saved = orderRepository.save(order);
        orderStatusHistoryService.record(
                saved.getId(), null, OrderStatus.PENDING, saved.getUserId(),
                "CUSTOMER", "CHECKOUT", "Khách hàng tạo đơn"
        );
        if (announceImmediately) announceCreatedOrder(saved);
        
        return saved;
    }

    public void announceCreatedOrder(Order order) {
        customerNotificationService.orderCreated(order);
        if (!PaymentStatusPolicy.isReadyForRestaurant(order)) return;
        orderEventPublisher.publishOrderUpdate(order);
        try {
            notificationService.createNotification(order);
        } catch (Exception e) {
            System.err.println("[OrderService] Failed to create notification: " + e.getMessage());
        }
    }

    private ResolvedPricing resolvePricing(
            CreateOrderRequest request,
            String orderId,
            boolean reserveVoucher
    ) {
        if (request == null || request.items == null || request.items.isEmpty()) {
            throw new IllegalArgumentException("Đơn hàng phải có ít nhất một món");
        }

        List<ResolvedLine> lines = new ArrayList<>();
        String restaurantId = null;
        String restaurantName = null;
        int subtotal = 0;

        Map<String, Integer> requestedQuantities = new LinkedHashMap<>();
        for (CreateOrderRequest.Item requestedItem : request.items) {
            if (requestedItem == null || !StringUtils.hasText(requestedItem.productId)) {
                throw new IllegalArgumentException("Mỗi món trong đơn phải có mã sản phẩm hợp lệ");
            }
            int quantity = requestedItem.getQuantity();
            if (requestedItem.quantity <= 0 && requestedItem.qty <= 0) {
                throw new IllegalArgumentException("Số lượng món phải lớn hơn 0");
            }
            requestedQuantities.merge(requestedItem.productId.trim(), quantity, Math::addExact);
        }

        Map<String, Product> catalog = new LinkedHashMap<>();
        requestedQuantities.forEach((productId, quantity) -> catalog.put(
                productId,
                productAvailabilityService.requireOrderable(productId, quantity, reserveVoucher)
        ));

        for (CreateOrderRequest.Item requestedItem : request.items) {
            if (requestedItem == null || !StringUtils.hasText(requestedItem.productId)) {
                throw new IllegalArgumentException("Mỗi món trong đơn phải có mã sản phẩm hợp lệ");
            }
            int quantity = requestedItem.getQuantity();
            if (requestedItem.quantity <= 0 && requestedItem.qty <= 0) {
                throw new IllegalArgumentException("Số lượng món phải lớn hơn 0");
            }

            Product product = catalog.get(requestedItem.productId.trim());

            ProductOptionPricingService.PricingResult itemPricing = optionPricingService.price(
                    product,
                    requestedItem.selectedOptionIds,
                    requestedItem.itemNote
            );
            String itemRestaurantId = RestaurantIdentityPolicy.canonicalize(product.getRestaurant());
            if (restaurantId == null) {
                restaurantId = itemRestaurantId;
                restaurantName = product.getRestaurant();
            } else if (!RestaurantIdentityPolicy.refersToSameRestaurant(restaurantId, itemRestaurantId)) {
                throw new IllegalArgumentException("Mỗi đơn hàng chỉ được chứa món của một nhà hàng");
            }

            subtotal = Math.addExact(
                    subtotal,
                    Math.multiplyExact(quantity, itemPricing.unitPrice())
            );
            lines.add(new ResolvedLine(product, quantity, itemPricing));
        }

        String requestedRestaurantId = RestaurantIdentityPolicy.canonicalize(request.restaurantId);
        if (StringUtils.hasText(requestedRestaurantId)
                && !RestaurantIdentityPolicy.refersToSameRestaurant(requestedRestaurantId, restaurantId)) {
            throw new IllegalArgumentException("Nhà hàng của đơn không khớp với nhà hàng của món ăn");
        }

        restaurantService.requireAvailableForOrdering(
                restaurantId,
                LocalTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))
        );

        VoucherService.VoucherApplication voucher;
        if (!StringUtils.hasText(request.voucherCode)) {
            voucher = VoucherService.VoucherApplication.none();
        } else if (reserveVoucher) {
            voucher = voucherService.applyAndReserve(
                    request.voucherCode,
                    restaurantId,
                    request.userId,
                    subtotal,
                    orderId,
                    Instant.now()
            );
        } else {
            voucher = voucherService.preview(
                    request.voucherCode,
                    restaurantId,
                    request.userId,
                    subtotal,
                    Instant.now()
            );
        }

        int shippingFee = 15_000;
        int taxAmount = 0;
        int discountAmount = Math.min(Math.max(0, voucher.discountAmount()), subtotal);
        int total = Math.max(0, Math.addExact(subtotal, shippingFee) - discountAmount);
        return new ResolvedPricing(
                List.copyOf(lines),
                restaurantId,
                restaurantName,
                subtotal,
                shippingFee,
                taxAmount,
                voucher,
                total
        );
    }

    private record ResolvedLine(
            Product product,
            int quantity,
            ProductOptionPricingService.PricingResult pricing
    ) {
    }

    private record ResolvedPricing(
            List<ResolvedLine> lines,
            String restaurantId,
            String restaurantName,
            int subtotal,
            int shippingFee,
            int taxAmount,
            VoucherService.VoucherApplication voucher,
            int total
    ) {
    }

    @Transactional
    public Order updateStatus(String id, OrderStatus status) {
        return updateStatus(id, status, null, "SYSTEM", "SYSTEM", null);
    }

    @Transactional
    public Order updateStatus(String id, OrderStatus status, String actorId) {
        return updateStatus(id, status, actorId, "RESTAURANT", "ORDER_MANAGEMENT", null);
    }

    @Transactional
    public Order updateStatus(
            String id,
            OrderStatus status,
            String actorId,
            String actorRole,
            String source,
            String reason
    ) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng: " + id));
        if (status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Hủy hoặc từ chối đơn phải dùng quy trình có lý do");
        }
        if (status != OrderStatus.CANCELLED && !PaymentStatusPolicy.isReadyForRestaurant(order)) {
            throw new IllegalStateException("Đơn VNPay phải thanh toán thành công trước khi nhà hàng xử lý");
        }
        OrderStatus previousStatus = order.getStatusEnum();
        OrderStatusPolicy.requireTransition(previousStatus, status);
        order.setStatus(status);
        long statusChangedAt = System.currentTimeMillis();
        if (status == OrderStatus.CONFIRMED) {
            order.setConfirmedAt(statusChangedAt);
            order.setConfirmedBy(StringUtils.hasText(actorId) ? actorId.trim() : null);
        }
        if (status == OrderStatus.PREPARING && order.getPreparingAt() == null) {
            order.setPreparingAt(statusChangedAt);
        }
        if (status == OrderStatus.READY && order.getReadyAt() == null) {
            order.setReadyAt(statusChangedAt);
        }
        if (status == OrderStatus.DELIVERING && order.getDeliveringAt() == null) {
            order.setDeliveringAt(statusChangedAt);
        }
        if (status == OrderStatus.DELIVERED) {
            if (order.getDeliveredAt() == null) order.setDeliveredAt(statusChangedAt);
            String paymentStatus = PaymentStatusPolicy.statusAfterSuccessfulDelivery(order);
            order.setPaymentStatus(paymentStatus);
            if (PaymentStatusPolicy.PAID.equals(paymentStatus) && order.getPaidAt() == null) {
                order.setPaidAt(statusChangedAt);
            }
        }
        Order saved = orderRepository.save(order);
        if (status == OrderStatus.READY) {
            deliveryTaskService.ensureWaitingTask(saved);
        }
        if (status == OrderStatus.DELIVERED) {
            orderGroupPaymentService.refreshForOrder(saved);
        }
        orderStatusHistoryService.record(
                saved.getId(), previousStatus, status, actorId, actorRole, source, reason
        );
        customerNotificationService.orderStatusChanged(saved, previousStatus);
        orderEventPublisher.publishOrderUpdate(saved);
        
        // Create notification for restaurant
        try {
            notificationService.createNotification(saved);
        } catch (Exception e) {
            // Log but don't fail order creation if notification fails
            System.err.println("[OrderService] Failed to create notification: " + e.getMessage());
        }
        
        return saved;
    }

    @Transactional
    public Order save(Order order) {
        order.setRestaurantId(RestaurantIdentityPolicy.canonicalize(order.getRestaurantId()));
        Order saved = orderRepository.save(order);
        if (PaymentStatusPolicy.isReadyForRestaurant(saved)) {
            orderEventPublisher.publishOrderUpdate(saved);
        }
        return saved;
    }

    private String generateOrderId() {
        return "ORDER-" + UUID.randomUUID().toString().replaceAll("-", "").substring(0, 12).toUpperCase();
    }
}

