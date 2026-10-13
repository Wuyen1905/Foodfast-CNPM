package com.foodfast.dto;

import java.util.List;

/**
 * Server-authoritative price preview for checkout.
 * No amount in this response is accepted from the browser when the order is created.
 */
public record OrderQuoteResponse(
        String restaurantId,
        String restaurantName,
        List<QuotedItem> items,
        int subtotal,
        int shippingFee,
        int taxAmount,
        int discountAmount,
        int total,
        String voucherCode,
        String voucherName
) {
    public record QuotedItem(
            String productId,
            String name,
            int quantity,
            int unitPrice,
            int lineTotal,
            List<QuotedOption> selectedOptions,
            String itemNote
    ) {
    }

    public record QuotedOption(
            String optionId,
            String groupName,
            String optionName,
            int priceDelta
    ) {
    }
}
