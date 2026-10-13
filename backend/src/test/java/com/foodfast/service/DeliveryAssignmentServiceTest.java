package com.foodfast.service;

import com.foodfast.entity.DeliveryAssignment;
import com.foodfast.entity.DeliveryStatus;
import com.foodfast.entity.Order;
import com.foodfast.entity.OrderStatus;
import com.foodfast.entity.RestaurantCourier;
import com.foodfast.exception.ForbiddenOperationException;
import com.foodfast.repository.DeliveryAssignmentRepository;
import com.foodfast.repository.RestaurantCourierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryAssignmentServiceTest {

    @Mock
    private DeliveryAssignmentRepository assignmentRepository;

    @Mock
    private RestaurantCourierRepository courierRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private RealtimeService realtimeService;

    @Mock
    private CustomerNotificationService customerNotificationService;

    private DeliveryAssignmentService service;

    @BeforeEach
    void setUp() {
        service = new DeliveryAssignmentService(
                assignmentRepository,
                courierRepository,
                orderService,
                realtimeService,
                customerNotificationService
        );
    }

    @Test
    void onlyReadyOrderCanBeAssigned() {
        Order order = order("ORDER-1", "rest_2", OrderStatus.PREPARING);
        RestaurantCourier courier = courier("COURIER-1", "rest_2");
        when(orderService.findById("ORDER-1")).thenReturn(Optional.of(order));
        when(courierRepository.findById("COURIER-1")).thenReturn(Optional.of(courier));

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.assignDelivery("ORDER-1", "COURIER-1", null, null)
        );

        assertEquals("Chỉ đơn ở trạng thái Ready mới được phân công giao hàng", error.getMessage());
    }

    @Test
    void courierMustBelongToOrderRestaurant() {
        Order order = order("ORDER-1", "rest_2", OrderStatus.READY);
        RestaurantCourier courier = courier("COURIER-1", "restaurant_2");
        when(orderService.findById("ORDER-1")).thenReturn(Optional.of(order));
        when(courierRepository.findById("COURIER-1")).thenReturn(Optional.of(courier));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.assignDelivery("ORDER-1", "COURIER-1", null, null)
        );
    }

    @Test
    void legacyRestaurantAliasCanUseCourierFromCanonicalRestaurant() {
        Order order = order("ORDER-1", "sweetdreams", OrderStatus.READY);
        RestaurantCourier courier = courier("COURIER-1", "rest_2");
        when(orderService.findById("ORDER-1")).thenReturn(Optional.of(order));
        when(courierRepository.findById("COURIER-1")).thenReturn(Optional.of(courier));
        when(assignmentRepository.findFirstByOrderIdOrderByAssignedAtDesc("ORDER-1"))
                .thenReturn(Optional.empty());
        when(assignmentRepository.save(any(DeliveryAssignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryAssignment assignment = service.assignDelivery(
                "ORDER-1",
                "COURIER-1",
                System.currentTimeMillis() + 30 * 60 * 1000L,
                null
        );

        assertEquals("rest_2", assignment.getRestaurantId());
        assertEquals(DeliveryStatus.ASSIGNED, assignment.getStatus());
    }

    @Test
    void startingDeliveryMovesOrderToDelivering() {
        DeliveryAssignment assignment = assignment("DELIVERY-1", DeliveryStatus.ASSIGNED);
        when(assignmentRepository.findById("DELIVERY-1")).thenReturn(Optional.of(assignment));
        stubAssignmentSave();

        DeliveryAssignment updated = service.startDelivery("DELIVERY-1");

        assertEquals(DeliveryStatus.DELIVERING, updated.getStatus());
        verify(orderService).updateStatus(
                "ORDER-1", OrderStatus.DELIVERING, "rest_2",
                "RESTAURANT", "DELIVERY_PROCESS", "Nhân viên giao hàng bắt đầu giao"
        );
        verify(realtimeService).sendDeliveryUpdate(updated);
    }

    @Test
    void failedDeliveryReturnsOrderToReadyForReassignment() {
        DeliveryAssignment assignment = assignment("DELIVERY-1", DeliveryStatus.DELIVERING);
        when(assignmentRepository.findById("DELIVERY-1")).thenReturn(Optional.of(assignment));
        stubAssignmentSave();

        DeliveryAssignment updated = service.failDelivery("DELIVERY-1", "Khách không nghe máy");

        assertEquals(DeliveryStatus.FAILED, updated.getStatus());
        verify(orderService).updateStatus(
                "ORDER-1", OrderStatus.READY, "rest_2",
                "RESTAURANT", "DELIVERY_FAILED", "Khách không nghe máy"
        );
    }

    @Test
    void customerConfirmationMovesOrderToDelivered() {
        DeliveryAssignment assignment = assignment("DELIVERY-1", DeliveryStatus.DELIVERING);
        Order order = order("ORDER-1", "rest_2", OrderStatus.DELIVERING);
        order.setUserId("CUSTOMER-1");
        when(assignmentRepository.findById("DELIVERY-1")).thenReturn(Optional.of(assignment));
        when(orderService.findById("ORDER-1")).thenReturn(Optional.of(order));
        stubAssignmentSave();

        DeliveryAssignment updated = service.confirmReceivedByCustomer("DELIVERY-1", "CUSTOMER-1");

        assertEquals(DeliveryStatus.DELIVERED, updated.getStatus());
        assertEquals(com.foodfast.entity.DeliveryCompletionSource.CUSTOMER, updated.getCompletionSource());
        verify(orderService).updateStatus(
                "ORDER-1", OrderStatus.DELIVERED, "CUSTOMER-1",
                "CUSTOMER", "CUSTOMER_CONFIRMATION", "Khách xác nhận đã nhận hàng"
        );
    }

    @Test
    void anotherCustomerCannotConfirmReceived() {
        DeliveryAssignment assignment = assignment("DELIVERY-1", DeliveryStatus.DELIVERING);
        Order order = order("ORDER-1", "rest_2", OrderStatus.DELIVERING);
        order.setUserId("CUSTOMER-1");
        when(assignmentRepository.findById("DELIVERY-1")).thenReturn(Optional.of(assignment));
        when(orderService.findById("ORDER-1")).thenReturn(Optional.of(order));

        ForbiddenOperationException error = assertThrows(
                ForbiddenOperationException.class,
                () -> service.confirmReceivedByCustomer("DELIVERY-1", "CUSTOMER-2")
        );

        assertEquals("Chỉ khách hàng sở hữu đơn mới được xác nhận đã nhận hàng", error.getMessage());
    }

    @Test
    void restaurantOverrideIsBlockedDuringCustomerGracePeriod() {
        DeliveryAssignment assignment = assignment("DELIVERY-1", DeliveryStatus.DELIVERING);
        assignment.setEstimatedDeliveryAt(System.currentTimeMillis());
        when(assignmentRepository.findById("DELIVERY-1")).thenReturn(Optional.of(assignment));

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.completeByRestaurantOverride(
                        "rest_2",
                        "DELIVERY-1",
                        "Người giao báo khách đã nhận đủ món"
                )
        );

        assertEquals(
                "Chỉ được hoàn tất ngoại lệ sau thời gian giao dự kiến 60 phút",
                error.getMessage()
        );
    }

    @Test
    void restaurantCanCompleteWithReasonAfterGracePeriod() {
        DeliveryAssignment assignment = assignment("DELIVERY-1", DeliveryStatus.DELIVERING);
        assignment.setEstimatedDeliveryAt(
                System.currentTimeMillis()
                        - DeliveryAssignmentService.RESTAURANT_OVERRIDE_GRACE_MS
                        - 1_000
        );
        when(assignmentRepository.findById("DELIVERY-1")).thenReturn(Optional.of(assignment));
        stubAssignmentSave();

        DeliveryAssignment updated = service.completeByRestaurantOverride(
                "rest_2",
                "DELIVERY-1",
                "Người giao báo khách đã nhận đủ món"
        );

        assertEquals(DeliveryStatus.DELIVERED, updated.getStatus());
        assertEquals(
                com.foodfast.entity.DeliveryCompletionSource.RESTAURANT_OVERRIDE,
                updated.getCompletionSource()
        );
        verify(orderService).updateStatus(
                "ORDER-1", OrderStatus.DELIVERED, "rest_2",
                "RESTAURANT", "RESTAURANT_OVERRIDE", "Người giao báo khách đã nhận đủ món"
        );
    }

    private Order order(String id, String restaurantId, OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setRestaurantId(restaurantId);
        order.setStatus(status);
        return order;
    }

    private RestaurantCourier courier(String id, String restaurantId) {
        RestaurantCourier courier = new RestaurantCourier();
        courier.setId(id);
        courier.setRestaurantId(restaurantId);
        courier.setName("Nguyễn Văn Giao");
        courier.setPhone("0900000000");
        courier.setActive(true);
        return courier;
    }

    private DeliveryAssignment assignment(String id, DeliveryStatus status) {
        DeliveryAssignment assignment = new DeliveryAssignment();
        assignment.setId(id);
        assignment.setOrderId("ORDER-1");
        assignment.setRestaurantId("rest_2");
        assignment.setCourierId("COURIER-1");
        assignment.setStatus(status);
        return assignment;
    }

    private void stubAssignmentSave() {
        when(assignmentRepository.save(any(DeliveryAssignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }
}
