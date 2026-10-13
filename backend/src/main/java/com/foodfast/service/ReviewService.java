package com.foodfast.service;

import com.foodfast.dto.CreateReviewRequest;
import com.foodfast.entity.Order;
import com.foodfast.entity.OrderStatus;
import com.foodfast.entity.Restaurant;
import com.foodfast.entity.Review;
import com.foodfast.exception.ForbiddenOperationException;
import com.foodfast.repository.OrderRepository;
import com.foodfast.repository.RestaurantRepository;
import com.foodfast.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;

    public ReviewService(
            ReviewRepository reviewRepository,
            OrderRepository orderRepository,
            RestaurantRepository restaurantRepository
    ) {
        this.reviewRepository = reviewRepository;
        this.orderRepository = orderRepository;
        this.restaurantRepository = restaurantRepository;
    }

    @Transactional
    public Review createForOrder(
            String orderId,
            String authenticatedUserId,
            CreateReviewRequest request
    ) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng: " + orderId));
        if (request == null || !StringUtils.hasText(authenticatedUserId)) {
            throw new IllegalArgumentException("Thiếu tài khoản đánh giá");
        }
        if (!authenticatedUserId.trim().equals(order.getUserId())) {
            throw new ForbiddenOperationException("Bạn chỉ được đánh giá đơn hàng của chính mình");
        }
        if (order.getStatusEnum() != OrderStatus.DELIVERED) {
            throw new IllegalStateException("Chỉ có thể đánh giá sau khi đơn đã giao");
        }
        if (request.rating < 1 || request.rating > 5) {
            throw new IllegalArgumentException("Điểm đánh giá phải từ 1 đến 5");
        }
        if (reviewRepository.findByOrderId(orderId).isPresent()) {
            throw new IllegalStateException("Đơn hàng này đã được đánh giá");
        }

        Review review = new Review();
        review.setOrderId(orderId);
        review.setUserId(authenticatedUserId.trim());
        review.setRestaurantId(RestaurantIdentityPolicy.canonicalize(order.getRestaurantId()));
        review.setRating(request.rating);
        review.setComment(StringUtils.hasText(request.comment) ? request.comment.trim() : null);
        Review saved = reviewRepository.save(review);
        refreshRestaurantRating(saved.getRestaurantId());
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<Review> findByOrder(String orderId) {
        return reviewRepository.findByOrderId(orderId);
    }

    @Transactional(readOnly = true)
    public List<Review> findByRestaurant(String restaurantId) {
        return reviewRepository.findByRestaurantIdOrderByCreatedAtDesc(
                RestaurantIdentityPolicy.canonicalize(restaurantId)
        );
    }

    private void refreshRestaurantRating(String restaurantId) {
        Double average = reviewRepository.averageRating(restaurantId);
        Restaurant restaurant = restaurantRepository.findById(restaurantId).orElse(null);
        if (restaurant != null && average != null) {
            restaurant.setRating(Math.round(average * 10.0) / 10.0);
            restaurantRepository.save(restaurant);
        }
    }
}
