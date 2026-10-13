package com.foodfast.entity;

/**
 * Quy tắc thời gian cam kết xử lý dùng chung cho trung tâm vận hành.
 * Tên kỹ thuật cũ trong cơ sở dữ liệu vẫn được giữ để tương thích dữ liệu.
 */
public final class SupportDeadlinePolicy {
    public static final long FOUR_HOURS = 4 * 60 * 60 * 1000L;
    public static final long EIGHT_HOURS = 8 * 60 * 60 * 1000L;
    public static final long TWENTY_FOUR_HOURS = 24 * 60 * 60 * 1000L;

    private SupportDeadlinePolicy() {
    }

    public static long ticketDeadline(SupportTicket ticket) {
        long createdAt = ticket.getCreatedAt() == 0
                ? System.currentTimeMillis()
                : ticket.getCreatedAt();
        return createdAt + ticketDuration(ticket);
    }

    public static long ticketDuration(SupportTicket ticket) {
        if (ticket.getDepartment() == SupportDepartment.FINANCE
                || "REFUND_OR_PAYMENT".equalsIgnoreCase(ticket.getIntent())) {
            return FOUR_HOURS;
        }

        SupportPriority priority = ticket.getPriority() == null
                ? SupportPriority.NORMAL
                : ticket.getPriority();
        return switch (priority) {
            case URGENT -> FOUR_HOURS;
            case HIGH -> EIGHT_HOURS;
            case NORMAL, LOW -> TWENTY_FOUR_HOURS;
        };
    }

    public static boolean isTicketOverdue(SupportTicket ticket, long now) {
        return ticket.getStatus() != SupportTicketStatus.RESOLVED
                && ticketDeadline(ticket) < now;
    }

    public static long complaintDeadline(Complaint complaint) {
        if (complaint.getRefundStatus() == RefundStatus.PENDING) {
            long approvedAt = complaint.getResolvedAt() == null
                    ? complaint.getCreatedAt()
                    : complaint.getResolvedAt();
            return approvedAt + FOUR_HOURS;
        }
        return complaint.getCreatedAt() + EIGHT_HOURS;
    }

    public static boolean isComplaintOverdue(Complaint complaint, long now) {
        boolean awaitingRefund = complaint.getRefundStatus() == RefundStatus.PENDING;
        boolean complaintOpen = complaint.getStatus() == ComplaintStatus.OPEN
                || complaint.getStatus() == ComplaintStatus.REVIEWING;
        return (awaitingRefund || complaintOpen) && complaintDeadline(complaint) < now;
    }
}
