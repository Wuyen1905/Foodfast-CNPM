package com.foodfast.controller;

import com.foodfast.dto.ManagerAccountRequest;
import com.foodfast.dto.ManagerAccountResponse;
import com.foodfast.dto.ManagerStatusRequest;
import com.foodfast.security.CurrentUserService;
import com.foodfast.service.ManagerAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/managers")
public class ManagerAccountController {

    private final ManagerAccountService managerAccountService;
    private final CurrentUserService currentUserService;

    public ManagerAccountController(
            ManagerAccountService managerAccountService,
            CurrentUserService currentUserService
    ) {
        this.managerAccountService = managerAccountService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<ManagerAccountResponse> list() {
        return managerAccountService.list();
    }

    @PostMapping
    public ResponseEntity<ManagerAccountResponse> create(
            @Valid @RequestBody ManagerAccountRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(managerAccountService.create(request, currentUserService.requireUser()));
    }

    @PutMapping("/{managerId}")
    public ManagerAccountResponse update(
            @PathVariable String managerId,
            @Valid @RequestBody ManagerAccountRequest request
    ) {
        return managerAccountService.update(managerId, request, currentUserService.requireUser());
    }

    @PatchMapping("/{managerId}/status")
    public ManagerAccountResponse setStatus(
            @PathVariable String managerId,
            @Valid @RequestBody ManagerStatusRequest request
    ) {
        return managerAccountService.setActive(
                managerId, request.active(), request.reason(), currentUserService.requireUser()
        );
    }
}
