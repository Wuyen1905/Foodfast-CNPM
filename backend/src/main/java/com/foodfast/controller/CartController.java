package com.foodfast.controller;

import com.foodfast.entity.CartItem;
import com.foodfast.service.CartRestaurantConflictException;
import com.foodfast.service.CartService;
import com.foodfast.security.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;
    private final CurrentUserService currentUserService;

    public CartController(CartService cartService, CurrentUserService currentUserService) {
        this.cartService = cartService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<CartItem> getCart() {
        return cartService.getCart(currentUserService.requireUser().getId());
    }

    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@Valid @RequestBody AddCartItemRequest request) {
        try {
            return ResponseEntity.ok(cartService.addItem(
                    currentUserService.requireUser().getId(),
                    request.getProductId(),
                    request.getQuantity(),
                    request.getSelectedOptionIds(),
                    request.getItemNote()
            ));
        } catch (CartRestaurantConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "code", "CART_RESTAURANT_CONFLICT",
                    "message", e.getMessage(),
                    "currentRestaurantId", e.getCurrentRestaurantId(),
                    "requestedRestaurantId", e.getRequestedRestaurantId()
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "code", "CART_INVALID_STATE",
                    "message", e.getMessage()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/reorder/{orderId}")
    public ResponseEntity<?> reorder(@PathVariable String orderId) {
        try {
            return ResponseEntity.ok(cartService.reorderOrder(
                    currentUserService.requireUser().getId(), orderId));
        } catch (CartRestaurantConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "code", "CART_RESTAURANT_CONFLICT",
                    "message", e.getMessage(),
                    "currentRestaurantId", e.getCurrentRestaurantId(),
                    "requestedRestaurantId", e.getRequestedRestaurantId()
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "code", "REORDER_INVALID_STATE",
                    "message", e.getMessage()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/item/{cartItemId}")
    public ResponseEntity<?> setCartItemQuantity(
            @PathVariable Long cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        try {
            return ResponseEntity.ok(cartService.setQuantityByCartItemId(
                    currentUserService.requireUser().getId(), cartItemId, request.getQuantity()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/item/{cartItemId}")
    public ResponseEntity<?> removeCartItem(@PathVariable Long cartItemId) {
        try {
            return ResponseEntity.ok(cartService.removeCartItem(
                    currentUserService.requireUser().getId(), cartItemId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/product/{productId}")
    public ResponseEntity<?> setQuantity(
            @PathVariable String productId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        try {
            return ResponseEntity.ok(cartService.setQuantity(
                    currentUserService.requireUser().getId(),
                    productId,
                    request.getQuantity()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/product/{productId}")
    public ResponseEntity<?> removeFromCart(@PathVariable String productId) {
        try {
            return ResponseEntity.ok(cartService.removeItem(
                    currentUserService.requireUser().getId(), productId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/clear")
    public ResponseEntity<?> clearCart() {
        try {
            return ResponseEntity.ok(cartService.clear(currentUserService.requireUser().getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    public static class AddCartItemRequest {
        @NotBlank
        private String productId;

        @NotNull
        @Min(1)
        private Integer quantity;

        private List<String> selectedOptionIds;

        private String itemNote;

        public String getProductId() {
            return productId;
        }

        public void setProductId(String productId) {
            this.productId = productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public List<String> getSelectedOptionIds() { return selectedOptionIds; }
        public void setSelectedOptionIds(List<String> selectedOptionIds) { this.selectedOptionIds = selectedOptionIds; }
        public String getItemNote() { return itemNote; }
        public void setItemNote(String itemNote) { this.itemNote = itemNote; }

    }

    public static class UpdateCartItemRequest {
        @NotNull
        @Min(1)
        private Integer quantity;
        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

    }
}

