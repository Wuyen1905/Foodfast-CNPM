package com.foodfast.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "support_ticket_history")
public class SupportTicketHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_id", length = 50, nullable = false)
    private String ticketId;

    @Column(length = 40, nullable = false)
    private String action;

    @Column(name = "actor_role", length = 20, nullable = false)
    private String actorRole;

    @Column(name = "actor_id", length = 60, nullable = false)
    private String actorId;

    @Column(length = 1200)
    private String note;

    @Column(name = "from_status", length = 20)
    private String fromStatus;

    @Column(name = "to_status", length = 20, nullable = false)
    private String toStatus;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == 0) createdAt = System.currentTimeMillis();
    }

    public Long getId() { return id; }
    public String getTicketId() { return ticketId; }
    public void setTicketId(String ticketId) { this.ticketId = ticketId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getActorRole() { return actorRole; }
    public void setActorRole(String actorRole) { this.actorRole = actorRole; }
    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getFromStatus() { return fromStatus; }
    public void setFromStatus(String fromStatus) { this.fromStatus = fromStatus; }
    public String getToStatus() { return toStatus; }
    public void setToStatus(String toStatus) { this.toStatus = toStatus; }
    public long getCreatedAt() { return createdAt; }
}
