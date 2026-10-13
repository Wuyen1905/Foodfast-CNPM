package com.foodfast.service;

import com.foodfast.entity.DeliveryStatus;
import com.foodfast.entity.RestaurantCourier;
import com.foodfast.repository.DeliveryAssignmentRepository;
import com.foodfast.repository.RestaurantCourierRepository;
import com.foodfast.repository.RestaurantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RestaurantCourierService {

    private static final List<DeliveryStatus> ACTIVE_DELIVERY_STATUSES =
            List.of(DeliveryStatus.ASSIGNED, DeliveryStatus.DELIVERING);

    private final RestaurantCourierRepository courierRepository;
    private final RestaurantRepository restaurantRepository;
    private final DeliveryAssignmentRepository assignmentRepository;

    public RestaurantCourierService(
            RestaurantCourierRepository courierRepository,
            RestaurantRepository restaurantRepository,
            DeliveryAssignmentRepository assignmentRepository
    ) {
        this.courierRepository = courierRepository;
        this.restaurantRepository = restaurantRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional(readOnly = true)
    public List<RestaurantCourier> listByRestaurant(String restaurantId, boolean activeOnly) {
        requireRestaurant(restaurantId);
        return activeOnly
                ? courierRepository.findByRestaurantIdAndActiveTrueOrderByNameAsc(restaurantId)
                : courierRepository.findByRestaurantIdOrderByNameAsc(restaurantId);
    }

    @Transactional
    public RestaurantCourier create(String restaurantId, RestaurantCourier courier) {
        requireRestaurant(restaurantId);
        validateRequiredFields(courier);
        courier.setId(null);
        courier.setRestaurantId(restaurantId);
        courier.setActive(true);
        return courierRepository.save(courier);
    }

    @Transactional
    public RestaurantCourier update(String courierId, RestaurantCourier changes, Boolean active) {
        RestaurantCourier courier = findById(courierId);
        if (changes.getName() != null) courier.setName(changes.getName().trim());
        if (changes.getPhone() != null) courier.setPhone(changes.getPhone().trim());
        if (changes.getVehicleType() != null) courier.setVehicleType(changes.getVehicleType().trim());
        if (changes.getLicensePlate() != null) courier.setLicensePlate(changes.getLicensePlate().trim());
        if (active != null) {
            if (!active && assignmentRepository.existsByCourierIdAndStatusIn(courierId, ACTIVE_DELIVERY_STATUSES)) {
                throw new IllegalStateException("Không thể ngừng hoạt động nhân viên đang có đơn giao");
            }
            courier.setActive(active);
        }
        validateRequiredFields(courier);
        return courierRepository.save(courier);
    }

    @Transactional
    public void deactivate(String courierId) {
        RestaurantCourier courier = findById(courierId);
        if (assignmentRepository.existsByCourierIdAndStatusIn(courierId, ACTIVE_DELIVERY_STATUSES)) {
            throw new IllegalStateException("Không thể ngừng hoạt động nhân viên đang có đơn giao");
        }
        courier.setActive(false);
        courierRepository.save(courier);
    }

    @Transactional(readOnly = true)
    public RestaurantCourier findById(String courierId) {
        return courierRepository.findById(courierId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên giao hàng: " + courierId));
    }

    private void requireRestaurant(String restaurantId) {
        if (restaurantId == null || restaurantId.isBlank()
                || !restaurantRepository.existsById(restaurantId)) {
            throw new IllegalArgumentException("Nhà hàng không tồn tại: " + restaurantId);
        }
    }

    private void validateRequiredFields(RestaurantCourier courier) {
        if (courier.getName() == null || courier.getName().isBlank()) {
            throw new IllegalArgumentException("Tên nhân viên giao hàng không được để trống");
        }
        if (courier.getPhone() == null || !courier.getPhone().trim().matches("^(0|\\+84)[0-9]{9}$")) {
            throw new IllegalArgumentException("Số điện thoại nhân viên giao hàng không hợp lệ");
        }
    }
}
