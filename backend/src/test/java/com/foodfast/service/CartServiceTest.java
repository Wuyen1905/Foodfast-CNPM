package com.foodfast.service;

import com.foodfast.entity.CartItem;
import com.foodfast.entity.Order;
import com.foodfast.entity.OrderItem;
import com.foodfast.entity.OrderStatus;
import com.foodfast.entity.Product;
import com.foodfast.entity.ProductOption;
import com.foodfast.entity.ProductOptionGroup;
import com.foodfast.entity.ProductOptionSelectionType;
import com.foodfast.repository.CartItemRepository;
import com.foodfast.repository.OrderRepository;
import com.foodfast.repository.ProductRepository;
import com.foodfast.repository.ProductOptionGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RealtimeService realtimeService;

    @Mock
    private ProductOptionGroupRepository productOptionGroupRepository;

    private CartService service;

    @BeforeEach
    void setUp() {
        lenient().when(productOptionGroupRepository.findByProduct_IdAndActiveTrueOrderByDisplayOrderAscNameAsc(any()))
                .thenReturn(List.of());
        service = new CartService(
                cartItemRepository,
                orderRepository,
                realtimeService,
                new ProductOptionPricingService(productOptionGroupRepository),
                new ProductAvailabilityService(productRepository)
        );
    }

    @Test
    void derivesRestaurantAndPriceFromProductCatalog() {
        Product pizza = product("ak-2", "Pizza Hawaii", 89_000, "Aloha");
        when(productRepository.findById("ak-2")).thenReturn(Optional.of(pizza));
        when(cartItemRepository.findByUserIdOrderByIdAsc("u2")).thenReturn(List.of());
        when(cartItemRepository.findByUserIdAndVariantKey("u2", "ak-2||"))
                .thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.addItem("u2", "ak-2", 1);

        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        CartItem saved = captor.getValue();
        assertEquals("restaurant_2", saved.getRestaurantCode());
        assertEquals(89_000d, saved.getUnitPrice());
        assertEquals("u2", saved.getUserId());
    }

    @Test
    void acceptsProductFromAnotherRestaurantForGroupedCheckout() {
        CartItem existing = new CartItem();
        existing.setUserId("u2");
        existing.setProductId("sd-1");
        existing.setRestaurantCode("rest_2");
        Product pizza = product("ak-2", "Pizza Hawaii", 89_000, "Aloha");
        when(productRepository.findById("ak-2")).thenReturn(Optional.of(pizza));
        when(cartItemRepository.findByUserIdOrderByIdAsc("u2"))
                .thenReturn(List.of(existing));
        when(cartItemRepository.findByUserIdAndVariantKey("u2", "ak-2||"))
                .thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.addItem("u2", "ak-2", 1);

        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        assertEquals("restaurant_2", captor.getValue().getRestaurantCode());
    }

    @Test
    void settingQuantityUsesAbsoluteValueInsteadOfAddingAgain() {
        CartItem existing = new CartItem();
        existing.setUserId("u2");
        existing.setProductId("ak-2");
        existing.setQuantity(1);
        when(cartItemRepository.findByUserIdAndProductId("u2", "ak-2"))
                .thenReturn(Optional.of(existing));
        when(productRepository.findById("ak-2"))
                .thenReturn(Optional.of(product("ak-2", "Pizza Hawaii", 89_000, "Aloha")));
        when(cartItemRepository.findByUserIdOrderByIdAsc("u2")).thenReturn(List.of(existing));

        service.setQuantity("u2", "ak-2", 2);

        assertEquals(2, existing.getQuantity());
        verify(cartItemRepository).save(existing);
    }

    @Test
    void reordersDeliveredOrderUsingCurrentCatalogPrice() {
        Product pizza = product("ak-2", "Pizza Hawaii", 95_000, "Aloha");
        OrderItem oldItem = new OrderItem();
        oldItem.setProductId("ak-2");
        oldItem.setName("Pizza Hawaii");
        oldItem.setQuantity(2);
        oldItem.setPrice(89_000);
        Order order = new Order();
        order.setId("ORDER-OLD");
        order.setUserId("u2");
        order.setRestaurantId("restaurant_2");
        order.setStatus(OrderStatus.DELIVERED);
        order.setItems(List.of(oldItem));

        when(orderRepository.findById("ORDER-OLD")).thenReturn(Optional.of(order));
        when(productRepository.findById("ak-2")).thenReturn(Optional.of(pizza));
        when(cartItemRepository.findByUserIdOrderByIdAsc("u2")).thenReturn(List.of());
        when(cartItemRepository.findByUserIdAndVariantKey("u2", "ak-2||"))
                .thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CartService.ReorderResult result = service.reorderOrder("u2", "ORDER-OLD");

        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        assertEquals(2, captor.getValue().getQuantity());
        assertEquals(95_000d, captor.getValue().getUnitPrice());
        assertEquals(1, result.copiedItemCount());
        assertEquals(0, result.requiresConfiguration().size());
    }

    @Test
    void asksCustomerToReconfigureOldItemWhenRequiredOptionWasNotStored() {
        Product donut = product("sd-1", "Bánh Donut", 25_000, "SweetDreams");
        ProductOptionGroup flavor = requiredSingleGroup(donut, "sd-1-flavor", "Hương vị");

        OrderItem oldItem = new OrderItem();
        oldItem.setProductId("sd-1");
        oldItem.setName("Bánh Donut");
        oldItem.setQuantity(2);
        Order order = new Order();
        order.setId("ORDER-OLD");
        order.setUserId("u2");
        order.setRestaurantId("rest_2");
        order.setStatus(OrderStatus.DELIVERED);
        order.setItems(List.of(oldItem));

        when(orderRepository.findById("ORDER-OLD")).thenReturn(Optional.of(order));
        when(productRepository.findById("sd-1")).thenReturn(Optional.of(donut));
        when(productOptionGroupRepository.findByProduct_IdAndActiveTrueOrderByDisplayOrderAscNameAsc("sd-1"))
                .thenReturn(List.of(flavor));
        when(cartItemRepository.findByUserIdOrderByIdAsc("u2")).thenReturn(List.of());

        CartService.ReorderResult result = service.reorderOrder("u2", "ORDER-OLD");

        assertEquals(0, result.copiedItemCount());
        assertEquals(1, result.requiresConfiguration().size());
        assertEquals("sd-1", result.requiresConfiguration().get(0).productId());
        assertEquals(2, result.requiresConfiguration().get(0).quantity());
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    private ProductOptionGroup requiredSingleGroup(Product product, String id, String name) {
        ProductOptionGroup group = new ProductOptionGroup();
        group.setId(id);
        group.setProduct(product);
        group.setName(name);
        group.setRequired(true);
        group.setMinSelections(1);
        group.setMaxSelections(1);
        group.setSelectionType(ProductOptionSelectionType.SINGLE);

        ProductOption option = new ProductOption();
        option.setId(id + "-original");
        option.setName("Truyền thống");
        group.addOption(option);
        return group;
    }

    private Product product(String id, String name, int price, String restaurant) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setPrice(price);
        product.setCategory("Món chính");
        product.setRestaurant(restaurant);
        product.setAvailable(true);
        return product;
    }
}
