package com.foodfast.repository;

import com.foodfast.entity.RestaurantSettlement;
import com.foodfast.entity.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface RestaurantSettlementRepository extends JpaRepository<RestaurantSettlement, String> {
    List<RestaurantSettlement> findAllByOrderByCreatedAtDesc();
    long countByStatus(SettlementStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from RestaurantSettlement s where s.id = :id")
    Optional<RestaurantSettlement> findByIdForUpdate(@Param("id") String id);
}
