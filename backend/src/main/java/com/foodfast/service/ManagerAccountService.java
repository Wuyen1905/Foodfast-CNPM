package com.foodfast.service;

import com.foodfast.dto.ManagerAccountRequest;
import com.foodfast.dto.ManagerAccountResponse;
import com.foodfast.entity.ManagerPermission;
import com.foodfast.entity.User;
import com.foodfast.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ManagerAccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminAuditLogService auditLogService;

    public ManagerAccountService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AdminAuditLogService auditLogService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<ManagerAccountResponse> list() {
        return userRepository.findAll().stream()
                .filter(user -> "manager".equalsIgnoreCase(user.getRole()))
                .sorted(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ManagerAccountResponse create(ManagerAccountRequest request, User admin) {
        if (!StringUtils.hasText(request.password())) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu cho tài khoản quản lý");
        }

        User manager = new User();
        manager.setId("MGR-" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 16).toUpperCase(Locale.ROOT));
        applyEditableFields(manager, request, true);
        manager.setRole("manager");
        manager.setAccountStatus("active");
        manager.setOrderCount(0);
        manager.setCreatedAt(System.currentTimeMillis());
        User saved = userRepository.saveAndFlush(manager);

        auditLogService.record(admin, "manager_created", "manager", saved.getId(), saved.getName(),
                "Tạo tài khoản quản lý " + saved.getUsername(), "info");
        return toResponse(saved);
    }

    @Transactional
    public ManagerAccountResponse update(String managerId, ManagerAccountRequest request, User admin) {
        User manager = requireManager(managerId);
        applyEditableFields(manager, request, false);
        User saved = userRepository.saveAndFlush(manager);

        auditLogService.record(admin, "manager_updated", "manager", saved.getId(), saved.getName(),
                "Cập nhật hồ sơ và quyền quản lý", "info");
        return toResponse(saved);
    }

    @Transactional
    public ManagerAccountResponse setActive(
            String managerId,
            boolean active,
            String reason,
            User admin
    ) {
        User manager = requireManager(managerId);
        String normalizedReason = reason == null ? "" : reason.trim();
        if (!active && normalizedReason.length() < 5) {
            throw new IllegalArgumentException("Vui lòng nhập lý do khóa tài khoản từ 5 ký tự");
        }

        manager.setAccountStatus(active ? "active" : "suspended");
        manager.setSuspensionReason(active ? null : normalizedReason);
        manager.setAccountStatusUpdatedAt(System.currentTimeMillis());
        User saved = userRepository.save(manager);

        auditLogService.record(
                admin,
                active ? "manager_activated" : "manager_suspended",
                "manager",
                saved.getId(),
                saved.getName(),
                active ? "Kích hoạt lại tài khoản quản lý" : normalizedReason,
                active ? "info" : "warning"
        );
        return toResponse(saved);
    }

    private User requireManager(String managerId) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên quản lý"));
        if (!"manager".equalsIgnoreCase(manager.getRole())) {
            throw new IllegalArgumentException("Tài khoản không thuộc vai trò quản lý");
        }
        return manager;
    }

    private void applyEditableFields(User manager, ManagerAccountRequest request, boolean creating) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        String name = request.name().trim().replaceAll("\\s+", " ");
        String email = normalizeNullable(request.email(), true);
        String phone = normalizeNullable(request.phone(), false);

        userRepository.findByUsernameIgnoreCase(username)
                .filter(existing -> !existing.getId().equals(manager.getId()))
                .ifPresent(existing -> { throw new IllegalArgumentException("Tên đăng nhập đã tồn tại"); });
        if (email != null) {
            userRepository.findByEmailIgnoreCase(email)
                    .filter(existing -> !existing.getId().equals(manager.getId()))
                    .ifPresent(existing -> { throw new IllegalArgumentException("Email đã được sử dụng"); });
        }
        if (phone != null) {
            userRepository.findByPhone(phone)
                    .filter(existing -> !existing.getId().equals(manager.getId()))
                    .ifPresent(existing -> { throw new IllegalArgumentException("Số điện thoại đã được sử dụng"); });
        }

        manager.setUsername(username);
        manager.setName(name);
        manager.setEmail(email);
        manager.setPhone(phone);
        manager.setManagerPermissions(serializePermissions(request.permissions()));
        if (creating || StringUtils.hasText(request.password())) {
            manager.setPassword(passwordEncoder.encode(request.password()));
        }
    }

    private String normalizeNullable(String value, boolean lowercase) {
        if (!StringUtils.hasText(value)) return null;
        String normalized = value.trim();
        return lowercase ? normalized.toLowerCase(Locale.ROOT) : normalized;
    }

    private String serializePermissions(Set<ManagerPermission> permissions) {
        if (permissions == null || permissions.isEmpty()) return "";
        return permissions.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
    }

    private Set<ManagerPermission> parsePermissions(String value) {
        if (!StringUtils.hasText(value)) return Set.of();
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(ManagerPermission::valueOf)
                .sorted()
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private ManagerAccountResponse toResponse(User user) {
        return new ManagerAccountResponse(
                user.getId(), user.getUsername(), user.getName(), user.getEmail(), user.getPhone(),
                user.getRole(), user.isAccountActive(), parsePermissions(user.getManagerPermissions()),
                user.getCreatedAt(), user.getLastLoginAt(), user.getSuspensionReason(),
                user.getAccountStatusUpdatedAt()
        );
    }
}
