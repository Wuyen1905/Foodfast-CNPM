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
    private final DeliveryTaskService deliveryTaskService;

    public DeliveryAssignmentService(
            DeliveryAssignmentRepository assignmentRepository,
            RestaurantCourierRepository courierRepository,
            OrderService orderService,
            RealtimeService realtimeService,
            CustomerNotificationService customerNotificationService,
            DeliveryTaskService deliveryTaskService
    ) {
        this.assignmentRepository = assignmentRepository;
        this.courierRepository = courierRepository;
        this.orderService = orderService;
        this.realtimeService = realtimeService;
        this.customerNotificationService = customerNotificationService;
        this.deliveryTaskService = deliveryTaskService;
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
        // Khóa hồ sơ shipper để hai yêu cầu đồng thời không thể cùng giao hai đơn.
        RestaurantCourier courier = courierRepository.findByIdForUpdate(courierId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên giao hàng: " + courierId));

        if (order.getStatusEnum() != OrderStatus.READY) {
            throw new IllegalStateException("Chỉ đơn ở trạng thái Ready mới được phân công giao hàng");
        }
        if (!courier.isActive()) {
            throw new IllegalStateException("Nhân viên giao hàng đã ngừng hoạt động");
        }
        DeliveryAssignment task = deliveryTaskService.ensureWaitingTask(order);
        if (task == null) {
            throw new IllegalStateException("Không thể tạo nhiệm vụ giao hàng cho đơn này");
        }
        if (task.getStatus() == DeliveryStatus.DELIVERING) {
            throw new IllegalStateException("Đơn đang giao nên không thể đổi shipper");
        }
        boolean courierBusy = assignmentRepository.existsByCourierIdAndStatusIn(
                courierId, List.of(DeliveryStatus.ASSIGNED, DeliveryStatus.DELIVERING));
        if (courierBusy && !courierId.equals(task.getCourierId())) {
            throw new IllegalStateException("Shipper đang có một đơn hoạt động, vui lòng chọn người khác");
        }

        long now = System.currentTimeMillis();
        if (estimatedDeliveryAt != null && estimatedDeliveryAt <= now) {
            throw new IllegalArgumentException("Thời gian giao dự kiến phải ở tương lai");
        }

        task.assignTo(
                courierId,
                estimatedDeliveryAt != null ? estimatedDeliveryAt : now + 30 * 60 * 1000L,
                trimToNull(deliveryNote)
        );
        DeliveryAssignment saved = assignmentRepository.save(task);
        customerNotificationService.courierAssigned(
                order, courier.getName(), saved.getEstimatedDeliveryAt()
        );
        realtimeService.sendDeliveryUpdate(saved);
        return saved;
    }

    @Transactional
    public DeliveryAssignment startDelivery(String assignmentId, String adminId) {
        if (adminId == null || adminId.isBlank()) {
            throw new IllegalArgumentException("Thiếu mã Admin điều phối giao hàng");
        }
        DeliveryAssignment assignment = requireAssignment(assignmentId);
        assignment.start();
        DeliveryAssignment saved = assignmentRepository.save(assignment);
        orderService.updateStatus(
                saved.getOrderId(), OrderStatus.DELIVERING, adminId.trim(),
                "ADMIN", "ADMIN_DISPATCH", "Quản trị viên xác nhận shipper bắt đầu giao"
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
    public DeliveryAssignment failDelivery(String assignmentId, String reason, String adminId) {
        if (adminId == null || adminId.isBlank()) {
            throw new IllegalArgumentException("Thiếu mã Admin điều phối giao hàng");
        }
        DeliveryAssignment assignment = requireAssignment(assignmentId);
        assignment.fail(reason);
        DeliveryAssignment saved = assignmentRepository.save(assignment);
        orderService.updateStatus(
                saved.getOrderId(), OrderStatus.READY, adminId.trim(),
                "ADMIN", "DELIVERY_FAILED", reason
        );
        // Giữ failedAt/failureReason để truy vết nhưng trả chính nhiệm vụ này về hàng đợi.
        saved.reopenWaiting();
        saved = assignmentRepository.save(saved);
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

    @Transactional
    public List<DeliveryAssignment> findAdminQueue() {
        return deliveryTaskService.listQueue();
    }

    @Transactional(readOnly = true)
    public RestaurantCourier findCourier(String courierId) {
        if (courierId == null || courierId.isBlank()) return null;
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

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
