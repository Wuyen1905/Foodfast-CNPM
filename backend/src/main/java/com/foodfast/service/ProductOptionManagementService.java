package com.foodfast.service;

import com.foodfast.dto.ReplaceProductOptionsRequest;
import com.foodfast.entity.Product;
import com.foodfast.entity.ProductOption;
import com.foodfast.entity.ProductOptionGroup;
import com.foodfast.entity.ProductOptionSelectionType;
import com.foodfast.repository.ProductOptionGroupRepository;
import com.foodfast.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ProductOptionManagementService {

    private final ProductRepository productRepository;
    private final ProductOptionGroupRepository groupRepository;

    public ProductOptionManagementService(
            ProductRepository productRepository,
            ProductOptionGroupRepository groupRepository
    ) {
        this.productRepository = productRepository;
        this.groupRepository = groupRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductOptionGroup> getActiveGroups(String productId) {
        requireProduct(productId);
        return groupRepository.findByProduct_IdAndActiveTrueOrderByDisplayOrderAscNameAsc(productId);
    }

    @Transactional(readOnly = true)
    public List<ProductOptionGroup> getAllGroups(String productId) {
        requireProduct(productId);
        return groupRepository.findByProduct_IdOrderByDisplayOrderAscNameAsc(productId);
    }

    @Transactional
    public List<ProductOptionGroup> replace(
            String productId,
            String authenticatedRestaurantId,
            ReplaceProductOptionsRequest request
    ) {
        Product product = requireProduct(productId);
        if (request == null || !StringUtils.hasText(authenticatedRestaurantId)
                || !RestaurantIdentityPolicy.refersToSameRestaurant(
                        authenticatedRestaurantId,
                        product.getRestaurant()
                )) {
            throw new IllegalArgumentException("Nhà hàng không có quyền sửa lựa chọn của món này");
        }

        List<ProductOptionGroup> existingGroups = groupRepository
                .findByProduct_IdOrderByDisplayOrderAscNameAsc(productId);
        Set<String> existingGroupIds = new HashSet<>();
        Set<String> existingOptionIds = new HashSet<>();
        for (ProductOptionGroup group : existingGroups) {
            existingGroupIds.add(group.getId());
            group.getOptions().forEach(option -> existingOptionIds.add(option.getId()));
        }

        List<ProductOptionGroup> replacements = new ArrayList<>();
        Set<String> submittedGroupIds = new HashSet<>();
        Set<String> submittedOptionIds = new HashSet<>();
        if (request.groups != null) {
            if (request.groups.size() > 10) {
                throw new IllegalArgumentException("Mỗi món chỉ được có tối đa 10 nhóm lựa chọn");
            }
            for (ReplaceProductOptionsRequest.Group source : request.groups) {
                if (source == null || !StringUtils.hasText(source.name)) {
                    throw new IllegalArgumentException("Tên nhóm lựa chọn không được để trống");
                }
                if (source.name.trim().length() > 100) {
                    throw new IllegalArgumentException("Tên nhóm lựa chọn không được vượt quá 100 ký tự");
                }
                ProductOptionGroup group = new ProductOptionGroup();
                String groupId = resolveId(source.id, existingGroupIds, "nhóm lựa chọn");
                if (!submittedGroupIds.add(groupId)) {
                    throw new IllegalArgumentException("Mã nhóm lựa chọn bị trùng");
                }
                group.setId(groupId);
                group.setProduct(product);
                group.setName(source.name.trim());
                group.setSelectionType(source.selectionType == null
                        ? ProductOptionSelectionType.SINGLE : source.selectionType);
                int minimum = Math.max(0, source.minSelections);
                if (source.required) minimum = Math.max(1, minimum);
                int maximum = group.getSelectionType() == ProductOptionSelectionType.SINGLE
                        ? 1 : source.maxSelections;
                if (maximum <= 0 || maximum < minimum) {
                    throw new IllegalArgumentException("Số lựa chọn tối đa phải lớn hơn hoặc bằng số tối thiểu");
                }
                group.setRequired(source.required);
                group.setMinSelections(minimum);
                group.setMaxSelections(maximum);
                group.setDisplayOrder(source.displayOrder);
                group.setActive(source.active);
                if (source.options == null || source.options.isEmpty()) {
                    throw new IllegalArgumentException("Nhóm “" + group.getName() + "” phải có ít nhất một lựa chọn");
                }
                if (source.options.size() > 30) {
                    throw new IllegalArgumentException("Mỗi nhóm chỉ được có tối đa 30 lựa chọn");
                }
                Set<String> optionNames = new HashSet<>();
                int activeOptions = 0;
                for (ReplaceProductOptionsRequest.Option optionSource : source.options) {
                    if (optionSource == null || !StringUtils.hasText(optionSource.name)) {
                        throw new IllegalArgumentException("Tên lựa chọn không được để trống");
                    }
                    String optionName = optionSource.name.trim();
                    if (optionName.length() > 120) {
                        throw new IllegalArgumentException("Tên lựa chọn không được vượt quá 120 ký tự");
                    }
                    if (!optionNames.add(optionName.toLowerCase())) {
                        throw new IllegalArgumentException("Tên lựa chọn trong cùng nhóm không được trùng");
                    }
                    if (optionSource.priceDelta < 0 || optionSource.priceDelta > 10_000_000) {
                        throw new IllegalArgumentException("Giá cộng thêm phải từ 0 đến 10.000.000 đ");
                    }
                    ProductOption option = new ProductOption();
                    String optionId = resolveId(optionSource.id, existingOptionIds, "lựa chọn");
                    if (!submittedOptionIds.add(optionId)) {
                        throw new IllegalArgumentException("Mã lựa chọn bị trùng");
                    }
                    option.setId(optionId);
                    option.setName(optionName);
                    option.setPriceDelta(optionSource.priceDelta);
                    option.setDisplayOrder(optionSource.displayOrder);
                    option.setActive(optionSource.active);
                    if (optionSource.active) activeOptions++;
                    group.addOption(option);
                }
                if (group.isActive() && activeOptions == 0) {
                    throw new IllegalArgumentException("Nhóm “" + group.getName() + "” phải có lựa chọn đang hoạt động");
                }
                if (group.isActive() && group.getMaxSelections() > activeOptions) {
                    throw new IllegalArgumentException("Số lựa chọn tối đa của “" + group.getName()
                            + "” không được vượt số lựa chọn đang hoạt động");
                }
                replacements.add(group);
            }
        }

        groupRepository.deleteByProduct_Id(productId);
        groupRepository.flush();
        groupRepository.saveAll(replacements);
        product.setCustomizable(replacements.stream().anyMatch(ProductOptionGroup::isActive));
        productRepository.save(product);
        return groupRepository.findByProduct_IdOrderByDisplayOrderAscNameAsc(productId);
    }

    private Product requireProduct(String productId) {
        if (!StringUtils.hasText(productId)) throw new IllegalArgumentException("Mã món ăn không hợp lệ");
        return productRepository.findById(productId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy món ăn"));
    }

    private String resolveId(String submittedId, Set<String> allowedExistingIds, String label) {
        if (!StringUtils.hasText(submittedId)) return UUID.randomUUID().toString();
        String normalized = submittedId.trim();
        if (!allowedExistingIds.contains(normalized)) {
            throw new IllegalArgumentException("Mã " + label + " không thuộc món ăn này");
        }
        return normalized;
    }
}
