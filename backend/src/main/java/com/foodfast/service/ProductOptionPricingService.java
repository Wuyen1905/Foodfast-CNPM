package com.foodfast.service;

import com.foodfast.entity.Product;
import com.foodfast.entity.ProductOption;
import com.foodfast.entity.ProductOptionGroup;
import com.foodfast.entity.ProductOptionSelectionType;
import com.foodfast.repository.ProductOptionGroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ProductOptionPricingService {

    private final ProductOptionGroupRepository groupRepository;

    public ProductOptionPricingService(ProductOptionGroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    @Transactional(readOnly = true)
    public PricingResult price(Product product, List<String> requestedOptionIds, String requestedNote) {
        List<ProductOptionGroup> groups = groupRepository
                .findByProduct_IdAndActiveTrueOrderByDisplayOrderAscNameAsc(product.getId());
        List<String> optionIds = normalizeOptionIds(requestedOptionIds);
        String itemNote = normalizeNote(requestedNote);

        Map<String, ProductOption> availableOptions = new HashMap<>();
        for (ProductOptionGroup group : groups) {
            for (ProductOption option : group.getOptions()) {
                if (option.isActive()) availableOptions.put(option.getId(), option);
            }
        }

        List<ProductOption> selected = new ArrayList<>();
        for (String optionId : optionIds) {
            ProductOption option = availableOptions.get(optionId);
            if (option == null) {
                throw new IllegalArgumentException("Lựa chọn món không hợp lệ hoặc đã ngừng bán: " + optionId);
            }
            selected.add(option);
        }

        for (ProductOptionGroup group : groups) {
            long count = selected.stream()
                    .filter(option -> option.getGroup().getId().equals(group.getId()))
                    .count();
            int minimum = group.isRequired() ? Math.max(1, group.getMinSelections()) : group.getMinSelections();
            int maximum = group.getSelectionType() == ProductOptionSelectionType.SINGLE
                    ? 1
                    : Math.max(minimum, group.getMaxSelections());
            if (count < minimum) {
                throw new IllegalArgumentException("Vui lòng chọn đủ lựa chọn cho “" + group.getName() + "”");
            }
            if (count > maximum) {
                throw new IllegalArgumentException("Chỉ được chọn tối đa " + maximum + " lựa chọn cho “" + group.getName() + "”");
            }
        }

        selected.sort(Comparator
                .comparingInt((ProductOption option) -> option.getGroup().getDisplayOrder())
                .thenComparingInt(ProductOption::getDisplayOrder)
                .thenComparing(ProductOption::getName));

        int unitPrice = product.getPrice();
        List<SelectedOption> snapshots = new ArrayList<>();
        List<String> summaries = new ArrayList<>();
        for (ProductOption option : selected) {
            unitPrice = Math.addExact(unitPrice, option.getPriceDelta());
            snapshots.add(new SelectedOption(
                    option.getId(),
                    option.getGroup().getName(),
                    option.getName(),
                    option.getPriceDelta()
            ));
            summaries.add(option.getGroup().getName() + ": " + option.getName());
        }

        List<String> stableIds = optionIds.stream().sorted().toList();
        String variantKey = product.getId() + "|" + String.join(",", stableIds) + "|" + itemNote;
        return new PricingResult(
                unitPrice,
                stableIds,
                String.join(" · ", summaries),
                itemNote,
                variantKey,
                snapshots
        );
    }

    private List<String> normalizeOptionIds(List<String> requestedOptionIds) {
        if (requestedOptionIds == null) return List.of();
        Set<String> unique = new LinkedHashSet<>();
        for (String optionId : requestedOptionIds) {
            if (StringUtils.hasText(optionId)) unique.add(optionId.trim());
        }
        return List.copyOf(unique);
    }

    private String normalizeNote(String requestedNote) {
        if (!StringUtils.hasText(requestedNote)) return "";
        String note = requestedNote.trim();
        if (note.length() > 200) {
            throw new IllegalArgumentException("Ghi chú cho món không được vượt quá 200 ký tự");
        }
        return note;
    }

    public record SelectedOption(
            String optionId,
            String groupName,
            String optionName,
            int priceDelta
    ) {}

    public record PricingResult(
            int unitPrice,
            List<String> optionIds,
            String optionSummary,
            String itemNote,
            String variantKey,
            List<SelectedOption> selectedOptions
    ) {}
}
