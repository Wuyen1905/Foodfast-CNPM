package com.foodfast.service;

import com.foodfast.entity.SupportTicketStatus;
import com.foodfast.entity.SupportTicketTarget;
import com.foodfast.repository.SupportTicketRepository;
import com.foodfast.repository.SupportTicketHistoryRepository;
import com.foodfast.repository.SupportTicketMessageRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SupportTicketServiceTest {

    private final SupportTicketRepository repository = mock(SupportTicketRepository.class);
    private final SupportTicketHistoryRepository historyRepository = mock(SupportTicketHistoryRepository.class);
    private final SupportTicketMessageRepository messageRepository = mock(SupportTicketMessageRepository.class);
    private final SupportTicketService service = new SupportTicketService(
            repository, historyRepository, messageRepository
    );

    @Test
    void restaurantInboxFiltersByRestaurantTargetAndStatus() {
        service.list(
                SupportTicketTarget.RESTAURANT,
                "restaurant_2",
                SupportTicketStatus.OPEN
        );

        verify(repository).findByRestaurantIdAndTargetAndStatusOrderByCreatedAtDesc(
                "restaurant_2",
                SupportTicketTarget.RESTAURANT,
                SupportTicketStatus.OPEN
        );
    }

    @Test
    void restaurantInboxRequiresRestaurantId() {
        assertThatThrownBy(() -> service.list(
                SupportTicketTarget.RESTAURANT,
                null,
                SupportTicketStatus.OPEN
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
