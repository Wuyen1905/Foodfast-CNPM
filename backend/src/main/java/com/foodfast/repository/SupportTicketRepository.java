package com.foodfast.repository;

import com.foodfast.entity.SupportTicket;
import com.foodfast.entity.SupportTicketStatus;
import com.foodfast.entity.SupportTicketTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, String> {
    List<SupportTicket> findByTargetAndStatusOrderByCreatedAtDesc(
            SupportTicketTarget target,
            SupportTicketStatus status
    );

    List<SupportTicket> findByRestaurantIdAndTargetAndStatusOrderByCreatedAtDesc(
            String restaurantId,
            SupportTicketTarget target,
            SupportTicketStatus status
    );

    @Query("""
            select ticket from SupportTicket ticket
            where ticket.customerId = :customerId
               or (ticket.customerId is null and :customerPhone is not null
                   and ticket.customerPhone = :customerPhone)
            order by ticket.updatedAt desc
            """)
    List<SupportTicket> findForCustomer(
            @Param("customerId") String customerId,
            @Param("customerPhone") String customerPhone
    );
}
