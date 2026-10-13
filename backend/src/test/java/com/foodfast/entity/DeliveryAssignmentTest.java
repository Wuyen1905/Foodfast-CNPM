package com.foodfast.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeliveryAssignmentTest {

    @Test
    void assignedDeliveryCanStartThenComplete() {
        DeliveryAssignment assignment = new DeliveryAssignment();
        assignment.setStatus(DeliveryStatus.ASSIGNED);

        assignment.start();
        assertEquals(DeliveryStatus.DELIVERING, assignment.getStatus());
        assertTrue(assignment.getStartedAt() > 0);

        assignment.complete(DeliveryCompletionSource.CUSTOMER, null);
        assertEquals(DeliveryStatus.DELIVERED, assignment.getStatus());
        assertEquals(DeliveryCompletionSource.CUSTOMER, assignment.getCompletionSource());
        assertTrue(assignment.getDeliveredAt() > 0);
    }

    @Test
    void assignedDeliveryCannotBeCompletedBeforeItStarts() {
        DeliveryAssignment assignment = new DeliveryAssignment();
        assignment.setStatus(DeliveryStatus.ASSIGNED);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> assignment.complete(DeliveryCompletionSource.CUSTOMER, null)
        );

        assertEquals("Chỉ đơn đang giao mới có thể hoàn thành", error.getMessage());
    }

    @Test
    void failedDeliveryRequiresReason() {
        DeliveryAssignment assignment = new DeliveryAssignment();
        assignment.setStatus(DeliveryStatus.DELIVERING);

        assertThrows(IllegalArgumentException.class, () -> assignment.fail("  "));

        assignment.fail("Khách không nghe máy");
        assertEquals(DeliveryStatus.FAILED, assignment.getStatus());
        assertEquals("Khách không nghe máy", assignment.getFailureReason());
    }

    @Test
    void restaurantOverrideRequiresAnAuditableReason() {
        DeliveryAssignment assignment = new DeliveryAssignment();
        assignment.setStatus(DeliveryStatus.DELIVERING);

        assertThrows(
                IllegalArgumentException.class,
                () -> assignment.complete(DeliveryCompletionSource.RESTAURANT_OVERRIDE, "quên")
        );

        assignment.complete(
                DeliveryCompletionSource.RESTAURANT_OVERRIDE,
                "Người giao báo khách đã nhận đủ món"
        );
        assertEquals(DeliveryStatus.DELIVERED, assignment.getStatus());
        assertEquals(DeliveryCompletionSource.RESTAURANT_OVERRIDE, assignment.getCompletionSource());
        assertEquals("Người giao báo khách đã nhận đủ món", assignment.getCompletionNote());
    }
}
