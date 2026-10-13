package com.foodfast.service;

import com.foodfast.entity.AdminAuditLog;
import com.foodfast.entity.User;
import com.foodfast.repository.AdminAuditLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminAuditLogService {

    private final AdminAuditLogRepository repository;

    public AdminAuditLogService(AdminAuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public AdminAuditLog record(
            User admin,
            String action,
            String targetType,
            String targetId,
            String targetName,
            String details,
            String severity
    ) {
        AdminAuditLog log = new AdminAuditLog();
        log.setTimestamp(System.currentTimeMillis());
        log.setAdminId(admin.getId());
        log.setAdminName(admin.getName() == null || admin.getName().isBlank()
                ? admin.getUsername() : admin.getName());
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setTargetName(targetName);
        log.setDetails(details);
        log.setSeverity(severity);
        return repository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AdminAuditLog> recent(int requestedLimit) {
        int limit = Math.max(1, Math.min(requestedLimit, 200));
        return repository.findAllByOrderByTimestampDesc(PageRequest.of(0, limit));
    }
}
