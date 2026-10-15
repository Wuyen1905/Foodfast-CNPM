package com.foodfast.dto;

import java.util.List;

public record OrderGroupQuoteResponse(
        List<OrderQuoteResponse> orders,
        int subtotal,
        int shippingFee,
        int taxAmount,
        int discountAmount,
        int total
) { }
