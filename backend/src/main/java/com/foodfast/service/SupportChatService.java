package com.foodfast.service;

import com.foodfast.dto.SupportChatRequest;
import com.foodfast.dto.SupportChatResponse;
import com.foodfast.entity.Order;
import com.foodfast.entity.DeliveryAssignment;
import com.foodfast.entity.DeliveryStatus;
import com.foodfast.entity.RestaurantCourier;
import com.foodfast.entity.SupportTicket;
import com.foodfast.entity.SupportTicketTarget;
import com.foodfast.service.SupportIntentClassifier.Intent;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.List;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class SupportChatService {

    private final OrderService orderService;
    private final SupportIntentClassifier intentClassifier;
    private final SupportTicketService ticketService;
    private final DeliveryAssignmentService deliveryService;

    private static final DateTimeFormatter VIETNAM_TIME = DateTimeFormatter
            .ofPattern("HH:mm 'ngày' dd/MM/yyyy")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    public SupportChatService(
            OrderService orderService,
            SupportIntentClassifier intentClassifier,
            SupportTicketService ticketService,
            DeliveryAssignmentService deliveryService
    ) {
        this.orderService = orderService;
        this.intentClassifier = intentClassifier;
        this.ticketService = ticketService;
        this.deliveryService = deliveryService;
    }

    public SupportChatResponse reply(
            SupportChatRequest request,
            String authenticatedCustomerId,
            String authenticatedCustomerName,
            String authenticatedCustomerPhone
    ) {
        Intent intent = intentClassifier.classify(request.message());

        return switch (intent) {
            case REFUND_OR_PAYMENT -> handoff(
                    "Mình không thể tự hoàn tiền hoặc sửa thanh toán. Mình sẽ hướng vấn đề này tới quản trị viên để xử lý an toàn.",
                    request, intent, SupportTicketTarget.ADMIN,
                    authenticatedCustomerId, authenticatedCustomerName, authenticatedCustomerPhone);
            case PAYMENT_HELP -> response(
                    "Bạn có thể chọn tiền mặt khi nhận hàng hoặc VNPay ở bước xác nhận đơn. Nếu phiên VNPay hết hạn hay thanh toán thất bại, hãy mở lại đơn và chọn “Thanh toán lại”; FoodFast chỉ ghi nhận thành công sau khi VNPay xác nhận hợp lệ.",
                    intent, null, request.orderId());
            case ACCOUNT -> handoff(
                    "Mình không tự thay đổi tài khoản hay mật khẩu. Quản trị viên sẽ hỗ trợ xác minh và xử lý cho bạn.",
                    request, intent, SupportTicketTarget.ADMIN,
                    authenticatedCustomerId, authenticatedCustomerName, authenticatedCustomerPhone);
            case ORDER_CHANGE -> handoff(
                    "Mình không tự hủy hoặc sửa đơn hàng. Mình sẽ hướng yêu cầu tới nhà hàng để nhân viên kiểm tra.",
                    request, intent, SupportTicketTarget.RESTAURANT,
                    authenticatedCustomerId, authenticatedCustomerName, authenticatedCustomerPhone);
            case FOOD_ISSUE -> handoff(
                    "Mình rất tiếc về vấn đề món ăn. Yêu cầu này cần nhà hàng kiểm tra trực tiếp, nên mình sẽ chuyển tới nhà hàng.",
                    request, intent, SupportTicketTarget.RESTAURANT,
                    authenticatedCustomerId, authenticatedCustomerName, authenticatedCustomerPhone);
            case DELIVERY_TIME -> answerDeliveryTime(request, intent, authenticatedCustomerId);
            case DELIVERY_FEE -> response(
                    "Phí giao hàng được hiển thị ở bước thanh toán trước khi bạn xác nhận đơn.",
                    intent, null, request.orderId());
            case ORDER_STATUS -> answerOrderStatus(request, intent, authenticatedCustomerId);
            case VOUCHER -> response(
                    "Các mã giảm giá còn hiệu lực được hiển thị ở bước xác nhận đơn. Hệ thống sẽ kiểm tra nhà hàng áp dụng, giá trị đơn tối thiểu, thời hạn và số lượt dùng trước khi trừ tiền.",
                    intent, null, request.orderId());
            case RESTAURANT_HOURS -> response(
                    "Giờ mở cửa và trạng thái đang nhận đơn được hiển thị ngay phía trên thực đơn. Khi nhà hàng đóng cửa hoặc tạm dừng nhận đơn, hệ thống sẽ chặn đặt hàng.",
                    intent, null, request.orderId());
            case REORDER -> response(
                    "Bạn mở “Đơn hàng của tôi”, chọn một đơn đã giao hoặc đã hủy rồi bấm “Đặt lại đơn”. Giá và tình trạng món sẽ được lấy lại theo hiện tại; món có topping bắt buộc cần được chọn lại.",
                    intent, null, request.orderId());
            case HUMAN_SUPPORT -> response(
                    "Bạn bấm “Liên hệ nhân viên hỗ trợ” bên dưới, chọn đúng nhóm vấn đề và đơn liên quan. Hệ thống sẽ chuyển phiếu tới nhà hàng hoặc bộ phận FoodFast phù hợp.",
                    intent, null, request.orderId());
            case GREETING -> response(
                    "Chào bạn! Mình có thể kiểm tra trạng thái và thời gian giao, hướng dẫn VNPay, mã giảm giá, đặt lại đơn hoặc giúp bạn liên hệ nhân viên hỗ trợ.",
                    intent, null, request.orderId());
            case GENERAL -> response(
                    "Mình chưa hiểu rõ câu hỏi. Bạn có thể hỏi về trạng thái đơn, thời gian giao, VNPay, mã giảm giá, đặt lại đơn hoặc chọn liên hệ nhân viên hỗ trợ.",
                    intent, null, request.orderId());
        };
    }

    private SupportChatResponse answerOrderStatus(
            SupportChatRequest request,
            Intent intent,
            String authenticatedCustomerId
    ) {
        if (request.orderId() == null || request.orderId().isBlank()) {
            return response("Bạn hãy chọn đơn cần tra cứu ở phía trên.", intent, null, null, true);
        }

        Optional<Order> orderOptional = orderService.findById(request.orderId().trim());
        if (orderOptional.isEmpty()) {
            return response("Mình chưa tìm thấy mã đơn này. Bạn hãy kiểm tra lại mã đơn.",
                    intent, null, request.orderId());
        }

        Order order = orderOptional.get();
        if (!isOrderOwner(order, authenticatedCustomerId)) {
            return response("Để bảo vệ thông tin đơn hàng, bạn cần đăng nhập bằng tài khoản đã đặt món.",
                    intent, null, request.orderId());
        }

        return response(buildOrderStatusReply(order), intent, null, order.getId());
    }

    private SupportChatResponse answerDeliveryTime(
            SupportChatRequest request,
            Intent intent,
            String authenticatedCustomerId
    ) {
        if (request.orderId() == null || request.orderId().isBlank()) {
            return response("Bạn hãy chọn đơn cần kiểm tra thời gian giao dự kiến.", intent, null, null, true);
        }

        Optional<Order> orderOptional = orderService.findById(request.orderId().trim());
        if (orderOptional.isEmpty()) {
            return response("Mình chưa tìm thấy mã đơn này. Bạn hãy kiểm tra lại mã đơn.",
                    intent, null, request.orderId());
        }

        Order order = orderOptional.get();
        if (!isOrderOwner(order, authenticatedCustomerId)) {
            return response("Để bảo vệ thông tin đơn hàng, bạn cần đăng nhập bằng tài khoản đã đặt món.",
                    intent, null, request.orderId());
        }

        Optional<DeliveryAssignment> deliveryOptional = deliveryService.findCurrentByOrderId(order.getId());
        if (deliveryOptional.isEmpty()) {
            return response(
                    "Đơn " + order.getId() + " hiện " + statusLabel(order)
                            + ", nhưng quản trị viên chưa phân công người giao nên chưa có thời gian giao dự kiến.",
                    intent, null, order.getId());
        }

        DeliveryAssignment delivery = deliveryOptional.get();
        if (delivery.getStatus() == DeliveryStatus.DELIVERED) {
            String deliveredAt = delivery.getDeliveredAt() == null
                    ? ""
                    : " lúc " + formatTime(delivery.getDeliveredAt());
            return response("Đơn " + order.getId() + " đã được giao thành công" + deliveredAt + ".",
                    intent, null, order.getId());
        }

        if (delivery.getStatus() == DeliveryStatus.FAILED) {
            return response("Lần giao gần nhất của đơn " + order.getId()
                            + " chưa thành công; quản trị viên đang cần phân công giao lại nên hiện chưa có thời gian giao dự kiến mới.",
                    intent, null, order.getId());
        }

        if (delivery.getEstimatedDeliveryAt() == null) {
            return response("Đơn " + order.getId() + " hiện " + statusLabel(order)
                            + ", nhưng quản trị viên chưa cập nhật thời gian giao dự kiến.",
                    intent, null, order.getId());
        }

        return response("Đơn " + order.getId() + " dự kiến tới lúc "
                        + formatTime(delivery.getEstimatedDeliveryAt()) + ". Người giao: "
                        + courierName(delivery.getCourierId()) + ".",
                intent, null, order.getId());
    }

    private SupportChatResponse handoff(
            String reply,
            SupportChatRequest request,
            Intent intent,
            SupportTicketTarget target,
            String authenticatedCustomerId,
            String authenticatedCustomerName,
            String authenticatedCustomerPhone
    ) {
        Order verifiedOrder = findVerifiedOrder(request, authenticatedCustomerId).orElse(null);
        if (target == SupportTicketTarget.RESTAURANT && verifiedOrder == null) {
            return response(
                    reply + " Bạn hãy chọn đúng đơn hàng để hệ thống xác định nhà hàng cần nhận yêu cầu.",
                    intent, null, request.orderId(), true
            );
        }

        SupportTicket ticket = ticketService.create(
                request,
                authenticatedCustomerId,
                authenticatedCustomerName,
                authenticatedCustomerPhone,
                intent.name(),
                target,
                verifiedOrder
        );
        return new SupportChatResponse(
                reply,
                "LOCAL_ASSISTANT",
                intent.name(),
                target.name(),
                verifiedOrder == null ? request.orderId() : verifiedOrder.getId(),
                ticket.getId(),
                List.of("Trạng thái đơn"),
                false
        );
    }

    private Optional<Order> findVerifiedOrder(
            SupportChatRequest request,
            String authenticatedCustomerId
    ) {
        if (request.orderId() == null || request.orderId().isBlank()) {
            return Optional.empty();
        }
        return orderService.findById(request.orderId().trim())
                .filter(order -> isOrderOwner(order, authenticatedCustomerId));
    }

    private boolean isOrderOwner(Order order, String authenticatedCustomerId) {
        return authenticatedCustomerId != null
                && authenticatedCustomerId.trim().equals(order.getUserId());
    }

    private String buildOrderStatusReply(Order order) {
        StringBuilder reply = new StringBuilder()
                .append("Đơn ")
                .append(order.getId())
                .append(" hiện ")
                .append(statusLabel(order));

        Optional<DeliveryAssignment> deliveryOptional = deliveryService
                .findCurrentByOrderId(order.getId());
        if (deliveryOptional.isPresent()) {
            appendDeliveryDetails(reply, deliveryOptional.get());
        } else {
            reply.append(".");
        }
        appendPaymentDetails(reply, order);
        return reply.toString();
    }

    private String statusLabel(Order order) {
        return switch (order.getStatusEnum()) {
            case PENDING -> "đang chờ nhà hàng xác nhận";
            case CONFIRMED -> "đã được nhà hàng xác nhận";
            case PREPARING -> "đang được chuẩn bị";
            case READY -> "đã sẵn sàng để giao";
            case DELIVERING -> "đang trên đường giao tới bạn";
            case DELIVERED -> "đã được giao thành công";
            case CANCELLED -> "đã bị hủy";
        };
    }

    private void appendDeliveryDetails(StringBuilder reply, DeliveryAssignment delivery) {
        String courierName = courierName(delivery.getCourierId());
        if (delivery.getStatus() == DeliveryStatus.ASSIGNED) {
            reply.append(". Quản trị viên đã phân công ").append(courierName);
            appendEta(reply, delivery.getEstimatedDeliveryAt());
            reply.append(".");
            return;
        }
        if (delivery.getStatus() == DeliveryStatus.DELIVERING) {
            reply.append(". Người giao là ").append(courierName);
            appendEta(reply, delivery.getEstimatedDeliveryAt());
            reply.append(".");
            return;
        }
        if (delivery.getStatus() == DeliveryStatus.DELIVERED) {
            if (delivery.getDeliveredAt() != null) {
                reply.append(" lúc ").append(formatTime(delivery.getDeliveredAt()));
            }
            reply.append(".");
            return;
        }
        if (delivery.getStatus() == DeliveryStatus.FAILED) {
            reply.append(". Lần giao gần nhất chưa thành công");
            if (delivery.getFailureReason() != null) {
                reply.append(" vì ").append(delivery.getFailureReason());
            }
            reply.append("; quản trị viên có thể phân công giao lại.");
            return;
        }
        reply.append(".");
    }

    private String courierName(String courierId) {
        try {
            RestaurantCourier courier = deliveryService.findCourier(courierId);
            return courier.getName();
        } catch (RuntimeException ignored) {
            return "nhân viên giao hàng";
        }
    }

    private void appendEta(StringBuilder reply, Long estimatedDeliveryAt) {
        if (estimatedDeliveryAt != null) {
            reply.append(", dự kiến tới lúc ").append(formatTime(estimatedDeliveryAt));
        }
    }

    private String formatTime(long timestamp) {
        return VIETNAM_TIME.format(Instant.ofEpochMilli(timestamp));
    }

    private void appendPaymentDetails(StringBuilder reply, Order order) {
        if (!PaymentStatusPolicy.isVNPay(order)) return;
        String paymentStatus = PaymentStatusPolicy.normalize(order.getPaymentStatus());
        String label = switch (paymentStatus) {
            case PaymentStatusPolicy.PAID -> "đã được VNPay xác nhận";
            case PaymentStatusPolicy.FAILED -> "thanh toán VNPay chưa thành công và có thể thanh toán lại";
            case PaymentStatusPolicy.EXPIRED -> "phiên VNPay đã hết hạn và có thể tạo lại giao dịch";
            case PaymentStatusPolicy.REFUND_PENDING, PaymentStatusPolicy.PARTIAL_REFUND_PENDING -> "đang chờ hoàn tiền";
            case PaymentStatusPolicy.REFUNDED -> "đã hoàn tiền";
            case PaymentStatusPolicy.PARTIAL_REFUNDED -> "đã hoàn một phần";
            default -> "đang chờ VNPay xác nhận";
        };
        reply.append(" Thanh toán: ").append(label).append(".");
    }

    private SupportChatResponse response(String reply, Intent intent, String handoffTarget, String orderId) {
        return response(reply, intent, handoffTarget, orderId, false);
    }

    private SupportChatResponse response(
            String reply,
            Intent intent,
            String handoffTarget,
            String orderId,
            boolean requiresOrder
    ) {
        return new SupportChatResponse(
                reply, "LOCAL_ASSISTANT", intent.name(), handoffTarget, orderId, null,
                suggestionsFor(intent), requiresOrder
        );
    }

    private List<String> suggestionsFor(Intent intent) {
        return switch (intent) {
            case ORDER_STATUS -> List.of("Khi nào giao tới?", "Liên hệ nhân viên hỗ trợ");
            case DELIVERY_TIME -> List.of("Trạng thái đơn", "Phí giao hàng");
            case PAYMENT_HELP -> List.of("Phiên VNPay hết hạn", "Tôi bị trừ tiền", "Liên hệ nhân viên hỗ trợ");
            case VOUCHER -> List.of("Mã giảm giá dùng ở đâu?", "Phí giao hàng");
            case REORDER -> List.of("Trạng thái đơn", "Mã giảm giá");
            case GREETING, GENERAL -> List.of("Trạng thái đơn", "Thanh toán VNPay", "Mã giảm giá");
            default -> List.of();
        };
    }

}
