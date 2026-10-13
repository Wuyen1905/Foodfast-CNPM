package com.foodfast.service;

import com.foodfast.dto.AdminAnalyticsResponse;
import com.foodfast.entity.CancellationReasonCode;
import com.foodfast.entity.DeliveryAssignment;
import com.foodfast.entity.DeliveryStatus;
import com.foodfast.entity.Order;
import com.foodfast.entity.OrderStatus;
import com.foodfast.entity.Restaurant;
import com.foodfast.repository.DeliveryAssignmentRepository;
import com.foodfast.repository.OrderRepository;
import com.foodfast.repository.RestaurantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AdminAnalyticsService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter LABEL_FORMAT = DateTimeFormatter.ofPattern("dd/MM");
    private static final long MINUTE = 60_000L;
    private static final double MAX_OPERATION_DURATION_MINUTES = 24 * 60;

    private final OrderRepository orderRepository;
    private final DeliveryAssignmentRepository deliveryRepository;
    private final RestaurantRepository restaurantRepository;

    public AdminAnalyticsService(
            OrderRepository orderRepository,
            DeliveryAssignmentRepository deliveryRepository,
            RestaurantRepository restaurantRepository
    ) {
        this.orderRepository = orderRepository;
        this.deliveryRepository = deliveryRepository;
        this.restaurantRepository = restaurantRepository;
    }

    @Transactional(readOnly = true)
    public AdminAnalyticsResponse report(int requestedDays) {
        return calculate(
                requestedDays,
                orderRepository.findAll(),
                deliveryRepository.findAll(),
                restaurantRepository.findAll()
        );
    }

    AdminAnalyticsResponse calculate(
            int requestedDays,
            List<Order> allOrders,
            List<DeliveryAssignment> deliveries,
            List<Restaurant> restaurants
    ) {
        int days = normalizeDays(requestedDays);
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDate firstDay = today.minusDays(days - 1L);
        Instant from = firstDay.atStartOfDay(BUSINESS_ZONE).toInstant();
        Instant to = today.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant();

        List<Order> orders = allOrders.stream()
                .filter(order -> order.getCreatedAtInstant() != null)
                .filter(order -> !order.getCreatedAtInstant().isBefore(from)
                        && order.getCreatedAtInstant().isBefore(to))
                .toList();
        Set<String> orderIds = orders.stream().map(Order::getId).collect(Collectors.toSet());
        List<DeliveryAssignment> periodDeliveries = deliveries.stream()
                .filter(item -> item.getOrderId() != null && orderIds.contains(item.getOrderId()))
                .toList();
        Map<String, List<DeliveryAssignment>> deliveriesByOrder = periodDeliveries.stream()
                .filter(item -> item.getOrderId() != null)
                .collect(Collectors.groupingBy(DeliveryAssignment::getOrderId));

        Map<String, String> restaurantNames = restaurants.stream()
                .collect(Collectors.toMap(Restaurant::getId, Restaurant::getName, (first, ignored) -> first));

        long completed = orders.stream().filter(order -> order.getStatusEnum() == OrderStatus.DELIVERED).count();
        long cancelled = orders.stream().filter(order -> order.getStatusEnum() == OrderStatus.CANCELLED).count();
        long revenue = orders.stream().mapToLong(RevenuePolicy::netRevenue).sum();
        long recognizedOrders = orders.stream().filter(RevenuePolicy::contributesToRevenue).count();

        AdminAnalyticsResponse.Summary summary = new AdminAnalyticsResponse.Summary(
                orders.size(), completed, cancelled, revenue,
                recognizedOrders == 0 ? 0 : Math.round((double) revenue / recognizedOrders),
                percent(completed, orders.size()), percent(cancelled, orders.size()),
                onTimeRate(orders, deliveriesByOrder),
                averageConfirmationMinutes(orders),
                averagePreparationMinutes(orders, deliveriesByOrder),
                averageDeliveryMinutes(periodDeliveries)
        );

        return new AdminAnalyticsResponse(
                days,
                from.toEpochMilli(),
                to.minusMillis(1).toEpochMilli(),
                summary,
                dailyTrend(firstDay, today, orders),
                statusBreakdown(orders),
                paymentBreakdown(orders),
                cancellationReasons(orders),
                restaurantPerformance(orders, restaurantNames)
        );
    }

    private int normalizeDays(int days) {
        if (days == 7 || days == 30 || days == 90) return days;
        throw new IllegalArgumentException("Khoảng báo cáo chỉ hỗ trợ 7, 30 hoặc 90 ngày");
    }

    private List<AdminAnalyticsResponse.DailyPoint> dailyTrend(
            LocalDate firstDay,
            LocalDate today,
            List<Order> orders
    ) {
        Map<LocalDate, List<Order>> byDay = orders.stream().collect(Collectors.groupingBy(
                order -> order.getCreatedAtInstant().atZone(BUSINESS_ZONE).toLocalDate()));
        List<AdminAnalyticsResponse.DailyPoint> result = new ArrayList<>();
        for (LocalDate day = firstDay; !day.isAfter(today); day = day.plusDays(1)) {
            List<Order> daily = byDay.getOrDefault(day, List.of());
            result.add(new AdminAnalyticsResponse.DailyPoint(
                    day.toString(), day.format(LABEL_FORMAT), daily.size(),
                    daily.stream().filter(order -> order.getStatusEnum() == OrderStatus.DELIVERED).count(),
                    daily.stream().filter(order -> order.getStatusEnum() == OrderStatus.CANCELLED).count(),
                    daily.stream().mapToLong(RevenuePolicy::netRevenue).sum()
            ));
        }
        return result;
    }

    private Map<String, Long> statusBreakdown(List<Order> orders) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) result.put(status.name(), 0L);
        orders.forEach(order -> result.computeIfPresent(order.getStatusEnum().name(), (key, value) -> value + 1));
        return result;
    }

    private Map<String, Long> paymentBreakdown(List<Order> orders) {
        return orders.stream().collect(Collectors.groupingBy(
                order -> {
                    String value = PaymentStatusPolicy.normalize(order.getPaymentStatus());
                    return value == null || value.isBlank() ? "UNKNOWN" : value;
                }, LinkedHashMap::new, Collectors.counting()));
    }

    private List<AdminAnalyticsResponse.ReasonCount> cancellationReasons(List<Order> orders) {
        Map<CancellationReasonCode, Long> counts = orders.stream()
                .filter(order -> order.getStatusEnum() == OrderStatus.CANCELLED)
                .filter(order -> order.getCancellationReasonCode() != null)
                .collect(Collectors.groupingBy(Order::getCancellationReasonCode,
                        () -> new EnumMap<>(CancellationReasonCode.class), Collectors.counting()));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<CancellationReasonCode, Long>comparingByValue().reversed())
                .map(entry -> new AdminAnalyticsResponse.ReasonCount(entry.getKey().name(), entry.getValue()))
                .toList();
    }

    private List<AdminAnalyticsResponse.RestaurantPerformance> restaurantPerformance(
            List<Order> orders,
            Map<String, String> restaurantNames
    ) {
        Map<String, List<Order>> grouped = orders.stream().collect(Collectors.groupingBy(order -> {
            if (order.getRestaurantId() != null && !order.getRestaurantId().isBlank()) return order.getRestaurantId();
            return order.getRestaurant() == null ? "UNKNOWN" : order.getRestaurant();
        }));
        return grouped.entrySet().stream().map(entry -> {
                    List<Order> values = entry.getValue();
                    long completed = values.stream().filter(order -> order.getStatusEnum() == OrderStatus.DELIVERED).count();
                    long cancelled = values.stream().filter(order -> order.getStatusEnum() == OrderStatus.CANCELLED).count();
                    long revenue = values.stream().mapToLong(RevenuePolicy::netRevenue).sum();
                    String fallbackName = values.stream().map(Order::getRestaurant)
                            .filter(Objects::nonNull).filter(value -> !value.isBlank()).findFirst().orElse(entry.getKey());
                    return new AdminAnalyticsResponse.RestaurantPerformance(
                            entry.getKey(), restaurantNames.getOrDefault(entry.getKey(), fallbackName),
                            values.size(), completed, cancelled, revenue,
                            percent(completed, values.size()), percent(cancelled, values.size())
                    );
                })
                .sorted(Comparator.comparingLong(AdminAnalyticsResponse.RestaurantPerformance::revenue).reversed()
                        .thenComparing(Comparator.comparingLong(AdminAnalyticsResponse.RestaurantPerformance::orders).reversed()))
                .limit(10)
                .toList();
    }

    private Double averageConfirmationMinutes(List<Order> orders) {
        return average(orders.stream().map(order -> durationMinutes(
                order.getCreatedAtInstant() == null ? null : order.getCreatedAtInstant().toEpochMilli(),
                order.getConfirmedAt())).filter(Objects::nonNull).toList());
    }

    private Double averagePreparationMinutes(
            List<Order> orders,
            Map<String, List<DeliveryAssignment>> deliveriesByOrder
    ) {
        return average(orders.stream().map(order -> {
            Long readyAt = order.getReadyAt();
            if (readyAt == null) {
                readyAt = deliveriesByOrder.getOrDefault(order.getId(), List.of()).stream()
                        .map(DeliveryAssignment::getAssignedAt).min(Long::compareTo).orElse(null);
            }
            return durationMinutes(order.getConfirmedAt(), readyAt);
        }).filter(Objects::nonNull).toList());
    }

    private Double averageDeliveryMinutes(List<DeliveryAssignment> deliveries) {
        return average(deliveries.stream()
                .filter(item -> item.getStatus() == DeliveryStatus.DELIVERED)
                .map(item -> durationMinutes(item.getStartedAt(), item.getDeliveredAt()))
                .filter(Objects::nonNull).toList());
    }

    private Double onTimeRate(
            List<Order> orders,
            Map<String, List<DeliveryAssignment>> deliveriesByOrder
    ) {
        List<DeliveryAssignment> eligible = orders.stream()
                .flatMap(order -> deliveriesByOrder.getOrDefault(order.getId(), List.of()).stream())
                .filter(item -> item.getStatus() == DeliveryStatus.DELIVERED)
                .filter(item -> durationMinutes(item.getStartedAt(), item.getDeliveredAt()) != null)
                .filter(item -> item.getEstimatedDeliveryAt() != null)
                .toList();
        if (eligible.isEmpty()) return null;
        long onTime = eligible.stream()
                .filter(item -> item.getDeliveredAt() <= item.getEstimatedDeliveryAt()).count();
        return percent(onTime, eligible.size());
    }

    private Double durationMinutes(Long start, Long end) {
        if (start == null || end == null || end < start) return null;
        double duration = (double) (end - start) / MINUTE;
        return duration <= MAX_OPERATION_DURATION_MINUTES ? duration : null;
    }

    private Double average(List<Double> values) {
        if (values.isEmpty()) return null;
        return roundOne(values.stream().mapToDouble(Double::doubleValue).average().orElse(0));
    }

    private double percent(long value, long total) {
        return total == 0 ? 0 : roundOne(value * 100.0 / total);
    }

    private double roundOne(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
