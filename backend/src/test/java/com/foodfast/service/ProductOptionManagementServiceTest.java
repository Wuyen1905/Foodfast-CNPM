package com.foodfast.service;

import com.foodfast.dto.ReplaceProductOptionsRequest;
import com.foodfast.entity.Product;
import com.foodfast.entity.ProductOptionGroup;
import com.foodfast.entity.ProductOptionSelectionType;
import com.foodfast.repository.ProductOptionGroupRepository;
import com.foodfast.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ProductOptionManagementServiceTest {

    @Mock ProductRepository productRepository;
    @Mock ProductOptionGroupRepository groupRepository;

    private ProductOptionManagementService service;
    private Product product;

    @BeforeEach
    void setUp() {
        service = new ProductOptionManagementService(productRepository, groupRepository);
        product = new Product();
        product.setId("ak-2");
        product.setRestaurant("Aloha");
        product.setPrice(89_000);
        lenient().when(productRepository.findById("ak-2")).thenReturn(Optional.of(product));
        lenient().when(groupRepository.findByProduct_IdOrderByDisplayOrderAscNameAsc("ak-2"))
                .thenReturn(List.of());
        lenient().when(groupRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void normalizesRequiredSingleGroupAndMarksProductCustomizable() {
        ReplaceProductOptionsRequest request = request(
                ProductOptionSelectionType.SINGLE, true, 0, 4, 2
        );

        service.replace("ak-2", "restaurant_2", request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProductOptionGroup>> captor = ArgumentCaptor.forClass(List.class);
        verify(groupRepository).saveAll(captor.capture());
        ProductOptionGroup saved = captor.getValue().get(0);
        assertThat(saved.getMinSelections()).isEqualTo(1);
        assertThat(saved.getMaxSelections()).isEqualTo(1);
        assertThat(product.isCustomizable()).isTrue();
    }

    @Test
    void rejectsForeignIdsInsteadOfOverwritingAnotherProductsOptions() {
        ReplaceProductOptionsRequest request = request(
                ProductOptionSelectionType.SINGLE, false, 0, 1, 1
        );
        request.groups.get(0).id = "other-products-group";

        assertThatThrownBy(() -> service.replace("ak-2", "restaurant_2", request))
                .hasMessageContaining("không thuộc món ăn này");
    }

    @Test
    void rejectsMaximumGreaterThanActiveOptions() {
        ReplaceProductOptionsRequest request = request(
                ProductOptionSelectionType.MULTIPLE, false, 0, 3, 2
        );

        assertThatThrownBy(() -> service.replace("ak-2", "restaurant_2", request))
                .hasMessageContaining("không được vượt số lựa chọn");
    }

    @Test
    void rejectsRestaurantThatDoesNotOwnProduct() {
        assertThatThrownBy(() -> service.replace(
                "ak-2", "rest_2",
                request(ProductOptionSelectionType.SINGLE, false, 0, 1, 1)
        )).hasMessageContaining("không có quyền");
    }

    private ReplaceProductOptionsRequest request(
            ProductOptionSelectionType type,
            boolean required,
            int minimum,
            int maximum,
            int activeOptions
    ) {
        ReplaceProductOptionsRequest request = new ReplaceProductOptionsRequest();
        ReplaceProductOptionsRequest.Group group = new ReplaceProductOptionsRequest.Group();
        group.name = "Topping";
        group.selectionType = type;
        group.required = required;
        group.minSelections = minimum;
        group.maxSelections = maximum;
        group.active = true;
        group.options = new java.util.ArrayList<>();
        for (int index = 0; index < activeOptions; index++) {
            ReplaceProductOptionsRequest.Option option = new ReplaceProductOptionsRequest.Option();
            option.name = "Lựa chọn " + index;
            option.priceDelta = index * 5_000;
            option.active = true;
            group.options.add(option);
        }
        request.groups = List.of(group);
        return request;
    }
}
