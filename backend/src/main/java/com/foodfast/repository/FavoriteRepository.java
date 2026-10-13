package com.foodfast.repository;

import com.foodfast.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUserIdOrderByCreatedAtDesc(String userId);
    Optional<Favorite> findByUserIdAndProductId(String userId, String productId);
    long deleteByUserIdAndProductId(String userId, String productId);
}
