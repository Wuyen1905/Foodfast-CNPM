package com.foodfast.repository;

import com.foodfast.entity.RestaurantCourier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RestaurantCourierRepository extends JpaRepository<RestaurantCourier, String> {
    List<RestaurantCourier> findByRestaurantIdOrderByNameAsc(String restaurantId);
    List<RestaurantCourier> findByRestaurantIdAndActiveTrueOrderByNameAsc(String restaurantId);
}
