package com.foodfast.repository;

import com.foodfast.entity.DeliveryAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryAddressRepository extends JpaRepository<DeliveryAddress, String> {
    List<DeliveryAddress> findByUserIdOrderByDefaultAddressDescUpdatedAtDesc(String userId);
    Optional<DeliveryAddress> findByIdAndUserId(String id, String userId);
}
