package com.foodfast.service;

import com.foodfast.dto.CreateSubstitutionProposalRequest;
import com.foodfast.dto.RespondSubstitutionRequest;
import com.foodfast.entity.Order;
import com.foodfast.entity.OrderItem;
import com.foodfast.entity.OrderStatus;
import com.foodfast.entity.Product;
import com.foodfast.entity.SubstitutionProposal;
import com.foodfast.entity.SubstitutionStatus;
import com.foodfast.event.OrderEventPublisher;
import com.foodfast.exception.ForbiddenOperationException;
import com.foodfast.repository.OrderRepository;
import com.foodfast.repository.ProductRepository;
import com.foodfast.repository.SubstitutionProposalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class SubstitutionService {
    private final SubstitutionProposalRepository proposalRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderEventPublisher orderEventPublisher;

    public SubstitutionService(
            SubstitutionProposalRepository proposalRepository,
            OrderRepository orderRepository,
            ProductRepository productRepository,
            OrderEventPublisher orderEventPublisher
    ) {
        this.proposalRepository = proposalRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.orderEventPublisher = orderEventPublisher;
    }

    @Transactional
    public SubstitutionProposal propose(
            String orderId,
            String authenticatedRestaurantId,
            CreateSubstitutionProposalRequest request
    ) {
        if (request == null || !StringUtils.hasText(authenticatedRestaurantId)
                || request.orderItemId == null || !StringUtils.hasText(request.replacementProductId)) {
            throw new IllegalArgumentException("Thiếu món cần thay, món thay thế hoặc nhà hàng thực hiện");
        }
        Order order = requireOrder(orderId);
        if (!RestaurantIdentityPolicy.refersToSameRestaurant(
                authenticatedRestaurantId,
                order.getRestaurantId()
        )) {
            throw new ForbiddenOperationException("Nhà hàng chỉ được đề xuất thay món trong đơn của mình");
        }
        if (!List.of(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PREPARING)
                .contains(order.getStatusEnum())) {
            throw new IllegalStateException("Không thể thay món khi đơn đã sẵn sàng hoặc đang giao");
        }
        OrderItem original = requireOrderItem(order, request.orderItemId);
        if (proposalRepository.existsByOrderItemIdAndStatus(original.getId(), SubstitutionStatus.PENDING_CUSTOMER)) {
            throw new IllegalStateException("Món này đã có một đề xuất đang chờ khách phản hồi");
        }
        Product replacement = productRepository.findById(request.replacementProductId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy món thay thế"));
        if (!replacement.isAvailable()) throw new IllegalArgumentException("Món thay thế hiện không còn bán");
        if (!RestaurantIdentityPolicy.refersToSameRestaurant(replacement.getRestaurant(), order.getRestaurantId())) {
            throw new IllegalArgumentException("Món thay thế phải thuộc cùng nhà hàng");
        }
        if (replacement.getId().equals(original.getProductId())) {
            throw new IllegalArgumentException("Món thay thế phải khác món ban đầu");
        }
        if (replacement.isCustomizable()) {
            throw new IllegalArgumentException("Chỉ có thể đề xuất món thay thế không yêu cầu khách chọn thêm tùy chọn");
        }
        if (replacement.getPrice() > original.getPrice()) {
            throw new IllegalArgumentException("Món thay thế không được đắt hơn món khách đã đặt");
        }
        String reason = normalizeReason(request.reason);
        int minutes = request.expiresInMinutes == null ? 15 : request.expiresInMinutes;
        if (minutes < 5 || minutes > 60) {
            throw new IllegalArgumentException("Thời gian phản hồi phải từ 5 đến 60 phút");
        }
        Instant now = Instant.now();
        SubstitutionProposal proposal = new SubstitutionProposal();
        proposal.setOrderId(order.getId());
        proposal.setOrderItemId(original.getId());
        proposal.setRestaurantId(order.getRestaurantId());
        proposal.setCustomerId(order.getUserId());
        proposal.setOriginalProductId(original.getProductId());
        proposal.setOriginalProductName(original.getName());
        proposal.setOriginalUnitPrice(original.getPrice());
        proposal.setReplacementProductId(replacement.getId());
        proposal.setReplacementProductName(replacement.getName());
        proposal.setReplacementUnitPrice(replacement.getPrice());
        proposal.setReason(reason);
        proposal.setProposedAt(now);
        proposal.setExpiresAt(now.plus(minutes, ChronoUnit.MINUTES));
        return proposalRepository.save(proposal);
    }

    @Transactional
    public List<SubstitutionProposal> list(String orderId) {
        requireOrder(orderId);
        List<SubstitutionProposal> proposals = proposalRepository.findByOrderIdOrderByProposedAtDesc(orderId);
        Instant now = Instant.now();
        proposals.stream()
                .filter(proposal -> proposal.getStatus() == SubstitutionStatus.PENDING_CUSTOMER)
                .filter(proposal -> now.isAfter(proposal.getExpiresAt()))
                .forEach(proposal -> proposal.setStatus(SubstitutionStatus.EXPIRED));
        return proposalRepository.saveAll(proposals);
    }

    @Transactional
    public SubstitutionProposal respond(
            String orderId,
            Long proposalId,
            String authenticatedUserId,
            RespondSubstitutionRequest request
    ) {
        if (request == null || !StringUtils.hasText(authenticatedUserId)) {
            throw new IllegalArgumentException("Không xác định được khách hàng phản hồi");
        }
        SubstitutionProposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đề xuất thay món"));
        if (!proposal.getOrderId().equals(orderId)) {
            throw new IllegalArgumentException("Đề xuất thay món không thuộc đơn hàng này");
        }
        if (!proposal.getCustomerId().equals(authenticatedUserId.trim())) {
            throw new ForbiddenOperationException("Chỉ khách đặt đơn mới được phản hồi đề xuất thay món");
        }
        if (proposal.getStatus() != SubstitutionStatus.PENDING_CUSTOMER) {
            throw new IllegalStateException("Đề xuất này đã được xử lý");
        }
        Instant now = Instant.now();
        if (now.isAfter(proposal.getExpiresAt())) {
            proposal.setStatus(SubstitutionStatus.EXPIRED);
            proposalRepository.save(proposal);
            return proposal;
        }
        proposal.setRespondedAt(now);
        if (!request.accept) {
            proposal.setStatus(SubstitutionStatus.REJECTED);
            return proposalRepository.save(proposal);
        }

        Order order = requireOrder(orderId);
        OrderItem item = requireOrderItem(order, proposal.getOrderItemId());
        Product replacement = productRepository.findById(proposal.getReplacementProductId())
                .orElseThrow(() -> new IllegalArgumentException("Món thay thế không còn tồn tại"));
        if (!replacement.isAvailable()) {
            throw new IllegalStateException("Món thay thế vừa hết hàng; nhà hàng cần đề xuất món khác");
        }
        int oldTotal = order.getTotal();
        item.setOriginalProductId(item.getProductId());
        item.setOriginalProductName(item.getName());
        item.setProductId(replacement.getId());
        item.setName(replacement.getName());
        item.setProductName(replacement.getName());
        item.setProductImage(replacement.getImageUrl());
        item.setPrice(replacement.getPrice());
        item.getSelectedOptions().clear();
        item.setSubstitutionProposalId(proposal.getId());
        order.recalculateTotal();
        int refundDifference = Math.max(0, oldTotal - order.getTotal());
        if (refundDifference > 0 && PaymentStatusPolicy.isPaid(order)) {
            order.setPartialRefundAmount(refundDifference);
            order.setPaymentStatus(PaymentStatusPolicy.PARTIAL_REFUND_PENDING);
        }
        proposal.setStatus(SubstitutionStatus.ACCEPTED);
        proposalRepository.save(proposal);
        Order saved = orderRepository.save(order);
        orderEventPublisher.publishOrderUpdate(saved);
        return proposal;
    }

    private Order requireOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng: " + orderId));
    }

    private OrderItem requireOrderItem(Order order, Long itemId) {
        return order.getItems().stream()
                .filter(item -> itemId.equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Món cần thay không thuộc đơn hàng này"));
    }

    private String normalizeReason(String reason) {
        if (!StringUtils.hasText(reason) || reason.trim().length() < 5) {
            throw new IllegalArgumentException("Vui lòng nêu lý do thay món ít nhất 5 ký tự");
        }
        if (reason.trim().length() > 500) throw new IllegalArgumentException("Lý do thay món quá dài");
        return reason.trim();
    }
}
