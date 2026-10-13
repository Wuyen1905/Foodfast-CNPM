package com.foodfast.dto;

import com.foodfast.entity.ProductOptionGroup;
import com.foodfast.entity.ProductOptionSelectionType;

import java.util.List;

public record ProductOptionGroupResponse(
        String id,
        String productId,
        String name,
        ProductOptionSelectionType selectionType,
        boolean required,
        int minSelections,
        int maxSelections,
        int displayOrder,
        boolean active,
        List<ProductOptionResponse> options
) {
    public static ProductOptionGroupResponse activeOnly(ProductOptionGroup group) {
        return new ProductOptionGroupResponse(
                group.getId(),
                group.getProduct().getId(),
                group.getName(),
                group.getSelectionType(),
                group.isRequired(),
                group.getMinSelections(),
                group.getMaxSelections(),
                group.getDisplayOrder(),
                group.isActive(),
                group.getOptions().stream()
                        .filter(option -> option.isActive())
                        .map(option -> new ProductOptionResponse(
                                option.getId(), group.getId(), option.getName(),
                                option.getPriceDelta(), option.getDisplayOrder(), option.isActive()
                        ))
                        .toList()
        );
    }

    public record ProductOptionResponse(
            String id,
            String groupId,
            String name,
            int priceDelta,
            int displayOrder,
            boolean active
    ) {
    }
}
