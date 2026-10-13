package com.foodfast.controller;

import com.foodfast.dto.DeliveryAddressRequest;
import com.foodfast.entity.DeliveryAddress;
import com.foodfast.security.CurrentUserService;
import com.foodfast.service.DeliveryAddressService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
public class DeliveryAddressController {

    private final DeliveryAddressService addressService;
    private final CurrentUserService currentUserService;

    public DeliveryAddressController(DeliveryAddressService addressService, CurrentUserService currentUserService) {
        this.addressService = addressService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<DeliveryAddress> list() {
        return addressService.list(currentUserService.requireUser().getId());
    }

    @PostMapping
    public ResponseEntity<DeliveryAddress> create(@Valid @RequestBody DeliveryAddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                addressService.create(currentUserService.requireUser().getId(), request)
        );
    }

    @PutMapping("/{addressId}")
    public DeliveryAddress update(
            @PathVariable String addressId,
            @Valid @RequestBody DeliveryAddressRequest request
    ) {
        return addressService.update(currentUserService.requireUser().getId(), addressId, request);
    }

    @PutMapping("/{addressId}/default")
    public DeliveryAddress makeDefault(@PathVariable String addressId) {
        return addressService.makeDefault(currentUserService.requireUser().getId(), addressId);
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> delete(@PathVariable String addressId) {
        addressService.delete(currentUserService.requireUser().getId(), addressId);
        return ResponseEntity.noContent().build();
    }
}
