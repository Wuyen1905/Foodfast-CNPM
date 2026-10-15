package com.foodfast.repository;

import com.foodfast.entity.RestaurantSettlementOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RestaurantSettlementOrderRepository extends JpaRepository<RestaurantSettlementOrder, Long> {
    boolean existsByOrderId(String orderId);
    List<RestaurantSettlementOrder> findBySettlementIdOrderByOrderId(String settlementId);
}
