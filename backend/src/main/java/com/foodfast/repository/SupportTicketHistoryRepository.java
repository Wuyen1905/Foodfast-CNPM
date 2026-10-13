package com.foodfast.repository;

import com.foodfast.entity.SupportTicketHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportTicketHistoryRepository extends JpaRepository<SupportTicketHistory, Long> {
    List<SupportTicketHistory> findByTicketIdOrderByCreatedAtAsc(String ticketId);
}
