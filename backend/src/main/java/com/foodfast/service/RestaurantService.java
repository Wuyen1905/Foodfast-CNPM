package com.foodfast.service;

import com.foodfast.dto.UpdateRestaurantOperationsRequest;
import com.foodfast.entity.Restaurant;
import com.foodfast.exception.ForbiddenOperationException;
import com.foodfast.repository.RestaurantRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

@Service
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;

    public RestaurantService(RestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }

    public List<Restaurant> getAllActiveRestaurants() {
        return restaurantRepository.findByIsActiveTrue();
    }

    public Optional<Restaurant> findById(String id) {
        return restaurantRepository.findById(id);
    }

    public Optional<Restaurant> findByOwnerId(String ownerId) {
        return restaurantRepository.findByOwnerId(ownerId);
    }

    public List<Restaurant> findByCategory(String category) {
        return restaurantRepository.findByCategoryIgnoreCase(category);
    }

    public List<Restaurant> getAll() {
        return restaurantRepository.findAll();
    }

    public Restaurant updateOperations(
            String restaurantId,
            String authenticatedUserId,
            boolean admin,
            UpdateRestaurantOperationsRequest request
    ) {
        String canonicalId = RestaurantIdentityPolicy.canonicalize(restaurantId);
        Restaurant restaurant = restaurantRepository.findById(canonicalId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhà hàng: " + restaurantId));

        if (request == null || (!admin && (authenticatedUserId == null
                || !authenticatedUserId.trim().equals(restaurant.getOwnerId())))) {
            throw new ForbiddenOperationException("Chỉ chủ nhà hàng mới được thay đổi trạng thái hoạt động");
        }

        if (request.openingTime != null || request.closingTime != null) {
            String opening = request.openingTime != null ? request.openingTime : restaurant.getOpeningTime();
            String closing = request.closingTime != null ? request.closingTime : restaurant.getClosingTime();
            validateOperatingHours(opening, closing);
            restaurant.setOpeningTime(opening);
            restaurant.setClosingTime(closing);
        }

        if (request.acceptingOrders != null) {
            if (!request.acceptingOrders) {
                String reason = request.pauseReason == null ? "" : request.pauseReason.trim();
                if (reason.length() < 5) {
                    throw new IllegalArgumentException("Vui lòng nhập lý do tạm ngừng ít nhất 5 ký tự");
                }
                restaurant.setPauseReason(reason);
            } else {
                restaurant.setPauseReason(null);
            }
            restaurant.setAcceptingOrders(request.acceptingOrders);
        }

        return restaurantRepository.save(restaurant);
    }

    public Restaurant requireAvailableForOrdering(String restaurantId, LocalTime atTime) {
        String canonicalId = RestaurantIdentityPolicy.canonicalize(restaurantId);
        Restaurant restaurant = restaurantRepository.findById(canonicalId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhà hàng của món ăn"));
        if (!Boolean.TRUE.equals(restaurant.getIsActive())) {
            throw new IllegalStateException("Nhà hàng hiện không hoạt động");
        }
        if (!Boolean.TRUE.equals(restaurant.getAcceptingOrders())) {
            String reason = restaurant.getPauseReason();
            throw new IllegalStateException(
                    reason == null || reason.isBlank()
                            ? "Nhà hàng đang tạm ngừng nhận đơn"
                            : "Nhà hàng đang tạm ngừng nhận đơn: " + reason
            );
        }
        if (!restaurant.isOpenAt(atTime)) {
            throw new IllegalStateException(
                    "Nhà hàng hiện đã đóng cửa. Giờ phục vụ: "
                            + restaurant.getOpeningTime() + "–" + restaurant.getClosingTime()
            );
        }
        return restaurant;
    }

    private void validateOperatingHours(String opening, String closing) {
        try {
            LocalTime.parse(opening);
            LocalTime.parse(closing);
        } catch (DateTimeParseException | NullPointerException error) {
            throw new IllegalArgumentException("Giờ hoạt động phải có dạng HH:mm");
        }
    }
}

