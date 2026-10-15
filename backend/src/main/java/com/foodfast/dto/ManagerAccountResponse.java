package com.foodfast.dto;

import com.foodfast.entity.ManagerPermission;

import java.util.Set;

public record ManagerAccountResponse(
        String id,
        String username,
        String name,
        String email,
        String phone,
        String role,
        boolean active,
        Set<ManagerPermission> permissions,
        Long createdAt,
        Long lastLoginAt,
        String suspensionReason,
        Long accountStatusUpdatedAt
) {
}
