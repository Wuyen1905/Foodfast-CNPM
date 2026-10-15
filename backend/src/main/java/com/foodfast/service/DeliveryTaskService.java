package com.foodfast.service;

import com.foodfast.entity.DeliveryAssignment;
import com.foodfast.entity.DeliveryStatus;
import com.foodfast.entity.Order;
import com.foodfast.entity.OrderStatus;
import com.foodfast.repository.DeliveryAssignmentRepository;
import com.foodfast.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Creates and exposes the one delivery task that belongs to each ready child order. */
@Service
public class DeliveryTaskService {

    private static final List<DeliveryStatus> QUEUE_STATUSES = List.of(
            DeliveryStatus.WAITING, DeliveryStatus.ASSIGNED, DeliveryStatus.DELIVERING
    );

    private final DeliveryAssignmentRepository assignmentRepository;
    private final OrderRepository orderRepository;

    public DeliveryTaskService(
            DeliveryAssignmentRepository assignmentRepository,
            OrderRepository orderRepository
    ) {
        this.assignmentRepository = assignmentRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public DeliveryAssignment ensureWaitingTask(Order order) {
        if (order == null || order.getStatusEnum() != OrderStatus.READY) return null;
        DeliveryAssignment existing = assignmentRepository
                .findFirstByOrderIdOrderByAssignedAtDesc(order.getId()).orElse(null);
        if (existing != null) {
            if (existing.getStatus() == DeliveryStatus.FAILED
                    || existing.getStatus() == DeliveryStatus.CANCELLED) {
                existing.reopenWaiting();
                return assignmentRepository.save(existing);
            }
            return existing;
        }
        DeliveryAssignment task = new DeliveryAssignment();
        task.setOrderId(order.getId());
        task.setRestaurantId(RestaurantIdentityPolicy.canonicalize(order.getRestaurantId()));
        task.setStatus(DeliveryStatus.WAITING);
        return assignmentRepository.save(task);
    }

    @Transactional
    public List<DeliveryAssignment> listQueue() {
        orderRepository.findAll().stream()
                .filter(order -> order.getStatusEnum() == OrderStatus.READY)
                .forEach(this::ensureWaitingTask);
        return assignmentRepository.findByStatusInOrderByAssignedAtAsc(QUEUE_STATUSES);
    }
}
