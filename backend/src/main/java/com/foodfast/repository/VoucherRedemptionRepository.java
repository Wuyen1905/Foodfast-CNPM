package com.foodfast.repository;

import com.foodfast.entity.VoucherRedemption;
import com.foodfast.entity.VoucherRedemptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VoucherRedemptionRepository extends JpaRepository<VoucherRedemption, Long> {
    long countByVoucherCodeAndStatusNot(String voucherCode, VoucherRedemptionStatus status);
    long countByVoucherCodeAndUserIdAndStatusNot(String voucherCode, String userId, VoucherRedemptionStatus status);
    Optional<VoucherRedemption> findByOrderId(String orderId);
}
