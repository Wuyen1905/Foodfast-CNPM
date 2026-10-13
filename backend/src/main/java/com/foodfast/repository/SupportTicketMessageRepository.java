package com.foodfast.repository;

import com.foodfast.entity.SupportTicketMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportTicketMessageRepository extends JpaRepository<SupportTicketMessage, Long> {
    List<SupportTicketMessage> findByTicketIdOrderByCreatedAtAsc(String ticketId);
}
