package com.foodfast.controller;

import com.foodfast.entity.Restaurant;
import com.foodfast.dto.UpdateRestaurantOperationsRequest;
import jakarta.validation.Valid;
import com.foodfast.service.RestaurantService;
import com.foodfast.security.CurrentUserService;
import com.foodfast.exception.ForbiddenOperationException;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/restaurants")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://192.168.0.100:5173",
        "http://192.168.0.100:5174",
        "http://192.168.0.100:5175",
        "http://localhost:8081"
})
public class RestaurantController {

    private final RestaurantService restaurantService;
    private final CurrentUserService currentUserService;

    public RestaurantController(RestaurantService restaurantService, CurrentUserService currentUserService) {
        this.restaurantService = restaurantService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<List<Restaurant>> getRestaurants(@RequestParam(required = false) String category) {
        List<Restaurant> restaurants;
        if (StringUtils.hasText(category)) {
            restaurants = restaurantService.findByCategory(category);
        } else {
            restaurants = restaurantService.getAllActiveRestaurants();
        }
        return ResponseEntity.ok(restaurants);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Restaurant> getRestaurant(@PathVariable String id) {
        Optional<Restaurant> restaurant = restaurantService.findById(id);
        return restaurant.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<Restaurant> getRestaurantByOwner(@PathVariable String ownerId) {
        String securedOwnerId;
        if (currentUserService.isAdmin()) {
            securedOwnerId = ownerId;
        } else if (currentUserService.isRestaurant()) {
            securedOwnerId = currentUserService.requireUser().getId();
        } else {
            throw new ForbiddenOperationException("Bạn không được xem hồ sơ chủ nhà hàng");
        }
        Optional<Restaurant> restaurant = restaurantService.findByOwnerId(securedOwnerId);
        return restaurant.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/operations")
    public ResponseEntity<Restaurant> updateOperations(
            @PathVariable String id,
            @RequestBody @Valid UpdateRestaurantOperationsRequest request
    ) {
        currentUserService.requireRestaurantAccess(id);
        return ResponseEntity.ok(restaurantService.updateOperations(
                id,
                currentUserService.requireUser().getId(),
                currentUserService.isAdmin(),
                request
        ));
    }
}

