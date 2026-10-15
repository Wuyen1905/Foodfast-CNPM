package com.foodfast.repository;

import com.foodfast.entity.RestaurantCourier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;

public interface RestaurantCourierRepository extends JpaRepository<RestaurantCourier, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from RestaurantCourier c where c.id = :id")
    java.util.Optional<RestaurantCourier> findByIdForUpdate(@Param("id") String id);
    List<RestaurantCourier> findAllByOrderByNameAsc();
    List<RestaurantCourier> findByActiveTrueOrderByNameAsc();
    List<RestaurantCourier> findByRestaurantIdOrderByNameAsc(String restaurantId);
    List<RestaurantCourier> findByRestaurantIdAndActiveTrueOrderByNameAsc(String restaurantId);
}
