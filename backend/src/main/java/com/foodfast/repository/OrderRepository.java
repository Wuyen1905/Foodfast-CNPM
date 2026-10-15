package com.foodfast.repository;

import com.foodfast.entity.Order;
import com.foodfast.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, String>, JpaSpecificationExecutor<Order> {

    List<Order> findByPaymentSessionId(String paymentSessionId);

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    Optional<Order> findByVnpayTransactionId(String vnpayTransactionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.paymentSessionId = :paymentSessionId")
    Optional<Order> findByPaymentSessionIdForUpdate(@Param("paymentSessionId") String paymentSessionId);

    List<Order> findByCustomerPhoneContainingIgnoreCase(String phone);

    // Filter by restaurantId instead of restaurant name
    List<Order> findByRestaurantId(String restaurantId);

    // Include canonical IDs and legacy aliases while old data is being migrated.
    List<Order> findByRestaurantIdIn(List<String> restaurantIds);

    // Filter by phone + restaurantId
    List<Order> findByCustomerPhoneContainingIgnoreCaseAndRestaurantId(String phone, String restaurantId);

    List<Order> findByCustomerPhoneContainingIgnoreCaseAndRestaurantIdIn(
            String phone,
            List<String> restaurantIds
    );

    // Filter by the logged-in customer's user id
    List<Order> findByUserId(String userId);

    List<Order> findByOrderGroupIdOrderByGroupPositionAsc(String orderGroupId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.restaurantId = :restaurantId and o.status = :status " +
            "and o.deliveredAt >= :fromMillis and o.deliveredAt < :toMillis order by o.deliveredAt")
    List<Order> findSettlementCandidatesForUpdate(
            @Param("restaurantId") String restaurantId,
            @Param("status") OrderStatus status,
            @Param("fromMillis") long fromMillis,
            @Param("toMillis") long toMillis
    );

    @Query("select o from Order o where o.status = :status and o.deliveredAt >= :fromMillis " +
            "and o.deliveredAt < :toMillis order by o.restaurantId, o.deliveredAt")
    List<Order> findSettlementCandidates(
            @Param("status") OrderStatus status,
            @Param("fromMillis") long fromMillis,
            @Param("toMillis") long toMillis
    );
}

