package com.foodfast.service;

import com.foodfast.dto.CreateOrderRequest;
import com.foodfast.dto.OrderQuoteResponse;
import com.foodfast.entity.Order;
import com.foodfast.entity.Product;
import com.foodfast.event.OrderEventPublisher;
import com.foodfast.repository.OrderRepository;
import com.foodfast.repository.ProductOptionGroupRepository;
import com.foodfast.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderRestaurantValidationTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    @Mock
    private NotificationService notificationService;

    @Mock
    private RestaurantService restaurantService;

    @Mock
    private ProductOptionGroupRepository productOptionGroupRepository;

    @Mock
    private VoucherService voucherService;

    @Mock
    private OrderStatusHistoryService orderStatusHistoryService;

    @Mock
    private CustomerNotificationService customerNotificationService;

    @Mock
    private DeliveryTaskService deliveryTaskService;

    @Mock
    private OrderGroupPaymentService orderGroupPaymentService;

    private OrderService service;

    @BeforeEach
    void setUp() {
        lenient().when(productOptionGroupRepository.findByProduct_IdAndActiveTrueOrderByDisplayOrderAscNameAsc(any()))
                .thenReturn(List.of());
        service = new OrderService(
                orderRepository,
                new ProductAvailabilityService(productRepository),
                orderEventPublisher,
                notificationService,
                restaurantService,
                new ProductOptionPricingService(productOptionGroupRepository),
                voucherService,
                orderStatusHistoryService,
                customerNotificationService,
                deliveryTaskService,
                orderGroupPaymentService
        );
        lenient().when(voucherService.applyAndReserve(any(), any(), any(), any(Integer.class), any(), any()))
                .thenReturn(VoucherService.VoucherApplication.none());
        lenient().when(voucherService.preview(any(), any(), any(), any(Integer.class), any()))
                .thenReturn(VoucherService.VoucherApplication.none());
    }

    @Test
    void rejectsOrderContainingProductsFromDifferentRestaurants() {
        when(productRepository.findByIdForUpdate("ak-2"))
                .thenReturn(Optional.of(product("ak-2", "Pizza Hawaii", 89_000, "Aloha")));
        when(productRepository.findByIdForUpdate("sd-1"))
                .thenReturn(Optional.of(product("sd-1", "Bánh Donut", 25_000, "SweetDreams")));
        CreateOrderRequest request = request(item("ak-2"), item("sd-1"));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.createOrderFromRequest(request)
        );

        assertEquals("Mỗi đơn hàng chỉ được chứa món của một nhà hàng", error.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void derivesRestaurantNameAndServerPricesForValidOrder() {
        when(productRepository.findByIdForUpdate("ak-2"))
                .thenReturn(Optional.of(product("ak-2", "Pizza Hawaii", 89_000, "Aloha")));
        when(productRepository.findByIdForUpdate("ak-1"))
                .thenReturn(Optional.of(product("ak-1", "Hamburger", 79_000, "Aloha")));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        CreateOrderRequest request = request(item("ak-2"), item("ak-1"));
        request.restaurantId = "aloha";

        Order saved = service.createOrderFromRequest(request);

        assertEquals("restaurant_2", saved.getRestaurantId());
        assertEquals("Aloha", saved.getRestaurant());
        assertEquals(2, saved.getItems().size());
        assertEquals(89_000, saved.getItems().get(0).getPrice());
        assertEquals("https://cdn.example/ak-2.jpg", saved.getItems().get(0).getProductImage());
        assertEquals(183_000, saved.getTotal());
    }

    @Test
    void quoteIgnoresBrowserNameAndPriceAndUsesServerAmounts() {
        when(productRepository.findById("ak-2"))
                .thenReturn(Optional.of(product("ak-2", "Pizza Hawaii", 89_000, "Aloha")));
        CreateOrderRequest request = request(item("ak-2"));
        request.items.get(0).name = "Giá giả từ trình duyệt";
        request.items.get(0).price = 1;

        OrderQuoteResponse quote = service.quote(request);

        assertEquals("Pizza Hawaii", quote.items().get(0).name());
        assertEquals(89_000, quote.items().get(0).unitPrice());
        assertEquals(89_000, quote.subtotal());
        assertEquals(15_000, quote.shippingFee());
        assertEquals(0, quote.taxAmount());
        assertEquals(104_000, quote.total());
        verify(orderRepository, never()).save(any(Order.class));
    }

    private CreateOrderRequest request(CreateOrderRequest.Item... items) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.customerName = "Khách hàng mẫu";
        request.customerPhone = "0123456789";
        request.address = "123 Nguyễn Huệ";
        request.userId = "u2";
        request.paymentMethod = "cod";
        request.items = List.of(items);
        return request;
    }

    private CreateOrderRequest.Item item(String productId) {
        CreateOrderRequest.Item item = new CreateOrderRequest.Item();
        item.productId = productId;
        item.quantity = 1;
        item.price = 1;
        item.name = "Dữ liệu không đáng tin từ trình duyệt";
        return item;
    }

    private Product product(String id, String name, int price, String restaurant) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setPrice(price);
        product.setCategory("Món chính");
        product.setRestaurant(restaurant);
        product.setImageUrl("https://cdn.example/" + id + ".jpg");
        product.setAvailable(true);
        return product;
    }
}
