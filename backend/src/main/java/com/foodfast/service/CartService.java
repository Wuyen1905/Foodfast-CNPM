package com.foodfast.service;

import com.foodfast.entity.CartItem;
import com.foodfast.entity.Order;
import com.foodfast.entity.OrderItem;
import com.foodfast.entity.OrderStatus;
import com.foodfast.entity.Product;
import com.foodfast.repository.CartItemRepository;
import com.foodfast.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final RealtimeService realtimeService;
    private final ProductOptionPricingService optionPricingService;
    private final ProductAvailabilityService productAvailabilityService;

    public CartService(
            CartItemRepository cartItemRepository,
            OrderRepository orderRepository,
            RealtimeService realtimeService,
            ProductOptionPricingService optionPricingService,
            ProductAvailabilityService productAvailabilityService
    ) {
        this.cartItemRepository = cartItemRepository;
        this.orderRepository = orderRepository;
        this.realtimeService = realtimeService;
        this.optionPricingService = optionPricingService;
        this.productAvailabilityService = productAvailabilityService;
    }

    @Transactional(readOnly = true)
    public List<CartItem> getCart(String userId) {
        if (!StringUtils.hasText(userId)) {
            return List.of();
        }
        return cartItemRepository.findByUserIdOrderByIdAsc(userId.trim());
    }

    @Transactional
    public List<CartItem> addItem(String userId, String productId, int quantity) {
        return addItem(userId, productId, quantity, List.of(), null);
    }

    @Transactional
    public List<CartItem> addItem(
            String userId,
            String productId,
            int quantity,
            List<String> selectedOptionIds,
            String itemNote
    ) {
        String normalizedUserId = requireText(userId, "Không xác định được khách hàng của giỏ hàng");
        addItemInternal(normalizedUserId, productId, quantity, selectedOptionIds, itemNote);
        return publish(normalizedUserId);
    }

    @Transactional
    public ReorderResult reorderOrder(String userId, String orderId) {
        String normalizedUserId = requireText(userId, "Không xác định được khách hàng của giỏ hàng");
        String normalizedOrderId = requireText(orderId, "Mã đơn hàng không hợp lệ");
        Order order = orderRepository.findById(normalizedOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));
        if (!normalizedUserId.equals(order.getUserId())) {
            throw new IllegalArgumentException("Bạn chỉ có thể đặt lại đơn hàng của chính mình");
        }
        if (order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.CANCELLED) {
            throw new IllegalStateException("Chỉ có thể đặt lại đơn đã giao hoặc đã hủy");
        }
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new IllegalStateException("Đơn hàng cũ không có món để đặt lại");
        }

        List<OrderItem> copyableItems = new ArrayList<>();
        List<ReorderConfigurationItem> requiresConfiguration = new ArrayList<>();

        // Đơn cũ có thể không còn khớp cấu hình món hiện tại. Không tự chọn
        // topping/hương vị thay khách: chỉ sao chép món hợp lệ và trả những món
        // cần cấu hình lại để giao diện đưa khách tới đúng trang chi tiết món.
        for (OrderItem orderItem : order.getItems()) {
            Product product = requireAvailableProduct(orderItem.getProductId(), orderItem.getQuantity());
            if (!RestaurantIdentityPolicy.refersToSameRestaurant(
                    order.getRestaurantId(), restaurantIdOf(product))) {
                throw new IllegalStateException("Một món trong đơn cũ không còn thuộc nhà hàng này");
            }
            try {
                optionPricingService.price(product, optionIdsOf(orderItem), orderItem.getItemNote());
                copyableItems.add(orderItem);
            } catch (IllegalArgumentException configurationError) {
                requiresConfiguration.add(new ReorderConfigurationItem(
                        product.getId(),
                        product.getName(),
                        orderItem.getQuantity(),
                        orderItem.getItemNote(),
                        configurationError.getMessage()
                ));
            }
        }

        for (OrderItem orderItem : copyableItems) {
            addItemInternal(
                    normalizedUserId,
                    orderItem.getProductId(),
                    orderItem.getQuantity(),
                    optionIdsOf(orderItem),
                    orderItem.getItemNote()
            );
        }
        List<CartItem> cart = publish(normalizedUserId);
        return new ReorderResult(cart, copyableItems.size(), List.copyOf(requiresConfiguration));
    }

    private void addItemInternal(
            String normalizedUserId,
            String productId,
            int quantity,
            List<String> selectedOptionIds,
            String itemNote
    ) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Số lượng món phải lớn hơn 0");
        }
        Product product = requireAvailableProduct(productId, quantity);
        ProductOptionPricingService.PricingResult pricing = optionPricingService.price(
                product, selectedOptionIds, itemNote
        );
        String requestedRestaurantId = restaurantIdOf(product);
        CartItem item = cartItemRepository
                .findByUserIdAndVariantKey(normalizedUserId, pricing.variantKey())
                .orElseGet(CartItem::new);
        item.setUserId(normalizedUserId);
        item.setProductId(product.getId());
        item.setProductName(product.getName());
        item.setProductImage(product.getImageUrl());
        item.setUnitPrice((double) pricing.unitPrice());
        item.setRestaurantCode(requestedRestaurantId);
        item.setVariantKey(pricing.variantKey());
        item.setSelectedOptionIds(pricing.optionIds());
        item.setOptionSummary(pricing.optionSummary());
        item.setItemNote(pricing.itemNote());
        item.setQuantity((item.getQuantity() == null ? 0 : item.getQuantity()) + quantity);
        cartItemRepository.save(item);
    }

    private List<String> optionIdsOf(OrderItem item) {
        if (item.getSelectedOptions() == null) return List.of();
        return item.getSelectedOptions().stream()
                .map(option -> option.getOptionId())
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
    }

    @Transactional
    public List<CartItem> setQuantity(String userId, String productId, int quantity) {
        String normalizedUserId = requireText(userId, "Không xác định được khách hàng của giỏ hàng");
        if (quantity < 1) {
            throw new IllegalArgumentException("Số lượng món phải lớn hơn 0");
        }
        CartItem item = cartItemRepository
                .findByUserIdAndProductId(normalizedUserId, productId)
                .orElseThrow(() -> new IllegalArgumentException("Món không có trong giỏ hàng"));
        productAvailabilityService.requireOrderable(item.getProductId(), quantity, false);
        item.setQuantity(quantity);
        cartItemRepository.save(item);
        return publish(normalizedUserId);
    }

    @Transactional
    public List<CartItem> setQuantityByCartItemId(String userId, Long cartItemId, int quantity) {
        String normalizedUserId = requireText(userId, "Không xác định được khách hàng của giỏ hàng");
        if (quantity < 1) throw new IllegalArgumentException("Số lượng món phải lớn hơn 0");
        CartItem item = cartItemRepository.findByIdAndUserId(cartItemId, normalizedUserId)
                .orElseThrow(() -> new IllegalArgumentException("Món không có trong giỏ hàng"));
        productAvailabilityService.requireOrderable(item.getProductId(), quantity, false);
        item.setQuantity(quantity);
        cartItemRepository.save(item);
        return publish(normalizedUserId);
    }

    @Transactional
    public List<CartItem> removeItem(String userId, String productId) {
        String normalizedUserId = requireText(userId, "Không xác định được khách hàng của giỏ hàng");
        CartItem item = cartItemRepository
                .findByUserIdAndProductId(normalizedUserId, productId)
                .orElseThrow(() -> new IllegalArgumentException("Món không có trong giỏ hàng"));
        cartItemRepository.delete(item);
        return publish(normalizedUserId);
    }

    @Transactional
    public List<CartItem> removeCartItem(String userId, Long cartItemId) {
        String normalizedUserId = requireText(userId, "Không xác định được khách hàng của giỏ hàng");
        CartItem item = cartItemRepository.findByIdAndUserId(cartItemId, normalizedUserId)
                .orElseThrow(() -> new IllegalArgumentException("Món không có trong giỏ hàng"));
        cartItemRepository.delete(item);
        return publish(normalizedUserId);
    }

    @Transactional
    public List<CartItem> clear(String userId) {
        String normalizedUserId = requireText(userId, "Không xác định được khách hàng của giỏ hàng");
        cartItemRepository.deleteByUserId(normalizedUserId);
        return publish(normalizedUserId);
    }

    private Product requireAvailableProduct(String productId, int quantity) {
        String normalizedProductId = requireText(productId, "Mã món ăn không hợp lệ");
        return productAvailabilityService.requireOrderable(normalizedProductId, quantity, false);
    }

    private String restaurantIdOf(Product product) {
        return RestaurantIdentityPolicy.canonicalize(product.getRestaurant());
    }

    private List<CartItem> publish(String userId) {
        List<CartItem> updatedCart = cartItemRepository.findByUserIdOrderByIdAsc(userId);
        realtimeService.sendCartUpdate(userId, updatedCart);
        return updatedCart;
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    public record ReorderConfigurationItem(
            String productId,
            String productName,
            int quantity,
            String itemNote,
            String reason
    ) { }

    public record ReorderResult(
            List<CartItem> cart,
            int copiedItemCount,
            List<ReorderConfigurationItem> requiresConfiguration
    ) { }
}
