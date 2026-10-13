package com.foodfast.repository;

import com.foodfast.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, String> {

    List<Product> findByRestaurantIgnoreCase(String restaurant);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") String id);

    @Query(value = """
            SELECT COALESCE(SUM(oi.qty), 0)
              FROM order_items oi
              JOIN orders o ON o.id = oi.order_id
             WHERE oi.product_id = :productId
               AND o.created_at >= :dayStart
               AND o.created_at < :dayEnd
               AND o.status <> 'CANCELLED'
               AND (
                    LOWER(COALESCE(o.payment_method, 'cod')) <> 'vnpay'
                    OR LOWER(COALESCE(o.payment_status, 'pending')) IN ('paid', 'pending')
               )
            """, nativeQuery = true)
    Long countReservedQuantity(
            @Param("productId") String productId,
            @Param("dayStart") Instant dayStart,
            @Param("dayEnd") Instant dayEnd
    );
}

