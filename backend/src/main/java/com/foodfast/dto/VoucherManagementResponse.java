package com.foodfast.dto;

import com.foodfast.entity.Voucher;
import com.foodfast.entity.VoucherDiscountType;
import com.foodfast.entity.VoucherScope;

import java.time.Instant;

public record VoucherManagementResponse(
        String code,
        String name,
        String restaurantId,
        VoucherScope scope,
        VoucherDiscountType discountType,
        int discountValue,
        int minOrderAmount,
        int maxDiscount,
        Instant startsAt,
        Instant endsAt,
        int usageLimit,
        int perUserLimit,
        boolean active,
        long usedCount
) {
    public static VoucherManagementResponse from(Voucher voucher, long usedCount) {
        return new VoucherManagementResponse(
                voucher.getCode(),
                voucher.getName(),
                voucher.getRestaurantId(),
                voucher.getScope(),
                voucher.getDiscountType(),
                voucher.getDiscountValue(),
                voucher.getMinOrderAmount(),
                voucher.getMaxDiscount(),
                voucher.getStartsAt(),
                voucher.getEndsAt(),
                voucher.getUsageLimit(),
                voucher.getPerUserLimit(),
                voucher.isActive(),
                usedCount
        );
    }
}
