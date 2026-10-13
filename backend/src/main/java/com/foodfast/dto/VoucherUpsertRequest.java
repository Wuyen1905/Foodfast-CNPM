package com.foodfast.dto;

import com.foodfast.entity.VoucherDiscountType;

import java.time.Instant;

public record VoucherUpsertRequest(
        String code,
        String name,
        String restaurantId,
        VoucherDiscountType discountType,
        Integer discountValue,
        Integer minOrderAmount,
        Integer maxDiscount,
        Instant startsAt,
        Instant endsAt,
        Integer usageLimit,
        Integer perUserLimit,
        Boolean active
) {
}
