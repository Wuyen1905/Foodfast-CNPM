package com.foodfast.service;

import com.foodfast.dto.ComplaintResponse;
import com.foodfast.dto.CreateComplaintRequest;
import com.foodfast.dto.ReviewComplaintRequest;
import com.foodfast.entity.Complaint;
import com.foodfast.entity.ComplaintAction;
import com.foodfast.entity.ComplaintCategory;
import com.foodfast.entity.ComplaintHistory;
import com.foodfast.entity.ComplaintStatus;
import com.foodfast.entity.Order;
import com.foodfast.entity.OrderStatus;
import com.foodfast.exception.ForbiddenOperationException;
import com.foodfast.repository.ComplaintHistoryRepository;
import com.foodfast.repository.ComplaintRepository;
import com.foodfast.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
public class ComplaintService {
    private final ComplaintRepository complaintRepository;
    private final ComplaintHistoryRepository historyRepository;
    private final OrderRepository orderRepository;

    public ComplaintService(
            ComplaintRepository complaintRepository,
            ComplaintHistoryRepository historyRepository,
            OrderRepository orderRepository
    ) {
        this.complaintRepository = complaintRepository;
        this.historyRepository = historyRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public ComplaintResponse create(String authenticatedCustomerId, CreateComplaintRequest request) {
        Order order = orderRepository.findById(request.orderId().trim())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));
        if (!StringUtils.hasText(order.getUserId())
                || !order.getUserId().equals(authenticatedCustomerId.trim())) {
            throw new ForbiddenOperationException("Bạn chỉ có thể báo cáo sự cố cho đơn hàng của chính mình");
        }
        validateEligibleStatus(order, request.category());
        if (complaintRepository.existsByOrderId(order.getId())) {
            throw new IllegalStateException("Mỗi đơn hàng chỉ được tạo một báo cáo sự cố");
        }

        List<String> evidence = sanitizeEvidence(request.evidenceUrls());
        if ((request.category() == ComplaintCategory.WRONG_ITEM
                || request.category() == ComplaintCategory.DAMAGED_ITEM) && evidence.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng cung cấp ít nhất một ảnh bằng chứng");
        }

        Complaint complaint = new Complaint();
        complaint.setOrderId(order.getId());
        complaint.setRestaurantId(order.getRestaurantId());
        complaint.setCustomerId(authenticatedCustomerId.trim());
        complaint.setCategory(request.category());
        complaint.setDescription(request.description().trim());
        complaint.setEvidenceUrls(evidence);
        Complaint saved = complaintRepository.save(complaint);
        appendHistory(saved, "CREATED", "CUSTOMER", authenticatedCustomerId, null,
                ComplaintStatus.OPEN, request.description());
        return response(saved);
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponse> list(String viewerRole, String viewerId, ComplaintStatus status) {
        String role = normalizeRole(viewerRole);
        if (!StringUtils.hasText(viewerId)) {
            throw new IllegalArgumentException("Thiếu mã người xem báo cáo sự cố");
        }
        List<Complaint> complaints = switch (role) {
            case "CUSTOMER" -> status == null
                    ? complaintRepository.findByCustomerIdOrderByCreatedAtDesc(viewerId.trim())
                    : complaintRepository.findByCustomerIdAndStatusOrderByCreatedAtDesc(viewerId.trim(), status);
            case "RESTAURANT" -> status == null
                    ? complaintRepository.findByRestaurantIdOrderByCreatedAtDesc(viewerId.trim())
                    : complaintRepository.findByRestaurantIdAndStatusOrderByCreatedAtDesc(viewerId.trim(), status);
            case "ADMIN" -> status == null
                    ? complaintRepository.findAllByOrderByCreatedAtDesc()
                    : complaintRepository.findByStatusOrderByCreatedAtDesc(status);
            default -> throw new ForbiddenOperationException("Vai trò không được xem danh sách báo cáo sự cố");
        };
        return complaints.stream().map(this::response).toList();
    }

    @Transactional
    public ComplaintResponse review(
            String complaintId,
            String authenticatedActorRole,
            String authenticatedActorId,
            ReviewComplaintRequest request
    ) {
        Complaint complaint = find(complaintId);
        String role = normalizeRole(authenticatedActorRole);
        authorizeHandler(complaint, role, authenticatedActorId);
        if (complaint.getStatus() == ComplaintStatus.RESOLVED
                || complaint.getStatus() == ComplaintStatus.REJECTED) {
            throw new IllegalStateException("Báo cáo sự cố đã có kết quả cuối cùng");
        }

        ComplaintStatus before = complaint.getStatus();
        String note = trimToNull(request.note());
        if (request.action() == ComplaintAction.START_REVIEW) {
            if (complaint.getStatus() != ComplaintStatus.OPEN) {
                throw new IllegalStateException("Báo cáo sự cố đã được tiếp nhận trước đó");
            }
            complaint.setStatus(ComplaintStatus.REVIEWING);
        } else if (request.action() == ComplaintAction.REJECT) {
            requireResolution(note);
            complaint.setStatus(ComplaintStatus.REJECTED);
            complaint.setResolution(note);
            complaint.setResolvedAt(System.currentTimeMillis());
        } else if (request.action() == ComplaintAction.RESOLVE) {
            requireResolution(note);
            complaint.setStatus(ComplaintStatus.RESOLVED);
            complaint.setResolution(note);
            complaint.setResolvedAt(System.currentTimeMillis());
        }

        complaint.setHandlerRole(role);
        complaint.setHandlerId(authenticatedActorId.trim());
        Complaint saved = complaintRepository.save(complaint);
        appendHistory(saved, request.action().name(), role, authenticatedActorId, before,
                saved.getStatus(), note);
        return response(saved);
    }

    private void validateEligibleStatus(Order order, ComplaintCategory category) {
        OrderStatus status = order.getStatusEnum();
        if (category == ComplaintCategory.LATE_DELIVERY) {
            if (status != OrderStatus.DELIVERING && status != OrderStatus.DELIVERED) {
                throw new IllegalStateException("Chỉ phản ánh giao trễ khi đơn đang giao hoặc đã giao");
            }
            return;
        }
        if (status != OrderStatus.DELIVERED) {
            throw new IllegalStateException("Vấn đề món ăn chỉ được báo sau khi đơn đã giao");
        }
    }

    private List<String> sanitizeEvidence(List<String> values) {
        if (values == null) return List.of();
        return values.stream().filter(StringUtils::hasText).map(String::trim).map(value -> {
            boolean uploadedEvidence = value.startsWith("/api/complaints/evidence/");
            boolean externalEvidence = value.startsWith("https://") || value.startsWith("http://");
            if ((!uploadedEvidence && !externalEvidence) || value.length() > 500) {
                throw new IllegalArgumentException("Đường dẫn bằng chứng không hợp lệ");
            }
            return value;
        }).distinct().limit(5).toList();
    }

    private void authorizeHandler(Complaint complaint, String role, String actorId) {
        if (!StringUtils.hasText(actorId)) throw new IllegalArgumentException("Thiếu mã người xử lý");
        if ("ADMIN".equals(role)) return;
        if (!"RESTAURANT".equals(role)
                || !complaint.getRestaurantId().equalsIgnoreCase(actorId.trim())) {
            throw new ForbiddenOperationException("Bạn không được xử lý báo cáo sự cố này");
        }
    }

    private String normalizeRole(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private void requireResolution(String note) {
        if (note == null || note.length() < 10) {
            throw new IllegalArgumentException("Kết quả xử lý phải có ít nhất 10 ký tự");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Complaint find(String id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy báo cáo sự cố"));
    }

    private void appendHistory(Complaint complaint, String action, String actorRole, String actorId,
                               ComplaintStatus from, ComplaintStatus to, String note) {
        ComplaintHistory history = new ComplaintHistory();
        history.setComplaintId(complaint.getId());
        history.setAction(action);
        history.setActorRole(actorRole);
        history.setActorId(actorId.trim());
        history.setFromStatus(from == null ? null : from.name());
        history.setToStatus(to.name());
        history.setNote(note);
        historyRepository.save(history);
    }

    private ComplaintResponse response(Complaint complaint) {
        return ComplaintResponse.from(complaint,
                historyRepository.findByComplaintIdOrderByCreatedAtAsc(complaint.getId()));
    }
}
