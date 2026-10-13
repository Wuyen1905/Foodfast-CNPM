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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private RestaurantRepository restaurantRepository;

    private ReviewService service;

    @BeforeEach
    void setUp() {
        service = new ReviewService(reviewRepository, orderRepository, restaurantRepository);
    }

    @Test
    void ownerCanReviewDeliveredOrderAndRestaurantRatingIsRecalculated() {
        Order order = order(OrderStatus.DELIVERED);
        Restaurant restaurant = restaurant();
        when(orderRepository.findById("ORDER-1")).thenReturn(Optional.of(order));
        when(reviewRepository.findByOrderId("ORDER-1")).thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewRepository.averageRating("restaurant_2")).thenReturn(4.25);
        when(restaurantRepository.findById("restaurant_2")).thenReturn(Optional.of(restaurant));

        Review review = service.createForOrder(
                "ORDER-1", "customer-1", request(5, "Món ngon")
        );

        assertThat(review.getRating()).isEqualTo(5);
        assertThat(review.getComment()).isEqualTo("Món ngon");
        assertThat(review.getRestaurantId()).isEqualTo("restaurant_2");
        assertThat(restaurant.getRating()).isEqualTo(4.3);
        verify(restaurantRepository).save(restaurant);
    }

    @Test
    void anotherCustomerCannotReviewTheOrder() {
        when(orderRepository.findById("ORDER-1")).thenReturn(Optional.of(order(OrderStatus.DELIVERED)));

        assertThatThrownBy(() -> service.createForOrder(
                "ORDER-1",
                "customer-2",
                request(5, null)
        )).isInstanceOf(ForbiddenOperationException.class);
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void orderMustBeDeliveredBeforeReview() {
        when(orderRepository.findById("ORDER-1")).thenReturn(Optional.of(order(OrderStatus.DELIVERING)));

        assertThatThrownBy(() -> service.createForOrder(
                "ORDER-1",
                "customer-1",
                request(4, null)
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sau khi đơn đã giao");
    }

    @Test
    void oneOrderCanOnlyBeReviewedOnce() {
        when(orderRepository.findById("ORDER-1")).thenReturn(Optional.of(order(OrderStatus.DELIVERED)));
        when(reviewRepository.findByOrderId("ORDER-1")).thenReturn(Optional.of(new Review()));

        assertThatThrownBy(() -> service.createForOrder(
                "ORDER-1",
                "customer-1",
                request(4, null)
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("đã được đánh giá");
    }

    private Order order(OrderStatus status) {
        Order order = new Order();
        order.setId("ORDER-1");
        order.setUserId("customer-1");
        order.setRestaurantId("aloha");
        order.setStatus(status);
        return order;
    }

    private Restaurant restaurant() {
        Restaurant restaurant = new Restaurant();
        restaurant.setId("restaurant_2");
        restaurant.setOwnerId("owner_aloha");
        restaurant.setRating(0.0);
        return restaurant;
    }

    private CreateReviewRequest request(int rating, String comment) {
        CreateReviewRequest request = new CreateReviewRequest();
        request.rating = rating;
        request.comment = comment;
        return request;
    }
}
