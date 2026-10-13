package com.foodfast.repository;

import com.foodfast.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, String> {
    Optional<Review> findByOrderId(String orderId);
    List<Review> findByRestaurantIdOrderByCreatedAtDesc(String restaurantId);

    @Query("select avg(r.rating) from Review r where r.restaurantId = :restaurantId")
    Double averageRating(@Param("restaurantId") String restaurantId);
}
