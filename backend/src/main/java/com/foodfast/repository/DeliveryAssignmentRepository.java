package com.foodfast.repository;

import com.foodfast.entity.DeliveryAssignment;
import com.foodfast.entity.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, String> {
    Optional<DeliveryAssignment> findFirstByOrderIdOrderByAssignedAtDesc(String orderId);
    List<DeliveryAssignment> findByRestaurantIdOrderByAssignedAtDesc(String restaurantId);
    List<DeliveryAssignment> findByStatusInOrderByAssignedAtAsc(Collection<DeliveryStatus> statuses);
    boolean existsByCourierIdAndStatusIn(String courierId, Collection<DeliveryStatus> statuses);
}
