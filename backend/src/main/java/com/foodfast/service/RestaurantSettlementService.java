package com.foodfast.service;

import com.foodfast.dto.CreateSettlementRequest;
import com.foodfast.dto.SettlementDashboardResponse;
import com.foodfast.dto.SettlementOrderResponse;
import com.foodfast.dto.SettlementPreviewResponse;
import com.foodfast.dto.SettlementResponse;
import com.foodfast.entity.ManagerPermission;
import com.foodfast.entity.Order;
import com.foodfast.entity.OrderStatus;
import com.foodfast.entity.Restaurant;
import com.foodfast.entity.RestaurantSettlement;
import com.foodfast.entity.RestaurantSettlementOrder;
import com.foodfast.entity.SettlementStatus;
import com.foodfast.entity.User;
import com.foodfast.repository.OrderRepository;
import com.foodfast.repository.RestaurantRepository;
import com.foodfast.repository.RestaurantSettlementOrderRepository;
import com.foodfast.repository.RestaurantSettlementRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RestaurantSettlementService {
    private final OrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantSettlementRepository settlementRepository;
    private final RestaurantSettlementOrderRepository settlementOrderRepository;
    private final ManagerAuthorizationService authorizationService;
    private final AdminAuditLogService auditLogService;
    private final int commissionRateBps;

    public RestaurantSettlementService(
            OrderRepository orderRepository,
            RestaurantRepository restaurantRepository,
            RestaurantSettlementRepository settlementRepository,
            RestaurantSettlementOrderRepository settlementOrderRepository,
            ManagerAuthorizationService authorizationService,
            AdminAuditLogService auditLogService,
            @Value("${settlement.commission-rate-bps:1500}") int commissionRateBps
    ) {
        this.orderRepository = orderRepository;
        this.restaurantRepository = restaurantRepository;
        this.settlementRepository = settlementRepository;
        this.settlementOrderRepository = settlementOrderRepository;
        this.authorizationService = authorizationService;
        this.auditLogService = auditLogService;
        if (commissionRateBps < 0 || commissionRateBps > 10_000) {
            throw new IllegalArgumentException("Tỷ lệ hoa hồng đối soát phải từ 0 đến 10000 điểm cơ bản");
        }
        this.commissionRateBps = commissionRateBps;
    }

    @Transactional(readOnly = true)
    public SettlementDashboardResponse dashboard(User manager, Instant start, Instant end) {
        authorizationService.requireAny(manager,
                ManagerPermission.RESTAURANT_SETTLEMENT,
                ManagerPermission.PAYMENT_RECONCILIATION);
        Period period = requirePeriod(start, end);
        List<SettlementResponse> settlements = settlementRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::response).toList();
        List<SettlementPreviewResponse> previews = previewInternal(period);
        List<String> alerts = previews.stream().flatMap(item -> item.alerts().stream()).distinct().toList();
        long pendingAmount = settlements.stream()
                .filter(item -> item.status() == SettlementStatus.PENDING_PAYMENT)
                .mapToLong(SettlementResponse::payableAmount).sum();
        long paidAmount = settlements.stream()
                .filter(item -> item.status() == SettlementStatus.PAID)
                .mapToLong(SettlementResponse::payableAmount).sum();
        return new SettlementDashboardResponse(
                settlements.stream().filter(item -> item.status() == SettlementStatus.PENDING_REVIEW).count(),
                settlements.stream().filter(item -> item.status() == SettlementStatus.PENDING_PAYMENT).count(),
                settlements.stream().filter(item -> item.status() == SettlementStatus.PAID).count(),
                pendingAmount,
                paidAmount,
                previews,
                settlements,
                alerts
        );
    }

    @Transactional(readOnly = true)
    public List<SettlementPreviewResponse> preview(User manager, Instant start, Instant end) {
        authorizationService.requireAny(manager,
                ManagerPermission.RESTAURANT_SETTLEMENT,
                ManagerPermission.PAYMENT_RECONCILIATION);
        return previewInternal(requirePeriod(start, end));
    }

    @Transactional
    public SettlementResponse create(User manager, CreateSettlementRequest request) {
        authorizationService.require(manager, ManagerPermission.RESTAURANT_SETTLEMENT);
        Period period = requirePeriod(request.periodStart(), request.periodEnd());
        String restaurantId = RestaurantIdentityPolicy.canonicalize(request.restaurantId());
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhà hàng: " + restaurantId));

        List<Order> locked = orderRepository.findSettlementCandidatesForUpdate(
                restaurantId, OrderStatus.DELIVERED,
                period.start().toEpochMilli(), period.end().toEpochMilli()
        );
        List<Line> lines = locked.stream()
                .filter(this::isEligible)
                .filter(order -> !settlementOrderRepository.existsByOrderId(order.getId()))
                .map(this::calculateLine)
                .toList();
        if (lines.isEmpty()) {
            throw new IllegalStateException("Không có đơn đủ điều kiện đối soát trong khoảng đã chọn");
        }

        RestaurantSettlement settlement = new RestaurantSettlement();
        settlement.setId("SETTLEMENT-" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 12).toUpperCase());
        settlement.setRestaurantId(restaurantId);
        settlement.setRestaurantName(restaurant.getName());
        settlement.setPeriodStart(period.start());
        settlement.setPeriodEnd(period.end());
        settlement.setStatus(SettlementStatus.PENDING_REVIEW);
        settlement.setCommissionRateBps(commissionRateBps);
        settlement.setCreatedBy(manager.getId());
        applyTotals(settlement, lines);
        RestaurantSettlement saved = settlementRepository.save(settlement);

        List<RestaurantSettlementOrder> items = lines.stream()
                .map(line -> settlementOrder(saved.getId(), line))
                .toList();
        settlementOrderRepository.saveAll(items);
        auditLogService.record(manager, "CREATE_RESTAURANT_SETTLEMENT", "RESTAURANT_SETTLEMENT",
                saved.getId(), saved.getRestaurantName(),
                "Lập đợt đối soát gồm " + saved.getOrderCount() + " đơn, phải trả "
                        + saved.getPayableAmount() + " đ", "INFO");
        return response(saved, items);
    }

    @Transactional
    public SettlementResponse submit(User manager, String settlementId) {
        authorizationService.require(manager, ManagerPermission.RESTAURANT_SETTLEMENT);
        RestaurantSettlement settlement = requireLocked(settlementId);
        if (settlement.getStatus() != SettlementStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Chỉ đợt đang chờ kiểm tra mới được chuyển sang chờ thanh toán");
        }
        settlement.setStatus(SettlementStatus.PENDING_PAYMENT);
        settlement.setSubmittedBy(manager.getId());
        settlement.setSubmittedAt(Instant.now());
        RestaurantSettlement saved = settlementRepository.save(settlement);
        auditLogService.record(manager, "SUBMIT_RESTAURANT_SETTLEMENT", "RESTAURANT_SETTLEMENT",
                saved.getId(), saved.getRestaurantName(), "Đã kiểm tra và chờ thanh toán", "INFO");
        return response(saved);
    }

    @Transactional
    public SettlementResponse markPaid(User manager, String settlementId, String paymentReference) {
        authorizationService.require(manager, ManagerPermission.PAYMENT_RECONCILIATION);
        if (!StringUtils.hasText(paymentReference)) {
            throw new IllegalArgumentException("Vui lòng nhập mã tham chiếu thanh toán");
        }
        RestaurantSettlement settlement = requireLocked(settlementId);
        if (settlement.getStatus() == SettlementStatus.PAID) {
            throw new IllegalStateException("Đợt đối soát này đã được thanh toán trước đó");
        }
        if (settlement.getStatus() != SettlementStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Đợt đối soát phải được kiểm tra trước khi thanh toán");
        }
        settlement.setStatus(SettlementStatus.PAID);
        settlement.setPaidBy(manager.getId());
        settlement.setPaidAt(Instant.now());
        settlement.setPaymentReference(paymentReference.trim());
        RestaurantSettlement saved = settlementRepository.save(settlement);
        auditLogService.record(manager, "PAY_RESTAURANT_SETTLEMENT", "RESTAURANT_SETTLEMENT",
                saved.getId(), saved.getRestaurantName(),
                "Đã thanh toán " + saved.getPayableAmount() + " đ, tham chiếu "
                        + saved.getPaymentReference(), "INFO");
        return response(saved);
    }

    @Transactional(readOnly = true)
    public SettlementResponse findOne(User manager, String settlementId) {
        authorizationService.requireAny(manager,
                ManagerPermission.RESTAURANT_SETTLEMENT,
                ManagerPermission.PAYMENT_RECONCILIATION);
        return response(settlementRepository.findById(settlementId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đợt đối soát")));
    }

    private List<SettlementPreviewResponse> previewInternal(Period period) {
        List<Order> candidates = orderRepository.findSettlementCandidates(
                OrderStatus.DELIVERED, period.start().toEpochMilli(), period.end().toEpochMilli());
        Map<String, PreviewBucket> buckets = new LinkedHashMap<>();
        for (Order order : candidates) {
            String restaurantId = RestaurantIdentityPolicy.canonicalize(order.getRestaurantId());
            if (!StringUtils.hasText(restaurantId)) {
                String unresolvedId = "__UNRESOLVED__" + order.getId();
                PreviewBucket unresolved = buckets.computeIfAbsent(unresolvedId,
                        ignored -> new PreviewBucket(unresolvedId, "Không xác định nhà hàng",
                                new ArrayList<>(), new ArrayList<>()));
                unresolved.alerts().add("Đơn " + order.getId()
                        + " thiếu mã nhà hàng nên chưa thể đối soát");
                continue;
            }
            PreviewBucket bucket = buckets.computeIfAbsent(restaurantId,
                    ignored -> new PreviewBucket(restaurantId, safeRestaurantName(order), new ArrayList<>(), new ArrayList<>()));
            if (settlementOrderRepository.existsByOrderId(order.getId())) continue;
            if (isEligible(order)) {
                bucket.lines().add(calculateLine(order));
            } else {
                bucket.alerts().add(alertFor(order));
            }
        }
        return buckets.values().stream()
                .map(this::previewResponse)
                .filter(item -> item.eligibleOrderCount() > 0 || !item.alerts().isEmpty())
                .sorted(Comparator.comparing(SettlementPreviewResponse::restaurantName))
                .toList();
    }

    private SettlementPreviewResponse previewResponse(PreviewBucket bucket) {
        Totals totals = totals(bucket.lines());
        return new SettlementPreviewResponse(
                bucket.restaurantId(), bucket.restaurantName(), bucket.lines().size(),
                totals.food(), totals.tax(), totals.restaurantDiscount(), totals.platformDiscount(),
                totals.commission(), totals.refund(), totals.payable(),
                bucket.lines().stream().map(line -> line.order().getId()).toList(),
                List.copyOf(bucket.alerts())
        );
    }

    private String alertFor(Order order) {
        String payment = PaymentStatusPolicy.normalize(order.getPaymentStatus());
        if (PaymentStatusPolicy.REFUND_PENDING.equals(payment)
                || PaymentStatusPolicy.PARTIAL_REFUND_PENDING.equals(payment)) {
            return "Đơn " + order.getId() + " đang hoàn giao dịch nên không đưa vào đối soát nhà hàng";
        }
        if (PaymentStatusPolicy.REFUNDED.equals(payment)
                || PaymentStatusPolicy.PARTIAL_REFUNDED.equals(payment)) {
            return "Đơn " + order.getId() + " đã hoàn giao dịch nên không đưa vào đối soát nhà hàng";
        }
        return "Đơn " + order.getId() + " có trạng thái thanh toán “" + payment
                + "”, cần kiểm tra trước khi đối soát";
    }

    private boolean isEligible(Order order) {
        String status = PaymentStatusPolicy.normalize(order.getPaymentStatus());
        return order.getStatusEnum() == OrderStatus.DELIVERED
                && PaymentStatusPolicy.PAID.equals(status);
    }

    private Line calculateLine(Order order) {
        RestaurantSettlementCalculator.Components amount =
                RestaurantSettlementCalculator.calculate(order, commissionRateBps);
        return new Line(order, amount.food(), amount.tax(), amount.restaurantDiscount(),
                amount.platformDiscount(), amount.commission(), amount.refund(), amount.payable());
    }

    private RestaurantSettlementOrder settlementOrder(String settlementId, Line line) {
        RestaurantSettlementOrder item = new RestaurantSettlementOrder();
        item.setSettlementId(settlementId);
        item.setOrderId(line.order().getId());
        item.setFoodAmount(line.food());
        item.setRestaurantTaxAmount(line.tax());
        item.setRestaurantDiscountAmount(line.restaurantDiscount());
        item.setPlatformDiscountAmount(line.platformDiscount());
        item.setCommissionAmount(line.commission());
        item.setRefundAmount(line.refund());
        item.setPayableAmount(line.payable());
        return item;
    }

    private void applyTotals(RestaurantSettlement settlement, List<Line> lines) {
        Totals totals = totals(lines);
        settlement.setOrderCount(lines.size());
        settlement.setFoodAmount(totals.food());
        settlement.setRestaurantTaxAmount(totals.tax());
        settlement.setRestaurantDiscountAmount(totals.restaurantDiscount());
        settlement.setPlatformDiscountAmount(totals.platformDiscount());
        settlement.setCommissionAmount(totals.commission());
        settlement.setRefundAmount(totals.refund());
        settlement.setAdjustmentAmount(0L);
        settlement.setPayableAmount(totals.payable());
    }

    private Totals totals(List<Line> lines) {
        return new Totals(
                lines.stream().mapToLong(Line::food).sum(),
                lines.stream().mapToLong(Line::tax).sum(),
                lines.stream().mapToLong(Line::restaurantDiscount).sum(),
                lines.stream().mapToLong(Line::platformDiscount).sum(),
                lines.stream().mapToLong(Line::commission).sum(),
                lines.stream().mapToLong(Line::refund).sum(),
                lines.stream().mapToLong(Line::payable).sum()
        );
    }

    private SettlementResponse response(RestaurantSettlement settlement) {
        return response(settlement,
                settlementOrderRepository.findBySettlementIdOrderByOrderId(settlement.getId()));
    }

    private SettlementResponse response(
            RestaurantSettlement settlement,
            List<RestaurantSettlementOrder> items
    ) {
        return new SettlementResponse(
                settlement.getId(), settlement.getRestaurantId(), settlement.getRestaurantName(),
                settlement.getPeriodStart(), settlement.getPeriodEnd(), settlement.getStatus(),
                settlement.getOrderCount(), settlement.getFoodAmount(), settlement.getRestaurantTaxAmount(),
                settlement.getRestaurantDiscountAmount(), settlement.getPlatformDiscountAmount(),
                settlement.getCommissionRateBps(), settlement.getCommissionAmount(), settlement.getRefundAmount(),
                settlement.getAdjustmentAmount(), settlement.getPayableAmount(), settlement.getCreatedBy(),
                settlement.getCreatedAt(), settlement.getSubmittedBy(), settlement.getSubmittedAt(),
                settlement.getPaidBy(), settlement.getPaidAt(), settlement.getPaymentReference(),
                items.stream().map(item -> new SettlementOrderResponse(
                        item.getOrderId(), item.getFoodAmount(), item.getRestaurantTaxAmount(),
                        item.getRestaurantDiscountAmount(), item.getPlatformDiscountAmount(),
                        item.getCommissionAmount(), item.getRefundAmount(), item.getPayableAmount()
                )).toList()
        );
    }

    private RestaurantSettlement requireLocked(String id) {
        return settlementRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đợt đối soát"));
    }

    private Period requirePeriod(Instant start, Instant end) {
        if (start == null || end == null || !start.isBefore(end)) {
            throw new IllegalArgumentException("Khoảng đối soát không hợp lệ");
        }
        if (Duration.between(start, end).toDays() > 366) {
            throw new IllegalArgumentException("Mỗi lần chỉ được đối soát tối đa 366 ngày");
        }
        return new Period(start, end);
    }

    private String safeRestaurantName(Order order) {
        return StringUtils.hasText(order.getRestaurant()) ? order.getRestaurant() : order.getRestaurantId();
    }

    private record Period(Instant start, Instant end) { }
    private record Line(Order order, long food, long tax, long restaurantDiscount,
                        long platformDiscount, long commission, long refund, long payable) { }
    private record Totals(long food, long tax, long restaurantDiscount, long platformDiscount,
                          long commission, long refund, long payable) { }
    private record PreviewBucket(String restaurantId, String restaurantName,
                                 List<Line> lines, List<String> alerts) { }
}
