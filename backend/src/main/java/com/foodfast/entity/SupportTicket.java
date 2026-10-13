package com.foodfast.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.UUID;

@Entity
@Table(name = "support_tickets")
public class SupportTicket {

    @Id
    @Column(length = 50, nullable = false)
    private String id;

    @Column(name = "order_id", length = 60)
    private String orderId;

    @Column(name = "restaurant_id", length = 50)
    private String restaurantId;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Column(name = "customer_id", length = 50)
    private String customerId;

    @Column(name = "customer_name", length = 150)
    private String customerName;

    @Column(length = 80, nullable = false)
    private String intent;

    @Column(length = 1000, nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private SupportTicketTarget target;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private SupportTicketStatus status = SupportTicketStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private SupportPriority priority = SupportPriority.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private SupportDepartment department = SupportDepartment.CUSTOMER_SERVICE;

    @Column(name = "assigned_admin_id", length = 60)
    private String assignedAdminId;

    @Column(name = "assigned_admin_name", length = 150)
    private String assignedAdminName;

    @Column(name = "sla_due_at")
    private Long slaDueAt;

    @Column(name = "resolution_note", length = 1200)
    private String resolutionNote;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Column(name = "resolved_at")
    private Long resolvedAt;

    @PrePersist
    void prePersist() {
        long now = System.currentTimeMillis();
        if (id == null || id.isBlank()) {
            id = "TICKET-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        createdAt = createdAt == 0 ? now : createdAt;
        updatedAt = now;
        if (priority == null) priority = SupportPriority.NORMAL;
        if (department == null) {
            department = target == SupportTicketTarget.RESTAURANT
                    ? SupportDepartment.OPERATIONS
                    : SupportDepartment.CUSTOMER_SERVICE;
        }
        recalculateResponseDeadline();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = System.currentTimeMillis();
    }

    public void resolve(String note) {
        if (status == SupportTicketStatus.RESOLVED) return;
        status = SupportTicketStatus.RESOLVED;
        resolutionNote = note;
        resolvedAt = System.currentTimeMillis();
        updatedAt = resolvedAt;
    }

    public void startProgress() {
        if (status == SupportTicketStatus.OPEN) status = SupportTicketStatus.IN_PROGRESS;
    }

    public void recalculateResponseDeadline() {
        slaDueAt = SupportDeadlinePolicy.ticketDeadline(this);
    }

    /**
     * Kept as a compatibility alias for callers created before the Vietnamese
     * "thời gian cam kết xử lý" wording was introduced.
     */
    public void recalculateSla() {
        recalculateResponseDeadline();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getRestaurantId() { return restaurantId; }
    public void setRestaurantId(String restaurantId) { this.restaurantId = restaurantId; }
    @JsonIgnore
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    @JsonIgnore
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public SupportTicketTarget getTarget() { return target; }
    public void setTarget(SupportTicketTarget target) { this.target = target; }
    public SupportTicketStatus getStatus() { return status; }
    public void setStatus(SupportTicketStatus status) { this.status = status; }
    public SupportPriority getPriority() { return priority; }
    public void setPriority(SupportPriority priority) { this.priority = priority; }
    public SupportDepartment getDepartment() { return department; }
    public void setDepartment(SupportDepartment department) { this.department = department; }
    public String getAssignedAdminId() { return assignedAdminId; }
    public void setAssignedAdminId(String assignedAdminId) { this.assignedAdminId = assignedAdminId; }
    public String getAssignedAdminName() { return assignedAdminName; }
    public void setAssignedAdminName(String assignedAdminName) { this.assignedAdminName = assignedAdminName; }
    public Long getSlaDueAt() { return slaDueAt; }
    public void setSlaDueAt(Long slaDueAt) { this.slaDueAt = slaDueAt; }
    public String getResolutionNote() { return resolutionNote; }
    public void setResolutionNote(String resolutionNote) { this.resolutionNote = resolutionNote; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    public Long getResolvedAt() { return resolvedAt; }
}
