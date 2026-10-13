package com.foodfast.controller;

import com.foodfast.dto.ReplaceProductOptionsRequest;
import com.foodfast.dto.ProductOptionGroupResponse;
import com.foodfast.entity.Product;
import com.foodfast.entity.ProductOptionGroup;
import com.foodfast.repository.ProductRepository;
import com.foodfast.security.CurrentUserService;
import com.foodfast.service.ProductOptionManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products/{productId}/option-groups")
public class ProductOptionController {

    private final ProductOptionManagementService service;
    private final ProductRepository productRepository;
    private final CurrentUserService currentUser;

    public ProductOptionController(
            ProductOptionManagementService service,
            ProductRepository productRepository,
            CurrentUserService currentUser
    ) {
        this.service = service;
        this.productRepository = productRepository;
        this.currentUser = currentUser;
    }

    @GetMapping
    public ResponseEntity<?> list(@PathVariable String productId) {
        try {
            List<ProductOptionGroup> groups = service.getActiveGroups(productId);
            return ResponseEntity.ok(groups.stream()
                    .map(ProductOptionGroupResponse::activeOnly)
                    .toList());
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
        }
    }

    @GetMapping("/manage")
    public ResponseEntity<?> listForManagement(@PathVariable String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy món ăn: " + productId));
        currentUser.requireRestaurantAccess(product.getRestaurant());
        return ResponseEntity.ok(service.getAllGroups(productId));
    }

    @PutMapping
    public ResponseEntity<?> replace(
            @PathVariable String productId,
            @RequestBody ReplaceProductOptionsRequest request
    ) {
        try {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy món ăn: " + productId));
            currentUser.requireRestaurantAccess(product.getRestaurant());
            return ResponseEntity.ok(service.replace(productId, product.getRestaurant(), request));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
        }
    }
}
