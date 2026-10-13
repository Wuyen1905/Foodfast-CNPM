package com.foodfast.controller;

import com.foodfast.dto.CreateCourierRequest;
import com.foodfast.dto.UpdateCourierRequest;
import com.foodfast.entity.RestaurantCourier;
import com.foodfast.service.RestaurantCourierService;
import com.foodfast.security.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RestaurantCourierController {

    private final RestaurantCourierService courierService;
    private final CurrentUserService currentUserService;

    public RestaurantCourierController(
            RestaurantCourierService courierService,
            CurrentUserService currentUserService
    ) {
        this.courierService = courierService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/restaurants/{restaurantId}/couriers")
    public List<RestaurantCourier> listCouriers(
            @PathVariable String restaurantId,
            @RequestParam(defaultValue = "false") boolean activeOnly
    ) {
        currentUserService.requireRestaurantAccess(restaurantId);
        return courierService.listByRestaurant(restaurantId, activeOnly);
    }

    @PostMapping("/restaurants/{restaurantId}/couriers")
    public ResponseEntity<RestaurantCourier> createCourier(
            @PathVariable String restaurantId,
            @RequestBody @Valid CreateCourierRequest request
    ) {
        currentUserService.requireRestaurantAccess(restaurantId);
        RestaurantCourier courier = new RestaurantCourier();
        courier.setName(request.name());
        courier.setPhone(request.phone());
        courier.setVehicleType(request.vehicleType());
        courier.setLicensePlate(request.licensePlate());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(courierService.create(restaurantId, courier));
    }

    @PatchMapping("/couriers/{courierId}")
    public RestaurantCourier updateCourier(
            @PathVariable String courierId,
            @RequestBody @Valid UpdateCourierRequest request
    ) {
        currentUserService.requireRestaurantAccess(courierService.findById(courierId).getRestaurantId());
        RestaurantCourier changes = new RestaurantCourier();
        changes.setName(request.name());
        changes.setPhone(request.phone());
        changes.setVehicleType(request.vehicleType());
        changes.setLicensePlate(request.licensePlate());
        return courierService.update(courierId, changes, request.active());
    }

    @DeleteMapping("/couriers/{courierId}")
    public ResponseEntity<Void> deactivateCourier(@PathVariable String courierId) {
        currentUserService.requireRestaurantAccess(courierService.findById(courierId).getRestaurantId());
        courierService.deactivate(courierId);
        return ResponseEntity.noContent().build();
    }
}
