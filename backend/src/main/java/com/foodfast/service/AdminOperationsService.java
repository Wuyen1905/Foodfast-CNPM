package com.foodfast.service;

import com.foodfast.dto.AdminRestaurantStatusRequest;
import com.foodfast.dto.AdminSupportTicketUpdateRequest;
import com.foodfast.dto.SupportTicketResponse;
import com.foodfast.entity.*;
import com.foodfast.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
public class AdminOperationsService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final DeliveryAssignmentRepository deliveryRepository;
    private final ComplaintRepository complaintRepository;
    private final ComplaintHistoryRepository complaintHistoryRepository;
    private final DeliveryAddressRepository addressRepository;
    private final SupportTicketRepository ticketRepository;
    private final SupportTicketHistoryRepository ticketHistoryRepository;
    private final AdminAuditLogService auditLogService;

    public AdminOperationsService(
            RestaurantRepository restaurantRepository,
            UserRepository userRepository,
            OrderRepository orderRepository,
            DeliveryAssignmentRepository deliveryRepository,
            ComplaintRepository complaintRepository,
            ComplaintHistoryRepository complaintHistoryRepository,
            DeliveryAddressRepository addressRepository,
            SupportTicketRepository ticketRepository,
            SupportTicketHistoryRepository ticketHistoryRepository,
            AdminAuditLogService auditLogService
    ) {
        this.restaurantRepository = restaurantRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.deliveryRepository = deliveryRepository;
        this.complaintRepository = complaintRepository;
        this.complaintHistoryRepository = complaintHistoryRepository;
        this.addressRepository = addressRepository;
        this.ticketRepository = ticketRepository;
        this.ticketHistoryRepository = ticketHistoryRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> overview() {
        List<Order> orders = orderRepository.findAll();
        List<Restaurant> restaurants = restaurantRepository.findAll();
        List<DeliveryAssignment> deliveries = deliveryRepository.findAll();
        List<Complaint> complaints = complaintRepository.findAll();
        List<SupportTicket> tickets = ticketRepository.findAll();

        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        Instant todayStart = today.atStartOfDay(BUSINESS_ZONE).toInstant();
        Instant weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay(BUSINESS_ZONE).toInstant();
        Instant monthStart = today.withDayOfMonth(1).atStartOfDay(BUSINESS_ZONE).toInstant();
        long now = System.currentTimeMillis();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ordersToday", countOrdersSince(orders, todayStart));
        response.put("revenueToday", revenueSince(orders, todayStart));
        response.put("revenueWeek", revenueSince(orders, weekStart));
        response.put("revenueMonth", revenueSince(orders, monthStart));
        response.put("failedDeliveries", deliveries.stream()
                .filter(item -> item.getStatus() == DeliveryStatus.FAILED).count());
        response.put("failedDeliveryOrderIds", deliveries.stream()
                .filter(item -> item.getStatus() == DeliveryStatus.FAILED)
                .map(DeliveryAssignment::getOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        response.put("paymentReconciliationNeeded", orders.stream()
                .filter(this::needsPaymentReconciliation).count());
        response.put("reconciliationOrderIds", orders.stream()
                .filter(this::needsPaymentReconciliation)
                .map(Order::getId)
                .toList());
        response.put("openComplaints", complaints.stream()
                .filter(item -> item.getStatus() == ComplaintStatus.OPEN
                        || item.getStatus() == ComplaintStatus.REVIEWING).count());
        List<Order> pendingRefundOrders = orders.stream()
                .filter(item -> PaymentStatusPolicy.REFUND_PENDING.equals(
                                PaymentStatusPolicy.normalize(item.getPaymentStatus()))
                        || PaymentStatusPolicy.PARTIAL_REFUND_PENDING.equals(
                                PaymentStatusPolicy.normalize(item.getPaymentStatus())))
                .toList();
        response.put("pendingRefunds", pendingRefundOrders.size());
        response.put("pendingRefundOrderIds", pendingRefundOrders.stream().map(Order::getId).toList());
        response.put("pendingRestaurants", restaurants.stream()
                .filter(item -> item.getAdminStatus() == RestaurantLifecycleStatus.PENDING).count());
        response.put("openSupportTickets", tickets.stream()
                .filter(item -> item.getTarget() == SupportTicketTarget.ADMIN)
                .filter(item -> item.getStatus() != SupportTicketStatus.RESOLVED).count());
        long overdueTickets = tickets.stream()
                .filter(item -> item.getTarget() == SupportTicketTarget.ADMIN)
                .filter(item -> SupportDeadlinePolicy.isTicketOverdue(item, now)).count();
        long overdueComplaints = complaints.stream()
                .filter(item -> SupportDeadlinePolicy.isComplaintOverdue(item, now)).count();
        response.put("overdueSupportTickets", overdueTickets + overdueComplaints);
        return response;
    }

    @Transactional
    public Restaurant updateRestaurantLifecycle(
            String restaurantId,
            AdminRestaurantStatusRequest request,
            User admin
    ) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhà hàng"));
        RestaurantLifecycleStatus before = restaurant.getAdminStatus();
        RestaurantLifecycleStatus target = request.status();
        if (before == target) throw new IllegalStateException("Nhà hàng đã ở trạng thái này");
        if (!allowedRestaurantTransition(before, target)) {
            throw new IllegalStateException("Không thể chuyển nhà hàng từ " + before + " sang " + target);
        }

        restaurant.setAdminStatus(target);
        restaurant.setAdminStatusReason(request.reason().trim());
        restaurant.setAdminStatusUpdatedAt(System.currentTimeMillis());
        if (target != RestaurantLifecycleStatus.ACTIVE) restaurant.setAcceptingOrders(false);
        Restaurant saved = restaurantRepository.save(restaurant);

        String action = switch (target) {
            case ACTIVE -> before == RestaurantLifecycleStatus.PENDING
                    ? "restaurant_approved" : "restaurant_activated";
            case SUSPENDED -> "restaurant_suspended";
            case CLOSED -> "restaurant_closed";
            case PENDING -> "restaurant_reopened_for_review";
        };
        auditLogService.record(admin, action, "restaurant", saved.getId(), saved.getName(),
                before + " → " + target + ": " + request.reason().trim(),
                target == RestaurantLifecycleStatus.ACTIVE ? "info" : "warning");
        return saved;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> restaurantDetails(String restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhà hàng"));
        List<Order> orders = orderRepository.findAll().stream()
                .filter(order -> restaurantId.equals(order.getRestaurantId())
                        || restaurantId.equals(order.getRestaurant()))
                .sorted(Comparator.comparing(this::orderCreatedAt).reversed())
                .toList();
        List<Complaint> complaints = complaintRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId);
        long cancelled = orders.stream().filter(order -> order.getStatusEnum() == OrderStatus.CANCELLED).count();
        long delivered = orders.stream().filter(order -> order.getStatusEnum() == OrderStatus.DELIVERED).count();
        long failed = deliveryRepository.findAll().stream()
                .filter(item -> restaurantId.equals(item.getRestaurantId()))
                .filter(item -> item.getStatus() == DeliveryStatus.FAILED).count();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", restaurant.getId());
        response.put("name", restaurant.getName());
        response.put("category", restaurant.getCategory());
        response.put("location", restaurant.getLocation());
        response.put("status", restaurant.getAdminStatus());
        response.put("statusReason", restaurant.getAdminStatusReason());
        response.put("statusUpdatedAt", restaurant.getAdminStatusUpdatedAt());
        response.put("rating", restaurant.getRating());
        response.put("totalOrders", orders.size());
        response.put("deliveredOrders", delivered);
        response.put("cancelledOrders", cancelled);
        response.put("failedDeliveries", failed);
        response.put("totalRevenue", orders.stream().mapToLong(RevenuePolicy::netRevenue).sum());
        response.put("cancellationRate", orders.isEmpty() ? 0D : cancelled * 100D / orders.size());
        response.put("complaintCount", complaints.size());
        response.put("openComplaintCount", complaints.stream()
                .filter(item -> item.getStatus() == ComplaintStatus.OPEN
                        || item.getStatus() == ComplaintStatus.REVIEWING).count());
        response.put("recentOrders", orders.stream().limit(10).map(this::orderSummary).toList());
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> customerDetails(String customerId) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng"));
        if (!"customer".equalsIgnoreCase(customer.getRole())
                && !"suspended".equalsIgnoreCase(customer.getRole())) {
            throw new IllegalArgumentException("Tài khoản không phải khách hàng");
        }
        List<Order> orders = orderRepository.findByUserId(customerId).stream()
                .sorted(Comparator.comparing(this::orderCreatedAt).reversed()).toList();
        List<Complaint> complaints = complaintRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        List<DeliveryAddress> addresses = addressRepository.findByUserIdOrderByDefaultAddressDescUpdatedAtDesc(customerId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", customer.getId());
        response.put("name", customer.getName());
        response.put("username", customer.getUsername());
        response.put("email", customer.getEmail());
        response.put("phone", customer.getPhone());
        response.put("accountStatus", customer.isAccountActive() ? "Active" : "Suspended");
        response.put("createdAt", customer.getCreatedAt());
        response.put("lastLoginAt", customer.getLastLoginAt());
        response.put("suspensionReason", customer.getSuspensionReason());
        response.put("accountStatusUpdatedAt", customer.getAccountStatusUpdatedAt());
        response.put("totalOrders", orders.size());
        response.put("totalSpend", orders.stream().mapToLong(RevenuePolicy::netRevenue).sum());
        response.put("orders", orders.stream().map(this::orderSummary).toList());
        response.put("payments", orders.stream().map(this::paymentSummary).toList());
        response.put("complaints", complaints.stream().map(this::complaintSummary).toList());
        response.put("addresses", addresses.stream().map(this::addressSummary).toList());
        return response;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> supportQueue() {
        List<Map<String, Object>> queue = new ArrayList<>();
        ticketRepository.findAll().stream()
                .filter(ticket -> ticket.getTarget() == SupportTicketTarget.ADMIN)
                .forEach(ticket -> queue.add(ticketQueueItem(ticket)));
        complaintRepository.findAllByOrderByCreatedAtDesc().forEach(item -> queue.add(complaintQueueItem(item)));
        queue.sort(Comparator
                .comparing((Map<String, Object> item) -> !Boolean.TRUE.equals(item.get("overdue")))
                .thenComparing(item -> -priorityWeight(String.valueOf(item.get("priority"))))
                .thenComparingLong(item -> ((Number) item.get("createdAt")).longValue()));
        return queue;
    }

    @Transactional
    public SupportTicketResponse updateSupportTicket(
            String ticketId,
            AdminSupportTicketUpdateRequest request,
            User admin
    ) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu hỗ trợ"));
        if (ticket.getTarget() != SupportTicketTarget.ADMIN) {
            throw new IllegalStateException("Phiếu này thuộc trách nhiệm của nhà hàng");
        }
        if (ticket.getStatus() == SupportTicketStatus.RESOLVED) {
            throw new IllegalStateException("Phiếu hỗ trợ đã hoàn tất");
        }
        boolean hasChange = request.status() != null || request.priority() != null
                || request.department() != null || Boolean.TRUE.equals(request.assignToMe());
        if (!hasChange) throw new IllegalArgumentException("Không có thay đổi nào để lưu");

        SupportTicketStatus before = ticket.getStatus();
        if (Boolean.TRUE.equals(request.assignToMe())) {
            ticket.setAssignedAdminId(admin.getId());
            ticket.setAssignedAdminName(admin.getName());
            ticket.startProgress();
        }
        if (request.priority() != null && request.priority() != ticket.getPriority()) {
            ticket.setPriority(request.priority());
            ticket.recalculateResponseDeadline();
        }
        if (request.department() != null && request.department() != ticket.getDepartment()) {
            ticket.setDepartment(request.department());
            ticket.recalculateResponseDeadline();
        }
        if (request.status() != null) {
            if (request.status() == SupportTicketStatus.OPEN && before != SupportTicketStatus.OPEN) {
                throw new IllegalStateException("Không thể đưa phiếu đang xử lý về trạng thái mới");
            }
            if (request.status() == SupportTicketStatus.IN_PROGRESS) ticket.startProgress();
            if (request.status() == SupportTicketStatus.RESOLVED) {
                if (!StringUtils.hasText(request.note()) || request.note().trim().length() < 5) {
                    throw new IllegalArgumentException("Kết quả xử lý phải có ít nhất 5 ký tự");
                }
                ticket.resolve(request.note().trim());
            }
        }

        SupportTicket saved = ticketRepository.save(ticket);
        appendTicketHistory(saved, "ADMIN_UPDATED", admin, before, saved.getStatus(), request.note());
        auditLogService.record(admin, "support_ticket_updated", "support_ticket", saved.getId(), saved.getId(),
                supportAuditDetails(saved, request.note()),
                saved.getStatus() == SupportTicketStatus.RESOLVED ? "info" : "warning");
        return ticketResponse(saved);
    }

    public SupportTicketResponse ticketResponse(SupportTicket ticket) {
        return SupportTicketResponse.from(ticket,
                ticketHistoryRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId()));
    }

    private long countOrdersSince(List<Order> orders, Instant since) {
        return orders.stream().filter(order -> !orderCreatedAt(order).isBefore(since)).count();
    }

    private long revenueSince(List<Order> orders, Instant since) {
        return orders.stream().filter(order -> !orderCreatedAt(order).isBefore(since))
                .mapToLong(RevenuePolicy::netRevenue).sum();
    }

    private boolean needsPaymentReconciliation(Order order) {
        String payment = PaymentStatusPolicy.normalize(order.getPaymentStatus());
        if (PaymentStatusPolicy.REFUND_PENDING.equals(payment)
                || PaymentStatusPolicy.PARTIAL_REFUND_PENDING.equals(payment)) return true;
        if (PaymentStatusPolicy.isVNPay(order) && PaymentStatusPolicy.PAID.equals(payment)) {
            return !StringUtils.hasText(order.getVnpayTransactionId());
        }
        return PaymentStatusPolicy.isVNPay(order)
                && PaymentStatusPolicy.PENDING.equals(payment)
                && orderCreatedAt(order).isBefore(Instant.now().minusSeconds(15 * 60));
    }

    private boolean allowedRestaurantTransition(RestaurantLifecycleStatus from, RestaurantLifecycleStatus to) {
        return switch (from) {
            case PENDING -> to == RestaurantLifecycleStatus.ACTIVE || to == RestaurantLifecycleStatus.CLOSED;
            case ACTIVE -> to == RestaurantLifecycleStatus.SUSPENDED || to == RestaurantLifecycleStatus.CLOSED;
            case SUSPENDED -> to == RestaurantLifecycleStatus.ACTIVE || to == RestaurantLifecycleStatus.CLOSED;
            case CLOSED -> to == RestaurantLifecycleStatus.PENDING;
        };
    }

    private Instant orderCreatedAt(Order order) {
        return order.getCreatedAtInstant() == null ? Instant.EPOCH : order.getCreatedAtInstant();
    }

    private Map<String, Object> orderSummary(Order order) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", order.getId());
        map.put("restaurantId", order.getRestaurantId());
        map.put("restaurant", order.getRestaurant());
        map.put("status", order.getStatusString());
        map.put("paymentMethod", order.getPaymentMethod());
        map.put("paymentStatus", order.getPaymentStatus());
        map.put("total", order.getTotal());
        map.put("createdAt", orderCreatedAt(order).toEpochMilli());
        return map;
    }

    private Map<String, Object> paymentSummary(Order order) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("orderId", order.getId());
        map.put("method", order.getPaymentMethod());
        map.put("status", PaymentStatusPolicy.normalize(order.getPaymentStatus()));
        map.put("amount", order.getTotal());
        map.put("transactionId", order.getVnpayTransactionId());
        map.put("createdAt", orderCreatedAt(order).toEpochMilli());
        return map;
    }

    private Map<String, Object> complaintSummary(Complaint item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", item.getId());
        map.put("orderId", item.getOrderId());
        map.put("category", item.getCategory());
        map.put("status", item.getStatus());
        map.put("createdAt", item.getCreatedAt());
        return map;
    }

    private Map<String, Object> addressSummary(DeliveryAddress item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", item.getId());
        map.put("label", item.getLabel());
        map.put("recipientName", item.getRecipientName());
        map.put("phone", item.getPhone());
        map.put("addressLine", item.getAddressLine());
        map.put("defaultAddress", item.isDefaultAddress());
        return map;
    }

    private Map<String, Object> ticketQueueItem(SupportTicket ticket) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", "SUPPORT_TICKET");
        map.put("id", ticket.getId());
        map.put("orderId", ticket.getOrderId());
        map.put("restaurantId", ticket.getRestaurantId());
        map.put("customerName", ticket.getCustomerName());
        map.put("summary", ticket.getMessage());
        map.put("status", ticket.getStatus());
        map.put("priority", ticket.getPriority());
        map.put("department", ticket.getDepartment());
        map.put("assignedId", ticket.getAssignedAdminId());
        map.put("assignedName", ticket.getAssignedAdminName());
        long dueAt = SupportDeadlinePolicy.ticketDeadline(ticket);
        map.put("slaDueAt", dueAt);
        map.put("overdue", SupportDeadlinePolicy.isTicketOverdue(ticket, System.currentTimeMillis()));
        map.put("createdAt", ticket.getCreatedAt());
        map.put("history", ticketHistoryRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId()));
        return map;
    }

    private Map<String, Object> complaintQueueItem(Complaint item) {
        long dueAt = SupportDeadlinePolicy.complaintDeadline(item);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", "COMPLAINT");
        map.put("id", item.getId());
        map.put("orderId", item.getOrderId());
        map.put("restaurantId", item.getRestaurantId());
        map.put("summary", item.getDescription());
        map.put("status", item.getStatus());
        map.put("priority", SupportPriority.HIGH);
        map.put("department", SupportDepartment.OPERATIONS);
        map.put("assignedId", item.getHandlerId());
        map.put("assignedName", item.getHandlerRole());
        map.put("slaDueAt", dueAt);
        map.put("overdue", SupportDeadlinePolicy.isComplaintOverdue(item, System.currentTimeMillis()));
        map.put("createdAt", item.getCreatedAt());
        map.put("history", complaintHistoryRepository.findByComplaintIdOrderByCreatedAtAsc(item.getId()));
        return map;
    }

    private void appendTicketHistory(
            SupportTicket ticket,
            String action,
            User admin,
            SupportTicketStatus before,
            SupportTicketStatus after,
            String note
    ) {
        SupportTicketHistory history = new SupportTicketHistory();
        history.setTicketId(ticket.getId());
        history.setAction(action);
        history.setActorRole("ADMIN");
        history.setActorId(admin.getId());
        history.setFromStatus(before.name());
        history.setToStatus(after.name());
        history.setNote(StringUtils.hasText(note) ? note.trim() : null);
        ticketHistoryRepository.save(history);
    }

    private String supportAuditDetails(SupportTicket ticket, String note) {
        String result = "Trạng thái " + ticket.getStatus() + ", ưu tiên " + ticket.getPriority()
                + ", bộ phận " + ticket.getDepartment();
        return StringUtils.hasText(note) ? result + ": " + note.trim() : result;
    }

    private int priorityWeight(String priority) {
        return switch (priority) {
            case "URGENT" -> 4;
            case "HIGH" -> 3;
            case "NORMAL" -> 2;
            default -> 1;
        };
    }
}
