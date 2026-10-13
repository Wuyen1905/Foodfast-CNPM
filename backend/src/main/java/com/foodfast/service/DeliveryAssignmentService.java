package com.foodfast.service;

import com.foodfast.entity.DeliveryAssignment;
import com.foodfast.entity.DeliveryCompletionSource;
import com.foodfast.entity.DeliveryStatus;
import com.foodfast.entity.Order;
import com.foodfast.entity.OrderStatus;
import com.foodfast.entity.RestaurantCourier;
import com.foodfast.exception.ForbiddenOperationException;
import com.foodfast.repository.DeliveryAssignmentRepository;
import com.foodfast.repository.RestaurantCourierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DeliveryAssignmentService {

    public static final long RESTAURANT_OVERRIDE_GRACE_MS = 60 * 60 * 1000L;

    private final DeliveryAssignmentRepository assignmentRepository;
    private final RestaurantCourierRepository courierRepository;
    private final OrderService orderService;
    private final RealtimeService realtimeService;
    private final CustomerNotificationService customerNotificationService;

    public DeliveryAssignmentService(
            DeliveryAssignmentRepository assignmentRepository,
            RestaurantCourierRepository courierRepository,
            OrderService orderService,
            RealtimeService realtimeService,
            CustomerNotificationService customerNotificationService
    ) {
        this.assignmentRepository = assignmentRepository;
        this.courierRepository = courierRepository;
        this.orderService = orderService;
        this.realtimeService = realtimeService;
        this.customerNotificationService = customerNotificationService;
    }

    @Transactional
    public DeliveryAssignment assignDelivery(
            String orderId,
            String courierId,
            Long estimatedDeliveryAt,
            String deliveryNote
    ) {
        Order order = orderService.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng: " + orderId));
        RestaurantCourier courier = courierRepository.findById(courierId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên giao hàng: " + courierId));

        if (order.getStatusEnum() != OrderStatus.READY) {
            throw new IllegalStateException("Chỉ đơn ở trạng thái Ready mới được phân công giao hàng");
        }
        if (!courier.isActive()) {
            throw new IllegalStateException("Nhân viên giao hàng đã ngừng hoạt động");
        }
        if (!RestaurantIdentityPolicy.refersToSameRestaurant(
                order.getRestaurantId(),
                courier.getRestaurantId()
        )) {
            throw new IllegalArgumentException("Nhân viên giao hàng không thuộc nhà hàng của đơn");
        }

        Optional<DeliveryAssignment> latest = assignmentRepository
                .findFirstByOrderIdOrderByAssignedAtDesc(orderId);
        if (latest.isPresent() && isActive(latest.get().getStatus())) {
            throw new IllegalStateException("Đơn hàng đã có phân công giao hàng đang hoạt động");
        }

        long now = System.currentTimeMillis();
        if (estimatedDeliveryAt != null && estimatedDeliveryAt <= now) {
            throw new IllegalArgumentException("Thời gian giao dự kiến phải ở tương lai");
        }

        DeliveryAssignment assignment = new DeliveryAssignment();
        assignment.setOrderId(orderId);
        assignment.setRestaurantId(RestaurantIdentityPolicy.canonicalize(order.getRestaurantId()));
        assignment.setCourierId(courierId);
        assignment.setStatus(DeliveryStatus.ASSIGNED);
        assignment.setEstimatedDeliveryAt(
                estimatedDeliveryAt != null ? estimatedDeliveryAt : now + 30 * 60 * 1000L
        );
        assignment.setDeliveryNote(trimToNull(deliveryNote));

        DeliveryAssignment saved = assignmentRepository.save(assignment);
        customerNotificationService.courierAssigned(
                order, courier.getName(), saved.getEstimatedDeliveryAt()
        );
        realtimeService.sendDeliveryUpdate(saved);
        return saved;
    }

    @Transactional
    public DeliveryAssignment startDelivery(String assignmentId) {
        DeliveryAssignment assignment = requireAssignment(assignmentId);
        assignment.start();
        DeliveryAssignment saved = assignmentRepository.save(assignment);
        orderService.updateStatus(
                saved.getOrderId(), OrderStatus.DELIVERING, saved.getRestaurantId(),
                "RESTAURANT", "DELIVERY_PROCESS", "Nhân viên giao hàng bắt đầu giao"
        );
        realtimeService.sendDeliveryUpdate(saved);
        return saved;
    }

    @Transactional
    public DeliveryAssignment confirmReceivedByCustomer(String assignmentId, String customerId) {
        DeliveryAssignment assignment = requireAssignment(assignmentId);
        Order order = orderService.findById(assignment.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy đơn hàng: " + assignment.getOrderId()
                ));
        if (order.getUserId() == null || !order.getUserId().equals(customerId)) {
            throw new ForbiddenOperationException(
                    "Chỉ khách hàng sở hữu đơn mới được xác nhận đã nhận hàng"
            );
        }

        assignment.complete(DeliveryCompletionSource.CUSTOMER, null);
        DeliveryAssignment saved = assignmentRepository.save(assignment);
        orderService.updateStatus(
                saved.getOrderId(), OrderStatus.DELIVERED, customerId,
                "CUSTOMER", "CUSTOMER_CONFIRMATION", "Khách xác nhận đã nhận hàng"
        );
        realtimeService.sendDeliveryUpdate(saved);
        return saved;
    }

    @Transactional
    public DeliveryAssignment completeByRestaurantOverride(
            String restaurantId,
            String assignmentId,
            String reason
    ) {
        DeliveryAssignment assignment = requireAssignment(assignmentId);
        if (!RestaurantIdentityPolicy.refersToSameRestaurant(
                assignment.getRestaurantId(),
                restaurantId
        )) {
            throw new ForbiddenOperationException(
                    "Nhà hàng không có quyền xử lý phân công giao hàng này"
            );
        }

        Long estimatedDeliveryAt = assignment.getEstimatedDeliveryAt();
        long availableAt = estimatedDeliveryAt == null
                ? Long.MAX_VALUE
                : estimatedDeliveryAt + RESTAURANT_OVERRIDE_GRACE_MS;
        if (System.currentTimeMillis() < availableAt) {
            throw new IllegalStateException(
                    "Chỉ được hoàn tất ngoại lệ sau thời gian giao dự kiến 60 phút"
            );
        }

        assignment.complete(DeliveryCompletionSource.RESTAURANT_OVERRIDE, reason);
        DeliveryAssignment saved = assignmentRepository.save(assignment);
        orderService.updateStatus(
                saved.getOrderId(), OrderStatus.DELIVERED, restaurantId,
                "RESTAURANT", "RESTAURANT_OVERRIDE", reason
        );
        realtimeService.sendDeliveryUpdate(saved);
        return saved;
    }

    @Transactional
    public DeliveryAssignment failDelivery(String assignmentId, String reason) {
        DeliveryAssignment assignment = requireAssignment(assignmentId);
        assignment.fail(reason);
        DeliveryAssignment saved = assignmentRepository.save(assignment);
        orderService.updateStatus(
                saved.getOrderId(), OrderStatus.READY, saved.getRestaurantId(),
                "RESTAURANT", "DELIVERY_FAILED", reason
        );
        realtimeService.sendDeliveryUpdate(saved);
        return saved;
    }

    @Transactional
    public DeliveryAssignment cancelDelivery(String assignmentId) {
        DeliveryAssignment assignment = requireAssignment(assignmentId);
        assignment.cancel();
        DeliveryAssignment saved = assignmentRepository.save(assignment);
        realtimeService.sendDeliveryUpdate(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<DeliveryAssignment> findCurrentByOrderId(String orderId) {
        return assignmentRepository.findFirstByOrderIdOrderByAssignedAtDesc(orderId);
    }

    @Transactional(readOnly = true)
    public List<DeliveryAssignment> findByRestaurant(String restaurantId) {
        return assignmentRepository.findByRestaurantIdOrderByAssignedAtDesc(restaurantId);
    }

    @Transactional(readOnly = true)
    public RestaurantCourier findCourier(String courierId) {
        return courierRepository.findById(courierId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên giao hàng: " + courierId));
    }

    @Transactional(readOnly = true)
    public DeliveryAssignment findById(String assignmentId) {
        return requireAssignment(assignmentId);
    }

    private DeliveryAssignment requireAssignment(String assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phân công giao hàng: " + assignmentId));
    }

    private boolean isActive(DeliveryStatus status) {
        return status == DeliveryStatus.ASSIGNED || status == DeliveryStatus.DELIVERING;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
