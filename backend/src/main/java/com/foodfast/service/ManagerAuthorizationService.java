package com.foodfast.service;

import com.foodfast.entity.ManagerPermission;
import com.foodfast.entity.User;
import com.foodfast.exception.ForbiddenOperationException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

@Service
public class ManagerAuthorizationService {

    public User requireAny(User user, ManagerPermission... required) {
        if (user == null || !"manager".equalsIgnoreCase(user.getRole()) || !user.isAccountActive()) {
            throw new ForbiddenOperationException("Tài khoản nhân viên quản lý không hợp lệ hoặc đã bị khóa");
        }
        Set<ManagerPermission> assigned = parse(user.getManagerPermissions());
        if (Arrays.stream(required).noneMatch(assigned::contains)) {
            throw new ForbiddenOperationException("Bạn chưa được giao quyền thực hiện nghiệp vụ này");
        }
        return user;
    }

    public User require(User user, ManagerPermission permission) {
        return requireAny(user, permission);
    }

    public boolean has(User user, ManagerPermission permission) {
        return user != null && "manager".equalsIgnoreCase(user.getRole())
                && user.isAccountActive() && parse(user.getManagerPermissions()).contains(permission);
    }

    private Set<ManagerPermission> parse(String raw) {
        Set<ManagerPermission> permissions = EnumSet.noneOf(ManagerPermission.class);
        if (raw == null || raw.isBlank()) return permissions;
        for (String value : raw.split(",")) {
            try {
                permissions.add(ManagerPermission.valueOf(value.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                // Quyền lạ trong dữ liệu cũ không được tự động cấp.
            }
        }
        return permissions;
    }
}
