package com.foodfast.repository;

import com.foodfast.entity.OrderGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface OrderGroupRepository extends JpaRepository<OrderGroup, String> {
    Optional<OrderGroup> findByIdempotencyKey(String idempotencyKey);
    List<OrderGroup> findByUserIdOrderByCreatedAtDesc(String userId);
    Optional<OrderGroup> findByPaymentSessionId(String paymentSessionId);
    Optional<OrderGroup> findByVnpayTransactionId(String vnpayTransactionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from OrderGroup g where g.id = :id")
    Optional<OrderGroup> findByIdForUpdate(@Param("id") String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from OrderGroup g where g.paymentSessionId = :paymentSessionId")
    Optional<OrderGroup> findByPaymentSessionIdForUpdate(@Param("paymentSessionId") String paymentSessionId);
}
