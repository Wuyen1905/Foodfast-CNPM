package com.foodfast.dto;

import com.foodfast.entity.VoucherDiscountType;

import java.time.Instant;

public record AvailableVoucherResponse(
        String code,
        String name,
        VoucherDiscountType discountType,
        int discountValue,
        int minOrderAmount,
        int maxDiscount,
        Instant endsAt,
        boolean eligible,
        int amountNeeded,
        int estimatedDiscount,
        String message
) {
}
