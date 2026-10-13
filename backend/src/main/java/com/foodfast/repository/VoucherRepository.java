package com.foodfast.repository;

import com.foodfast.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, String> {
    Optional<Voucher> findByCodeIgnoreCase(String code);

    List<Voucher> findAllByOrderByStartsAtDesc();

    List<Voucher> findByRestaurantIdOrderByStartsAtDesc(String restaurantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Voucher v where upper(v.code) = upper(:code)")
    Optional<Voucher> findByCodeForUpdate(@Param("code") String code);
}
