package com.foodfast.repository;

import com.foodfast.entity.Complaint;
import com.foodfast.entity.ComplaintStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, String> {
    boolean existsByOrderId(String orderId);
    List<Complaint> findByCustomerIdOrderByCreatedAtDesc(String customerId);
    List<Complaint> findByCustomerIdAndStatusOrderByCreatedAtDesc(String customerId, ComplaintStatus status);
    List<Complaint> findByRestaurantIdOrderByCreatedAtDesc(String restaurantId);
    List<Complaint> findByRestaurantIdAndStatusOrderByCreatedAtDesc(String restaurantId, ComplaintStatus status);
    List<Complaint> findByStatusOrderByCreatedAtDesc(ComplaintStatus status);
    List<Complaint> findAllByOrderByCreatedAtDesc();
}
