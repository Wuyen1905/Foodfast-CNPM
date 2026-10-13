package com.foodfast.service;

import com.foodfast.dto.CreateSupportContactRequest;
import com.foodfast.dto.SupportChatRequest;
import com.foodfast.dto.SupportMessageResponse;
import com.foodfast.dto.SupportTicketResponse;
import com.foodfast.entity.Order;
import com.foodfast.entity.SupportDepartment;
import com.foodfast.entity.SupportIssueType;
import com.foodfast.entity.SupportPriority;
import com.foodfast.entity.SupportTicket;
import com.foodfast.entity.SupportTicketHistory;
import com.foodfast.entity.SupportTicketMessage;
import com.foodfast.entity.SupportTicketStatus;
import com.foodfast.entity.SupportTicketTarget;
import com.foodfast.entity.User;
import com.foodfast.repository.SupportTicketHistoryRepository;
import com.foodfast.repository.SupportTicketMessageRepository;
import com.foodfast.repository.SupportTicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class SupportTicketService {

    private final SupportTicketRepository repository;
    private final SupportTicketHistoryRepository historyRepository;
    private final SupportTicketMessageRepository messageRepository;

    public SupportTicketService(
            SupportTicketRepository repository,
            SupportTicketHistoryRepository historyRepository,
            SupportTicketMessageRepository messageRepository
    ) {
        this.repository = repository;
        this.historyRepository = historyRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional
    public SupportTicket create(
            SupportChatRequest request,
            String authenticatedCustomerId,
            String authenticatedCustomerName,
            String authenticatedCustomerPhone,
            String intent,
            SupportTicketTarget target,
            Order verifiedOrder
    ) {
        SupportTicket ticket = createTicket(
                authenticatedCustomerId,
                authenticatedCustomerName,
                authenticatedCustomerPhone,
                intent,
                request.message(),
                target,
                verifiedOrder
        );
        if ("REFUND_OR_PAYMENT".equalsIgnoreCase(intent)) {
            ticket.setDepartment(SupportDepartment.FINANCE);
            ticket.setPriority(SupportPriority.URGENT);
            ticket.recalculateResponseDeadline();
        }
        SupportTicket saved = repository.save(ticket);
        appendCreatedHistory(saved, authenticatedCustomerId);
        appendMessage(saved, "CUSTOMER", authenticatedCustomerId, authenticatedCustomerName,
                saved.getMessage(), saved.getCreatedAt());
        return saved;
    }

    @Transactional
    public SupportTicketResponse createContact(
            CreateSupportContactRequest request,
            User customer,
            Order verifiedOrder
    ) {
        SupportTicketTarget target = targetFor(request.issueType());
        SupportTicket ticket = createTicket(
                customer.getId(), customer.getName(), customer.getPhone(), request.issueType().name(),
                request.message(), target, verifiedOrder
        );
        configureIssue(ticket, request.issueType());
        SupportTicket saved = repository.save(ticket);
        appendCreatedHistory(saved, customer.getId());
        long messageTime = saved.getCreatedAt();
        if (StringUtils.hasText(request.aiTranscript())) {
            appendMessage(saved, "SYSTEM", "assistant", "Trợ lý FoodFast",
                    "Tóm tắt trao đổi trước đó với trợ lý:\n" + request.aiTranscript().trim(), messageTime);
            messageTime++;
        }
        appendMessage(saved, "CUSTOMER", customer.getId(), customer.getName(),
                saved.getMessage(), messageTime);
        return response(saved);
    }

    @Transactional(readOnly = true)
    public List<SupportTicket> list(
            SupportTicketTarget target,
            String restaurantId,
            SupportTicketStatus status
    ) {
        if (target == SupportTicketTarget.RESTAURANT) {
            if (restaurantId == null || restaurantId.isBlank()) {
                throw new IllegalArgumentException("Phải cung cấp restaurantId cho phiếu của nhà hàng");
            }
            return repository.findByRestaurantIdAndTargetAndStatusOrderByCreatedAtDesc(
                    restaurantId.trim(), SupportTicketTarget.RESTAURANT, status
            );
        }
        return repository.findByTargetAndStatusOrderByCreatedAtDesc(target, status);
    }

    @Transactional(readOnly = true)
    public List<SupportTicketResponse> listMine(User customer) {
        return repository.findForCustomer(customer.getId(), customer.getPhone()).stream()
                .map(this::response)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SupportMessageResponse> listMessages(String ticketId) {
        findById(ticketId);
        return messageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
                .map(SupportMessageResponse::from)
                .toList();
    }

    @Transactional
    public SupportMessageResponse sendMessage(SupportTicket ticket, User sender, String senderRole, String message) {
        if (ticket.getStatus() == SupportTicketStatus.RESOLVED) {
            throw new IllegalStateException("Phiếu hỗ trợ đã hoàn tất, không thể gửi thêm tin nhắn");
        }
        String normalizedRole = senderRole == null ? "CUSTOMER" : senderRole.trim().toUpperCase();
        SupportTicketStatus before = ticket.getStatus();
        if ("ADMIN".equals(normalizedRole) || "RESTAURANT".equals(normalizedRole)) {
            ticket.startProgress();
            if ("ADMIN".equals(normalizedRole) && !StringUtils.hasText(ticket.getAssignedAdminId())) {
                ticket.setAssignedAdminId(sender.getId());
                ticket.setAssignedAdminName(sender.getName());
            }
        }
        ticket.setUpdatedAt(System.currentTimeMillis());
        SupportTicket saved = repository.save(ticket);
        if (before != saved.getStatus()) {
            appendHistory(saved, "FIRST_RESPONSE", normalizedRole, sender.getId(),
                    before, saved.getStatus(), "Đã bắt đầu trao đổi với khách hàng");
        }
        return SupportMessageResponse.from(appendMessage(
                saved, normalizedRole, sender.getId(), sender.getName(), message.trim(), System.currentTimeMillis()
        ));
    }

    @Transactional
    public SupportTicket resolve(String ticketId, String actorRole, String actorId) {
        SupportTicket ticket = findById(ticketId);
        SupportTicketStatus before = ticket.getStatus();
        ticket.resolve("Đã xử lý xong");
        SupportTicket saved = repository.save(ticket);
        appendHistory(saved, "RESOLVED", actorRole, actorId, before, saved.getStatus(), "Đã xử lý xong");
        return saved;
    }

    @Transactional(readOnly = true)
    public SupportTicket findById(String ticketId) {
        return repository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu hỗ trợ: " + ticketId));
    }

    @Transactional(readOnly = true)
    public SupportTicketResponse response(SupportTicket ticket) {
        return SupportTicketResponse.from(
                ticket, historyRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId())
        );
    }

    public boolean belongsToCustomer(SupportTicket ticket, User customer) {
        if (StringUtils.hasText(ticket.getCustomerId())) {
            return ticket.getCustomerId().equals(customer.getId());
        }
        return StringUtils.hasText(ticket.getCustomerPhone())
                && ticket.getCustomerPhone().equals(customer.getPhone());
    }

    public static boolean requiresOrder(SupportIssueType issueType) {
        return issueType == SupportIssueType.ORDER_CHANGE
                || issueType == SupportIssueType.FOOD_ISSUE
                || issueType == SupportIssueType.DELIVERY;
    }

    private SupportTicket createTicket(
            String customerId,
            String customerName,
            String customerPhone,
            String intent,
            String message,
            SupportTicketTarget target,
            Order verifiedOrder
    ) {
        SupportTicket ticket = new SupportTicket();
        ticket.setOrderId(verifiedOrder == null ? null : verifiedOrder.getId());
        ticket.setRestaurantId(verifiedOrder == null ? null : verifiedOrder.getRestaurantId());
        ticket.setCustomerId(customerId);
        ticket.setCustomerName(StringUtils.hasText(customerName) ? customerName.trim() : "Khách hàng");
        ticket.setCustomerPhone(customerPhone);
        ticket.setIntent(intent);
        ticket.setMessage(message.trim());
        ticket.setTarget(target);
        return ticket;
    }

    private void configureIssue(SupportTicket ticket, SupportIssueType issueType) {
        switch (issueType) {
            case PAYMENT -> {
                ticket.setDepartment(SupportDepartment.FINANCE);
                ticket.setPriority(SupportPriority.URGENT);
            }
            case ORDER_CHANGE, DELIVERY -> {
                ticket.setDepartment(SupportDepartment.OPERATIONS);
                ticket.setPriority(SupportPriority.HIGH);
            }
            case FOOD_ISSUE -> {
                ticket.setDepartment(SupportDepartment.OPERATIONS);
                ticket.setPriority(SupportPriority.URGENT);
            }
            case ACCOUNT, OTHER -> ticket.setDepartment(SupportDepartment.CUSTOMER_SERVICE);
        }
        ticket.recalculateResponseDeadline();
    }

    private SupportTicketTarget targetFor(SupportIssueType issueType) {
        return switch (issueType) {
            case ORDER_CHANGE, FOOD_ISSUE, DELIVERY -> SupportTicketTarget.RESTAURANT;
            case PAYMENT, ACCOUNT, OTHER -> SupportTicketTarget.ADMIN;
        };
    }

    private void appendCreatedHistory(SupportTicket ticket, String actorId) {
        appendHistory(ticket, "CREATED", "CUSTOMER", actorId,
                null, ticket.getStatus(), ticket.getMessage());
    }

    private SupportTicketMessage appendMessage(
            SupportTicket ticket,
            String senderRole,
            String senderId,
            String senderName,
            String message,
            long createdAt
    ) {
        SupportTicketMessage item = new SupportTicketMessage();
        item.setTicketId(ticket.getId());
        item.setSenderRole(senderRole);
        item.setSenderId(StringUtils.hasText(senderId) ? senderId : "system");
        item.setSenderName(StringUtils.hasText(senderName) ? senderName.trim() : "Hệ thống");
        item.setMessage(message);
        item.setCreatedAt(createdAt);
        return messageRepository.save(item);
    }

    private void appendHistory(
            SupportTicket ticket,
            String action,
            String actorRole,
            String actorId,
            SupportTicketStatus from,
            SupportTicketStatus to,
            String note
    ) {
        SupportTicketHistory history = new SupportTicketHistory();
        history.setTicketId(ticket.getId());
        history.setAction(action);
        history.setActorRole(actorRole == null ? "SYSTEM" : actorRole);
        history.setActorId(actorId == null || actorId.isBlank() ? "system" : actorId);
        history.setFromStatus(from == null ? null : from.name());
        history.setToStatus(to.name());
        history.setNote(note);
        historyRepository.save(history);
    }
}
