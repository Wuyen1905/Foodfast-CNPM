package com.foodfast.service;

import com.foodfast.entity.Product;
import com.foodfast.entity.ProductOption;
import com.foodfast.entity.ProductOptionGroup;
import com.foodfast.entity.ProductOptionSelectionType;
import com.foodfast.repository.ProductOptionGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductOptionPricingServiceTest {

    @Mock
    private ProductOptionGroupRepository groupRepository;

    private ProductOptionPricingService service;
    private Product pizza;
    private ProductOptionGroup size;
    private ProductOptionGroup toppings;

    @BeforeEach
    void setUp() {
        service = new ProductOptionPricingService(groupRepository);
        pizza = new Product();
        pizza.setId("ak-2");
        pizza.setName("Pizza Hawaii");
        pizza.setPrice(89_000);
        pizza.setAvailable(true);

        size = group("size", "Kích cỡ", ProductOptionSelectionType.SINGLE, true, 1, 1, 1);
        size.addOption(option("size-m", "Vừa", 0, 1));
        size.addOption(option("size-l", "Lớn", 30_000, 2));
        toppings = group("toppings", "Topping thêm", ProductOptionSelectionType.MULTIPLE, false, 0, 2, 2);
        toppings.addOption(option("cheese", "Thêm phô mai", 15_000, 1));
        toppings.addOption(option("ham", "Thêm thịt nguội", 20_000, 2));
        when(groupRepository.findByProduct_IdAndActiveTrueOrderByDisplayOrderAscNameAsc("ak-2"))
                .thenReturn(List.of(size, toppings));
    }

    @Test
    void calculatesUnitPriceFromServerOptions() {
        ProductOptionPricingService.PricingResult result = service.price(
                pizza,
                List.of("size-l", "cheese"),
                "  Ít sốt  "
        );

        assertEquals(134_000, result.unitPrice());
        assertEquals("Kích cỡ: Lớn · Topping thêm: Thêm phô mai", result.optionSummary());
        assertEquals("Ít sốt", result.itemNote());
        assertEquals(2, result.selectedOptions().size());
    }

    @Test
    void rejectsMissingRequiredGroup() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.price(pizza, List.of("cheese"), null)
        );
        assertEquals("Vui lòng chọn đủ lựa chọn cho “Kích cỡ”", error.getMessage());
    }

    @Test
    void rejectsTooManyOptionsInGroup() {
        ProductOption extra = option("olives", "Ô-liu", 10_000, 3);
        toppings.addOption(extra);
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.price(pizza, List.of("size-m", "cheese", "ham", "olives"), null)
        );
        assertEquals("Chỉ được chọn tối đa 2 lựa chọn cho “Topping thêm”", error.getMessage());
    }

    @Test
    void rejectsOptionThatDoesNotBelongToProduct() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.price(pizza, List.of("size-m", "other-product-option"), null)
        );
        assertEquals("Lựa chọn món không hợp lệ hoặc đã ngừng bán: other-product-option", error.getMessage());
    }

    private ProductOptionGroup group(
            String id,
            String name,
            ProductOptionSelectionType type,
            boolean required,
            int min,
            int max,
            int displayOrder
    ) {
        ProductOptionGroup group = new ProductOptionGroup();
        group.setId(id);
        group.setProduct(pizza);
        group.setName(name);
        group.setSelectionType(type);
        group.setRequired(required);
        group.setMinSelections(min);
        group.setMaxSelections(max);
        group.setDisplayOrder(displayOrder);
        group.setActive(true);
        return group;
    }

    private ProductOption option(String id, String name, int delta, int displayOrder) {
        ProductOption option = new ProductOption();
        option.setId(id);
        option.setName(name);
        option.setPriceDelta(delta);
        option.setDisplayOrder(displayOrder);
        option.setActive(true);
        return option;
    }
}
