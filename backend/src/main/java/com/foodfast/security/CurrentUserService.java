package com.foodfast.security;

import com.foodfast.entity.Order;
import com.foodfast.entity.User;
import com.foodfast.exception.ForbiddenOperationException;
import com.foodfast.repository.UserRepository;
import com.foodfast.service.RestaurantIdentityPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User requireUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ForbiddenOperationException("Vui lòng đăng nhập để tiếp tục");
        }
        if (authentication.getDetails() instanceof User user) {
            return user;
        }
        return userRepository.findById(authentication.getName())
                .orElseThrow(() -> new ForbiddenOperationException("Phiên đăng nhập không còn hợp lệ"));
    }

    public String role() {
        return requireUser().getRole().trim().toUpperCase(Locale.ROOT);
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role());
    }

    public boolean isCustomer() {
        return "CUSTOMER".equals(role());
    }

    public boolean isRestaurant() {
        return "RESTAURANT".equals(role());
    }

    public String requireRestaurantId() {
        User user = requireUser();
        if (!isRestaurant() || user.getRestaurantId() == null || user.getRestaurantId().isBlank()) {
            throw new ForbiddenOperationException("Tài khoản không thuộc nhà hàng nào");
        }
        return RestaurantIdentityPolicy.canonicalize(user.getRestaurantId());
    }

    public void requireRestaurantAccess(String restaurantId) {
        if (isAdmin()) {
            return;
        }
        if (!isRestaurant() || !RestaurantIdentityPolicy.refersToSameRestaurant(
                requireRestaurantId(), restaurantId)) {
            throw new ForbiddenOperationException("Bạn không được truy cập dữ liệu của nhà hàng này");
        }
    }

    public void requireOrderAccess(Order order) {
        User user = requireUser();
        if (isAdmin()) {
            return;
        }
        if (isCustomer() && user.getId().equals(order.getUserId())) {
            return;
        }
        if (isRestaurant() && RestaurantIdentityPolicy.refersToSameRestaurant(
                user.getRestaurantId(), order.getRestaurantId())) {
            return;
        }
        throw new ForbiddenOperationException("Bạn không được truy cập đơn hàng này");
    }

    public String businessActorId() {
        User user = requireUser();
        return isRestaurant() ? requireRestaurantId() : user.getId();
    }
}
