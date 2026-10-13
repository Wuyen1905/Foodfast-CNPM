package com.foodfast.service;

import com.foodfast.entity.Product;
import com.foodfast.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class ProductAvailabilityService {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ProductRepository productRepository;

    public ProductAvailabilityService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public Product snapshot(Product product) {
        if (product == null) return null;
        product.setSoldToday(reservedToday(product.getId()));
        return product;
    }

    @Transactional(readOnly = true)
    public List<Product> snapshot(List<Product> products) {
        products.forEach(this::snapshot);
        return products;
    }

    /**
     * Verifies availability against current orders. Checkout uses a row lock so
     * concurrent customers cannot both pass the same last-portion check.
     */
    @Transactional
    public Product requireOrderable(String productId, int requestedQuantity, boolean lockForCheckout) {
        if (!StringUtils.hasText(productId)) {
            throw new IllegalArgumentException("Mã món ăn không hợp lệ");
        }
        if (requestedQuantity < 1) {
            throw new IllegalArgumentException("Số lượng món phải lớn hơn 0");
        }

        String normalizedId = productId.trim();
        Product product = (lockForCheckout
                ? productRepository.findByIdForUpdate(normalizedId)
                : productRepository.findById(normalizedId))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy món ăn"));
        snapshot(product);

        if (!product.isManuallyAvailable()) {
            throw new IllegalStateException("Món " + product.getName() + " đang tạm ngừng bán");
        }
        Integer remaining = product.getRemainingToday();
        if (remaining != null && remaining < requestedQuantity) {
            if (remaining == 0) {
                throw new IllegalStateException("Món " + product.getName() + " đã bán hết trong hôm nay");
            }
            throw new IllegalStateException("Món " + product.getName()
                    + " chỉ còn " + remaining + " phần trong hôm nay");
        }
        return product;
    }

    private int reservedToday(String productId) {
        LocalDate today = LocalDate.now(VIETNAM_ZONE);
        Instant dayStart = today.atStartOfDay(VIETNAM_ZONE).toInstant();
        Instant dayEnd = today.plusDays(1).atStartOfDay(VIETNAM_ZONE).toInstant();
        Long quantity = productRepository.countReservedQuantity(productId, dayStart, dayEnd);
        if (quantity == null) return 0;
        return Math.toIntExact(Math.max(0L, quantity));
    }
}
