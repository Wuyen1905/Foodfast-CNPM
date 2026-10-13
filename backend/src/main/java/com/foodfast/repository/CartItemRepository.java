package com.foodfast.repository;

import com.foodfast.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByProductId(String productId);
    Optional<CartItem> findByUserIdAndProductId(String userId, String productId);
    Optional<CartItem> findByUserIdAndVariantKey(String userId, String variantKey);
    Optional<CartItem> findByIdAndUserId(Long id, String userId);
    List<CartItem> findByUserIdOrderByIdAsc(String userId);
    long deleteByUserId(String userId);
}

