package com.foodfast.service;

import com.foodfast.entity.DeliveryAssignment;
import com.foodfast.entity.DeliveryStatus;
import com.foodfast.entity.Order;
import com.foodfast.entity.OrderStatus;
import com.foodfast.repository.DeliveryAssignmentRepository;
import com.foodfast.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryTaskServiceTest {

    @Mock DeliveryAssignmentRepository assignmentRepository;
    @Mock OrderRepository orderRepository;
    private DeliveryTaskService service;

    @BeforeEach
    void setUp() {
        service = new DeliveryTaskService(assignmentRepository, orderRepository);
    }

    @Test
    void createsOneWaitingTaskForReadyChildOrder() {
        Order order = new Order();
        order.setId("ORDER-READY-1");
        order.setRestaurantId("sweetdreams");
        order.setStatus(OrderStatus.READY);
        when(assignmentRepository.findFirstByOrderIdOrderByAssignedAtDesc(order.getId()))
                .thenReturn(Optional.empty());
        when(assignmentRepository.save(any(DeliveryAssignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryAssignment task = service.ensureWaitingTask(order);

        assertThat(task.getOrderId()).isEqualTo(order.getId());
        assertThat(task.getRestaurantId()).isEqualTo("rest_2");
        assertThat(task.getCourierId()).isNull();
        assertThat(task.getStatus()).isEqualTo(DeliveryStatus.WAITING);
    }
}
