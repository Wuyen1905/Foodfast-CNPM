package com.foodfast.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdminOperationsModelTest {

    @Test
    void restaurantLifecycleSynchronizesAvailability() {
        Restaurant restaurant = new Restaurant();

        restaurant.setAdminStatus(RestaurantLifecycleStatus.SUSPENDED);
        assertThat(restaurant.getIsActive()).isFalse();

        restaurant.setAdminStatus(RestaurantLifecycleStatus.ACTIVE);
        assertThat(restaurant.getIsActive()).isTrue();
    }

    @Test
    void urgentTicketGetsFourHourDeadlineAndKeepsResolution() {
        SupportTicket ticket = new SupportTicket();
        ticket.setTarget(SupportTicketTarget.ADMIN);
        ticket.setPriority(SupportPriority.URGENT);
        ticket.prePersist();

        assertThat(ticket.getSlaDueAt() - ticket.getCreatedAt())
                .isEqualTo(SupportDeadlinePolicy.FOUR_HOURS);

        ticket.startProgress();
        assertThat(ticket.getStatus()).isEqualTo(SupportTicketStatus.IN_PROGRESS);

        ticket.resolve("Đã hướng dẫn khách hàng và hoàn tất đối soát");
        assertThat(ticket.getStatus()).isEqualTo(SupportTicketStatus.RESOLVED);
        assertThat(ticket.getResolutionNote()).contains("hoàn tất đối soát");
        assertThat(ticket.getResolvedAt()).isNotNull();
    }

    @Test
    void responseDeadlinesFollowSimpleBusinessRules() {
        SupportTicket normal = new SupportTicket();
        normal.setTarget(SupportTicketTarget.ADMIN);
        normal.prePersist();
        assertThat(normal.getSlaDueAt() - normal.getCreatedAt())
                .isEqualTo(SupportDeadlinePolicy.TWENTY_FOUR_HOURS);

        SupportTicket high = new SupportTicket();
        high.setTarget(SupportTicketTarget.ADMIN);
        high.setPriority(SupportPriority.HIGH);
        high.prePersist();
        assertThat(high.getSlaDueAt() - high.getCreatedAt())
                .isEqualTo(SupportDeadlinePolicy.EIGHT_HOURS);

        SupportTicket finance = new SupportTicket();
        finance.setTarget(SupportTicketTarget.ADMIN);
        finance.setDepartment(SupportDepartment.FINANCE);
        finance.prePersist();
        assertThat(finance.getSlaDueAt() - finance.getCreatedAt())
                .isEqualTo(SupportDeadlinePolicy.FOUR_HOURS);

        Complaint complaint = new Complaint();
        complaint.prePersist();
        assertThat(SupportDeadlinePolicy.complaintDeadline(complaint) - complaint.getCreatedAt())
                .isEqualTo(SupportDeadlinePolicy.EIGHT_HOURS);
    }
}
