package com.foodfast.controller;

import com.foodfast.dto.CreateReviewRequest;
import com.foodfast.entity.Order;
import com.foodfast.entity.Review;
import com.foodfast.security.CurrentUserService;
import com.foodfast.service.OrderService;
import com.foodfast.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ReviewController {

    private final ReviewService reviewService;
    private final OrderService orderService;
    private final CurrentUserService currentUser;

    public ReviewController(
            ReviewService reviewService,
            OrderService orderService,
            CurrentUserService currentUser
    ) {
        this.reviewService = reviewService;
        this.orderService = orderService;
        this.currentUser = currentUser;
    }

    @PostMapping("/orders/{orderId}/review")
    public ResponseEntity<Review> createReview(
            @PathVariable String orderId,
            @RequestBody @Valid CreateReviewRequest request
    ) {
        requireAccessibleOrder(orderId);
        if (!currentUser.isCustomer()) {
            throw new com.foodfast.exception.ForbiddenOperationException(
                    "Chỉ khách hàng đã đặt đơn mới có thể đánh giá"
            );
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.createForOrder(
                        orderId,
                        currentUser.requireUser().getId(),
                        request
                ));
    }

    @GetMapping("/orders/{orderId}/review")
    public ResponseEntity<Review> getOrderReview(@PathVariable String orderId) {
        requireAccessibleOrder(orderId);
        return reviewService.findByOrder(orderId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/restaurants/{restaurantId}/reviews")
    public ResponseEntity<List<Review>> getRestaurantReviews(@PathVariable String restaurantId) {
        return ResponseEntity.ok(reviewService.findByRestaurant(restaurantId));
    }

    private Order requireAccessibleOrder(String orderId) {
        Order order = orderService.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng: " + orderId));
        currentUser.requireOrderAccess(order);
        return order;
    }
}
