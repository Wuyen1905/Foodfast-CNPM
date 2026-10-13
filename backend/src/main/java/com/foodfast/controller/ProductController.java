package com.foodfast.controller;

import com.foodfast.entity.Product;
import com.foodfast.repository.ProductRepository;
import com.foodfast.security.CurrentUserService;
import com.foodfast.service.ProductAvailabilityService;
import com.foodfast.service.RestaurantIdentityPolicy;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Product Controller
 * Provides full CRUD operations for products matching frontend ProductApi.
 */
@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductRepository productRepository;
    private final CurrentUserService currentUserService;
    private final ProductAvailabilityService availabilityService;

    public ProductController(
            ProductRepository productRepository,
            CurrentUserService currentUserService,
            ProductAvailabilityService availabilityService
    ) {
        this.productRepository = productRepository;
        this.currentUserService = currentUserService;
        this.availabilityService = availabilityService;
    }

    @GetMapping
    public List<Product> getProducts(@RequestParam(required = false) String restaurant) {
        List<Product> products;
        if (!StringUtils.hasText(restaurant)) {
            products = productRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
        } else {
            products = productRepository.findByRestaurantIgnoreCase(restaurant.trim());
        }
        return availabilityService.snapshot(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable String id) {
        return productRepository.findById(id)
                .map(availabilityService::snapshot)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        if (currentUserService.isRestaurant()) {
            product.setRestaurant(RestaurantIdentityPolicy.productLabel(
                    currentUserService.requireRestaurantId()));
        }
        if (!StringUtils.hasText(product.getId())) {
            product.setId("PRODUCT-" + UUID.randomUUID()
                    .toString().replace("-", "").substring(0, 12).toUpperCase());
        }
        Product saved = productRepository.save(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(availabilityService.snapshot(saved));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateProduct(
            @PathVariable String id,
            @RequestBody Map<String, Object> updates
    ) {
        Optional<Product> productOpt = productRepository.findById(id);
        if (productOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Product product = productOpt.get();
        currentUserService.requireRestaurantAccess(product.getRestaurant());

        if (updates.containsKey("name")) {
            product.setName(String.valueOf(updates.get("name")));
        }
        if (updates.containsKey("description")) {
            product.setDescription(String.valueOf(updates.get("description")));
        }
        if (updates.containsKey("price") && updates.get("price") instanceof Number number) {
            product.setPrice(number.intValue());
        }
        if (updates.containsKey("category")) {
            product.setCategory(String.valueOf(updates.get("category")));
        }
        if (updates.containsKey("imageUrl") || updates.containsKey("image")) {
            Object imageObj = updates.get("imageUrl");
            if (imageObj == null) imageObj = updates.get("image");
            product.setImageUrl(imageObj == null ? null : String.valueOf(imageObj));
        }
        if (updates.containsKey("restaurant") && currentUserService.isAdmin()) {
            product.setRestaurant(String.valueOf(updates.get("restaurant")));
        }
        if (updates.containsKey("available")) {
            Object availableObj = updates.get("available");
            if (availableObj instanceof Boolean value) {
                product.setAvailable(value);
            } else if (availableObj instanceof String value) {
                product.setAvailable(Boolean.parseBoolean(value));
            }
        }
        if (updates.containsKey("customizable")) {
            Object customizableObj = updates.get("customizable");
            if (customizableObj instanceof Boolean value) {
                product.setCustomizable(value);
            } else if (customizableObj instanceof String value) {
                product.setCustomizable(Boolean.parseBoolean(value));
            }
        }
        if (updates.containsKey("dailyLimit")) {
            Object limitObj = updates.get("dailyLimit");
            try {
                if (limitObj == null || (limitObj instanceof String value && !StringUtils.hasText(value))) {
                    product.setDailyLimit(null);
                } else if (limitObj instanceof Number number) {
                    product.setDailyLimit(number.intValue());
                } else {
                    product.setDailyLimit(Integer.parseInt(String.valueOf(limitObj)));
                }
            } catch (NumberFormatException exception) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Giới hạn bán mỗi ngày phải là số nguyên dương"
                ));
            }
        }

        Product saved = productRepository.save(product);
        return ResponseEntity.ok(availabilityService.snapshot(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable String id) {
        Optional<Product> product = productRepository.findById(id);
        if (product.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        currentUserService.requireRestaurantAccess(product.get().getRestaurant());
        productRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
