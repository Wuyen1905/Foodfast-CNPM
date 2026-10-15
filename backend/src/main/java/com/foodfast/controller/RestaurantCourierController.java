package com.foodfast.controller;

import com.foodfast.dto.AdminCourierRequest;
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
@RequestMapping("/api/admin/couriers")
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

    @GetMapping
    public List<RestaurantCourier> listCouriers(
            @RequestParam(defaultValue = "false") boolean activeOnly
    ) {
        if (!currentUserService.isAdmin()) throw new com.foodfast.exception.ForbiddenOperationException("Chỉ Admin được quản lý shipper");
        return courierService.listAll(activeOnly);
    }

    @PostMapping
    public ResponseEntity<RestaurantCourier> createCourier(
            @RequestBody @Valid AdminCourierRequest request
    ) {
        if (!currentUserService.isAdmin()) throw new com.foodfast.exception.ForbiddenOperationException("Chỉ Admin được quản lý shipper");
        RestaurantCourier courier = new RestaurantCourier();
        courier.setName(request.name());
        courier.setPhone(request.phone());
        courier.setVehicleType(request.vehicleType());
        courier.setLicensePlate(request.licensePlate());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(courierService.create(request.restaurantId(), courier));
    }

    @PatchMapping("/{courierId}")
    public RestaurantCourier updateCourier(
            @PathVariable String courierId,
            @RequestBody @Valid UpdateCourierRequest request
    ) {
        if (!currentUserService.isAdmin()) throw new com.foodfast.exception.ForbiddenOperationException("Chỉ Admin được quản lý shipper");
        RestaurantCourier changes = new RestaurantCourier();
        changes.setName(request.name());
        changes.setPhone(request.phone());
        changes.setVehicleType(request.vehicleType());
        changes.setLicensePlate(request.licensePlate());
        return courierService.update(courierId, changes, request.active());
    }

    @DeleteMapping("/{courierId}")
    public ResponseEntity<Void> deactivateCourier(@PathVariable String courierId) {
        if (!currentUserService.isAdmin()) throw new com.foodfast.exception.ForbiddenOperationException("Chỉ Admin được quản lý shipper");
        courierService.deactivate(courierId);
        return ResponseEntity.noContent().build();
    }
}
