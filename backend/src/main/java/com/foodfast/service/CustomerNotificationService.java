package com.foodfast.service;

import com.foodfast.entity.CustomerNotification;
import com.foodfast.entity.Order;
import com.foodfast.entity.OrderStatus;
import com.foodfast.exception.ForbiddenOperationException;
import com.foodfast.repository.CustomerNotificationRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
public class CustomerNotificationService {

    private final CustomerNotificationRepository repository;
    private final SimpMessagingTemplate messagingTemplate;

    public CustomerNotificationService(
            CustomerNotificationRepository repository,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.repository = repository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public CustomerNotification orderCreated(Order order) {
        String message = PaymentStatusPolicy.isVNPay(order)
                ? "Đơn đã được tạo. Hãy hoàn tất thanh toán VNPay để nhà hàng tiếp nhận."
                : "Đơn đã được gửi tới nhà hàng và đang chờ xác nhận.";
        return create(order, "ORDER_CREATED", "Đặt hàng thành công", message);
    }

    @Transactional
    public CustomerNotification orderStatusChanged(Order order, OrderStatus fromStatus) {
        String type = "ORDER_" + order.getStatusEnum().name();
        String title;
        String message;
        switch (order.getStatusEnum()) {
            case CONFIRMED -> { title = "Nhà hàng đã xác nhận"; message = "Đơn đang được chuyển tới bếp."; }
            case PREPARING -> { title = "Đang chuẩn bị món"; message = "Nhà hàng đang chuẩn bị đơn của bạn."; }
            case READY -> {
                title = fromStatus == OrderStatus.DELIVERING ? "Giao hàng chưa thành công" : "Đơn đã sẵn sàng";
                message = fromStatus == OrderStatus.DELIVERING
                        ? "Nhà hàng sẽ kiểm tra và phân công giao lại đơn."
                        : "Đơn đã sẵn sàng để phân công người giao.";
            }
            case DELIVERING -> { title = "Đơn đang được giao"; message = "Người giao hàng đang mang đơn tới bạn."; }
            case DELIVERED -> { title = "Đơn đã hoàn tất"; message = "Cảm ơn bạn đã sử dụng FoodFast."; }
            case CANCELLED -> { title = "Đơn đã bị hủy"; message = cancellationMessage(order); }
            default -> { title = "Đơn hàng được cập nhật"; message = "Trạng thái mới: " + order.getStatusString() + "."; }
        }
        return create(order, type, title, message);
    }

    @Transactional
    public CustomerNotification courierAssigned(Order order, String courierName, Long estimatedDeliveryAt) {
        String eta = estimatedDeliveryAt == null ? "" : " Thời gian dự kiến đã được cập nhật trong chi tiết đơn.";
        return create(order, "DELIVERY_ASSIGNED", "Đã phân công người giao",
                (StringUtils.hasText(courierName) ? courierName.trim() : "Nhân viên giao hàng")
                        + " sẽ giao đơn của bạn." + eta);
    }

    @Transactional
    public CustomerNotification paymentSucceeded(Order order) {
        return create(order, "PAYMENT_SUCCEEDED", "Thanh toán VNPay thành công",
                "Nhà hàng đã nhận được yêu cầu và sẽ sớm xác nhận đơn.");
    }

    @Transactional
    public CustomerNotification paymentFailed(Order order) {
        return create(order, "PAYMENT_FAILED", "Thanh toán chưa thành công",
                "Bạn có thể tạo lại phiên thanh toán trong chi tiết đơn hàng.");
    }

    @Transactional(readOnly = true)
    public List<CustomerNotification> mine(String userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public long unreadCount(String userId) {
        return repository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public CustomerNotification markRead(String userId, String id) {
        CustomerNotification notification = requireOwned(userId, id);
        notification.setRead(true);
        return repository.save(notification);
    }

    @Transactional
    public int markAllRead(String userId) {
        List<CustomerNotification> notifications = repository.findByUserIdOrderByCreatedAtDesc(userId);
        List<CustomerNotification> unread = notifications.stream().filter(item -> !item.isRead()).toList();
        unread.forEach(item -> item.setRead(true));
        repository.saveAll(unread);
        return unread.size();
    }

    private CustomerNotification create(Order order, String type, String title, String message) {
        if (order == null || !StringUtils.hasText(order.getUserId())) return null;
        CustomerNotification notification = new CustomerNotification();
        notification.setId(UUID.randomUUID().toString());
        notification.setUserId(order.getUserId().trim());
        notification.setOrderId(order.getId());
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setActionUrl("/orders/" + order.getId());
        notification.setRead(false);
        notification.setCreatedAt(System.currentTimeMillis());
        CustomerNotification saved = repository.save(notification);
        messagingTemplate.convertAndSendToUser(order.getUserId(), "/queue/notifications", saved);
        return saved;
    }

    private CustomerNotification requireOwned(String userId, String id) {
        CustomerNotification notification = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông báo"));
        if (!notification.getUserId().equals(userId)) {
            throw new ForbiddenOperationException("Bạn không được truy cập thông báo này");
        }
        return notification;
    }

    private String cancellationMessage(Order order) {
        String detail = order.getCancellationReasonDetail();
        return StringUtils.hasText(detail)
                ? "Lý do: " + detail.trim()
                : "Mở chi tiết đơn để xem lý do và trạng thái hoàn tiền (nếu có).";
    }
}
