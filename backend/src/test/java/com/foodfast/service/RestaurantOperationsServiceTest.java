package com.foodfast.service;

import com.foodfast.dto.UpdateRestaurantOperationsRequest;
import com.foodfast.entity.Restaurant;
import com.foodfast.exception.ForbiddenOperationException;
import com.foodfast.repository.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantOperationsServiceTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    private RestaurantService service;

    @BeforeEach
    void setUp() {
        service = new RestaurantService(restaurantRepository);
    }

    @Test
    void ownerCanPauseAndResumeOrdersWithReason() {
        Restaurant restaurant = restaurant();
        when(restaurantRepository.findById("restaurant_2")).thenReturn(Optional.of(restaurant));
        when(restaurantRepository.save(restaurant)).thenReturn(restaurant);

        UpdateRestaurantOperationsRequest pause = request();
        pause.acceptingOrders = false;
        pause.pauseReason = "Bếp đang quá tải";
        Restaurant paused = service.updateOperations("aloha", "owner_aloha", false, pause);

        assertThat(paused.getAcceptingOrders()).isFalse();
        assertThat(paused.getPauseReason()).isEqualTo("Bếp đang quá tải");

        UpdateRestaurantOperationsRequest resume = request();
        resume.acceptingOrders = true;
        Restaurant resumed = service.updateOperations("restaurant_2", "owner_aloha", false, resume);

        assertThat(resumed.getAcceptingOrders()).isTrue();
        assertThat(resumed.getPauseReason()).isNull();
    }

    @Test
    void anotherUserCannotChangeRestaurantOperations() {
        Restaurant restaurant = restaurant();
        when(restaurantRepository.findById("restaurant_2")).thenReturn(Optional.of(restaurant));
        UpdateRestaurantOperationsRequest request = request();
        request.acceptingOrders = false;
        request.pauseReason = "Thử thay đổi trái phép";

        assertThatThrownBy(() -> service.updateOperations("restaurant_2", "u3", false, request))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(restaurantRepository, never()).save(restaurant);
    }

    @Test
    void pausingRequiresMeaningfulReason() {
        Restaurant restaurant = restaurant();
        when(restaurantRepository.findById("restaurant_2")).thenReturn(Optional.of(restaurant));
        UpdateRestaurantOperationsRequest request = request();
        request.acceptingOrders = false;
        request.pauseReason = "Bận";

        assertThatThrownBy(() -> service.updateOperations(
                "restaurant_2", "owner_aloha", false, request
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ít nhất 5 ký tự");
    }

    @Test
    void regularHoursAreOpenInsideAndClosedOutside() {
        Restaurant restaurant = restaurant();
        restaurant.setOpeningTime("08:00");
        restaurant.setClosingTime("22:00");

        assertThat(restaurant.isOpenAt(LocalTime.of(12, 0))).isTrue();
        assertThat(restaurant.isOpenAt(LocalTime.of(22, 0))).isFalse();
        assertThat(restaurant.isOpenAt(LocalTime.of(7, 59))).isFalse();
    }

    @Test
    void overnightHoursWorkAcrossMidnight() {
        Restaurant restaurant = restaurant();
        restaurant.setOpeningTime("18:00");
        restaurant.setClosingTime("02:00");

        assertThat(restaurant.isOpenAt(LocalTime.of(23, 0))).isTrue();
        assertThat(restaurant.isOpenAt(LocalTime.of(1, 30))).isTrue();
        assertThat(restaurant.isOpenAt(LocalTime.of(12, 0))).isFalse();
    }

    @Test
    void orderingIsBlockedWhenRestaurantPauses() {
        Restaurant restaurant = restaurant();
        restaurant.setAcceptingOrders(false);
        restaurant.setPauseReason("Tạm hết nguyên liệu");
        when(restaurantRepository.findById("restaurant_2")).thenReturn(Optional.of(restaurant));

        assertThatThrownBy(() -> service.requireAvailableForOrdering(
                "restaurant_2",
                LocalTime.of(12, 0)
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Tạm hết nguyên liệu");
    }

    @Test
    void orderingIsBlockedOutsideOperatingHours() {
        Restaurant restaurant = restaurant();
        when(restaurantRepository.findById("restaurant_2")).thenReturn(Optional.of(restaurant));

        assertThatThrownBy(() -> service.requireAvailableForOrdering(
                "restaurant_2",
                LocalTime.of(23, 0)
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("08:00–22:00");
    }

    private Restaurant restaurant() {
        Restaurant restaurant = new Restaurant();
        restaurant.setId("restaurant_2");
        restaurant.setName("Nhà hàng Aloha");
        restaurant.setOwnerId("owner_aloha");
        restaurant.setIsActive(true);
        restaurant.setAcceptingOrders(true);
        restaurant.setOpeningTime("08:00");
        restaurant.setClosingTime("22:00");
        return restaurant;
    }

    private UpdateRestaurantOperationsRequest request() {
        return new UpdateRestaurantOperationsRequest();
    }
}
