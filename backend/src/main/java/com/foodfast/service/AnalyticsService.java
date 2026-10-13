package com.foodfast.service;

import com.foodfast.entity.Analytics;
import com.foodfast.entity.Order;
import com.foodfast.entity.OrderItem;
import com.foodfast.entity.OrderStatus;
import com.foodfast.entity.Restaurant;
import com.foodfast.repository.AnalyticsRepository;
import com.foodfast.repository.DeliveryAssignmentRepository;
import com.foodfast.repository.OrderRepository;
import com.foodfast.repository.RestaurantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analytics Service
 * Calculates and provides analytics data for restaurants
 */
@Service
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepository;
    private final OrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;
    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    public AnalyticsService(
            AnalyticsRepository analyticsRepository,
            OrderRepository orderRepository,
            RestaurantRepository restaurantRepository,
            DeliveryAssignmentRepository deliveryAssignmentRepository) {
        this.analyticsRepository = analyticsRepository;
        this.orderRepository = orderRepository;
        this.restaurantRepository = restaurantRepository;
        this.deliveryAssignmentRepository = deliveryAssignmentRepository;
    }

    /**
     * Get analytics for a restaurant for a specific period
     * Note: This method is not used since we always calculate fresh analytics
     */
    @Transactional(readOnly = true)
    public Optional<Analytics> getAnalytics(String restaurantId, String period) {
        // Map period parameter to Vietnamese label for lookup
        String periodLabel = switch (period.toLowerCase()) {
            case "day" -> "Hôm nay";
            case "week" -> "Tuần này";
            case "month" -> "Tháng này";
            default -> "Hôm nay";
        };
        return analyticsRepository.findByRestaurantIdAndPeriod(restaurantId, periodLabel);
    }

    /**
     * Calculate analytics from orders for a restaurant
     * Returns Analytics with period label matching frontend expectations
     */
    @Transactional(readOnly = true)
    public Analytics calculateAnalytics(String restaurantId, String period) {
        String canonicalId = RestaurantIdentityPolicy.canonicalize(restaurantId);
        List<Order> orders = orderRepository.findByRestaurantIdIn(
                RestaurantIdentityPolicy.equivalentIds(canonicalId)
        );
        
        // Filter orders by period
        Instant now = Instant.now();
        Instant startDate;
        String periodLabel;
        
        switch (period.toLowerCase()) {
            case "day":
                startDate = LocalDate.now(BUSINESS_ZONE).atStartOfDay(BUSINESS_ZONE).toInstant();
                periodLabel = "Hôm nay";
                break;
            case "week":
                startDate = LocalDate.now(BUSINESS_ZONE).minusDays(6)
                        .atStartOfDay(BUSINESS_ZONE).toInstant();
                periodLabel = "Tuần này";
                break;
            case "month":
                startDate = LocalDate.now(BUSINESS_ZONE).withDayOfMonth(1)
                        .atStartOfDay(BUSINESS_ZONE).toInstant();
                periodLabel = "Tháng này";
                break;
            default:
                startDate = LocalDate.now(BUSINESS_ZONE).atStartOfDay(BUSINESS_ZONE).toInstant();
                periodLabel = "Hôm nay";
        }
        
        List<Order> filteredOrders = orders.stream()
                .filter(order -> {
                    Instant orderDate = order.getCreatedAtInstant();
                    return orderDate != null && !orderDate.isBefore(startDate) && !orderDate.isAfter(now);
                })
                .collect(Collectors.toList());
        
        long revenue = filteredOrders.stream()
                .mapToLong(RevenuePolicy::netRevenue)
                .sum();
        
        int ordersCount = filteredOrders.size();
        long revenueOrderCount = filteredOrders.stream()
                .filter(RevenuePolicy::contributesToRevenue)
                .count();
        int avgOrderValue = revenueOrderCount > 0 ? (int) (revenue / revenueOrderCount) : 0;
        
        int deliveryTime = averageDeliveryMinutes(canonicalId, startDate, now);
        
        Analytics analytics = new Analytics();
        analytics.setId(UUID.randomUUID().toString());
        analytics.setRestaurantId(canonicalId);
        analytics.setPeriod(periodLabel); // Use Vietnamese label for frontend
        analytics.setRevenue((int) revenue);
        analytics.setOrders(ordersCount);
        analytics.setAvgOrderValue(avgOrderValue);
        analytics.setDeliveryTime(deliveryTime);
        analytics.setCreatedAt(System.currentTimeMillis());
        
        return analytics;
    }

    /**
     * Get restaurant overview with KPIs
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getRestaurantOverview(String restaurantId) {
        String canonicalId = RestaurantIdentityPolicy.canonicalize(restaurantId);
        Optional<Restaurant> restaurantOpt = restaurantRepository.findById(canonicalId);
        if (restaurantOpt.isEmpty()) {
            return Collections.emptyMap();
        }
        
        Restaurant restaurant = restaurantOpt.get();
        List<Order> orders = orderRepository.findByRestaurantIdIn(
                RestaurantIdentityPolicy.equivalentIds(canonicalId)
        );
        
        // Calculate today's orders
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        List<Order> todayOrderList = orders.stream()
                .filter(order -> isOnDate(order, today))
                .toList();
        long todayOrders = todayOrderList.size();
        
        long todayRevenue = todayOrderList.stream()
                .mapToLong(RevenuePolicy::netRevenue)
                .sum();
        
        long activeDeliveries = orders.stream()
                .filter(order -> "Delivering".equalsIgnoreCase(order.getStatusString()))
                .count();

        long completedToday = todayOrderList.stream()
                .filter(order -> order.getStatusEnum() == OrderStatus.DELIVERED)
                .count();
        long cancelledToday = todayOrderList.stream()
                .filter(order -> order.getStatusEnum() == OrderStatus.CANCELLED)
                .count();
        double completionRate = todayOrders == 0 ? 0 : completedToday * 100.0 / todayOrders;
        double cancellationRate = todayOrders == 0 ? 0 : cancelledToday * 100.0 / todayOrders;

        int averageConfirmationMinutes = (int) Math.round(todayOrderList.stream()
                .filter(order -> order.getConfirmedAt() != null && order.getCreatedAt() != null)
                .mapToLong(order -> Math.max(0, order.getConfirmedAt() - order.getCreatedAt()))
                .average()
                .orElse(0) / 60_000.0);

        Instant todayStart = today.atStartOfDay(BUSINESS_ZONE).toInstant();
        int averageDeliveryMinutes = averageDeliveryMinutes(canonicalId, todayStart, Instant.now());

        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            statusCounts.put(status.name(), todayOrderList.stream()
                    .filter(order -> order.getStatusEnum() == status)
                    .count());
        }

        List<Map<String, Object>> revenueTrend = new ArrayList<>();
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("dd/MM");
        for (int offset = 6; offset >= 0; offset--) {
            LocalDate date = today.minusDays(offset);
            long value = orders.stream()
                    .filter(order -> isOnDate(order, date))
                    .mapToLong(RevenuePolicy::netRevenue)
                    .sum();
            revenueTrend.add(Map.of(
                    "date", date.toString(),
                    "label", date.format(labelFormatter),
                    "value", value
            ));
        }

        // Calculate top items from today's completed orders
        Map<String, ItemStats> itemStatsMap = new HashMap<>();
        for (Order order : todayOrderList) {
            if (!RevenuePolicy.contributesToRevenue(order)) continue;

            for (OrderItem item : order.getItems()) {
                String itemName = item.getName();
                ItemStats stats = itemStatsMap.getOrDefault(itemName, new ItemStats(itemName));
                stats.orders += item.getQty();
                stats.revenue += item.getPrice() * item.getQty();
                itemStatsMap.put(itemName, stats);
            }
        }

        List<Map<String, Object>> topItems = itemStatsMap.values().stream()
                .sorted((a, b) -> Long.compare(b.revenue, a.revenue))
                .limit(5)
                .map(stats -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", stats.name);
                    item.put("orders", stats.orders);
                    item.put("sales", stats.orders);
                    item.put("revenue", stats.revenue);
                    return item;
                })
                .collect(Collectors.toList());

        Map<String, Object> overview = new HashMap<>();
        overview.put("id", restaurant.getId());
        overview.put("name", restaurant.getName());
        overview.put("revenue", todayRevenue);
        overview.put("ordersToday", (int) todayOrders);
        overview.put("activeDeliveries", (int) activeDeliveries);
        overview.put("avgDeliveryTime", averageDeliveryMinutes);
        overview.put("avgConfirmationTime", averageConfirmationMinutes);
        overview.put("completionRate", completionRate);
        overview.put("cancellationRate", cancellationRate);
        overview.put("completedToday", completedToday);
        overview.put("cancelledToday", cancelledToday);
        overview.put("statusCounts", statusCounts);
        overview.put("revenueTrend", revenueTrend);
        overview.put("rating", restaurant.getRating() != null ? restaurant.getRating() : 0.0);
        overview.put("topItems", topItems);
        
        return overview;
    }

    private boolean isOnDate(Order order, LocalDate date) {
        return order.getCreatedAtInstant() != null
                && order.getCreatedAtInstant().atZone(BUSINESS_ZONE).toLocalDate().equals(date);
    }

    private int averageDeliveryMinutes(String restaurantId, Instant start, Instant end) {
        return (int) Math.round(deliveryAssignmentRepository
                .findByRestaurantIdOrderByAssignedAtDesc(restaurantId)
                .stream()
                .filter(assignment -> assignment.getStartedAt() != null && assignment.getDeliveredAt() != null)
                .filter(assignment -> {
                    Instant delivered = Instant.ofEpochMilli(assignment.getDeliveredAt());
                    return !delivered.isBefore(start) && !delivered.isAfter(end);
                })
                .mapToLong(assignment -> Math.max(0, assignment.getDeliveredAt() - assignment.getStartedAt()))
                .average()
                .orElse(0) / 60_000.0);
    }

    // Helper class for item statistics
    private static class ItemStats {
        String name;
        int orders = 0;
        long revenue = 0;

        ItemStats(String name) {
            this.name = name;
        }
    }
}

