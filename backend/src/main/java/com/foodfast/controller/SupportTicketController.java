package com.foodfast.controller;

import com.foodfast.dto.CreateSupportContactRequest;
import com.foodfast.dto.SupportMessageRequest;
import com.foodfast.dto.SupportMessageResponse;
import com.foodfast.dto.SupportTicketResponse;
import com.foodfast.entity.Order;
import com.foodfast.entity.SupportTicket;
import com.foodfast.entity.SupportTicketStatus;
import com.foodfast.entity.SupportTicketTarget;
import com.foodfast.entity.User;
import com.foodfast.exception.ForbiddenOperationException;
import com.foodfast.security.CurrentUserService;
import com.foodfast.service.OrderService;
import com.foodfast.service.SupportTicketService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/support/tickets")
public class SupportTicketController {

    private final SupportTicketService ticketService;
    private final CurrentUserService currentUser;
    private final OrderService orderService;

    public SupportTicketController(
            SupportTicketService ticketService,
            CurrentUserService currentUser,
            OrderService orderService
    ) {
        this.ticketService = ticketService;
        this.currentUser = currentUser;
        this.orderService = orderService;
    }

    @PostMapping("/contact")
    public SupportTicketResponse contact(@RequestBody @Valid CreateSupportContactRequest request) {
        User customer = currentUser.requireUser();
        if (!currentUser.isCustomer()) {
            throw new ForbiddenOperationException("Chỉ khách hàng được tạo yêu cầu hỗ trợ");
        }
        Order order = null;
        if (request.orderId() != null && !request.orderId().isBlank()) {
            order = orderService.findById(request.orderId().trim())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng đã chọn"));
            currentUser.requireOrderAccess(order);
        }
        if (SupportTicketService.requiresOrder(request.issueType()) && order == null) {
            throw new IllegalArgumentException("Vui lòng chọn đơn hàng liên quan tới vấn đề này");
        }
        return ticketService.createContact(request, customer, order);
    }

    @GetMapping("/mine")
    public List<SupportTicketResponse> mine() {
        User customer = currentUser.requireUser();
        if (!currentUser.isCustomer()) {
            throw new ForbiddenOperationException("Chỉ khách hàng được xem phiếu hỗ trợ của mình");
        }
        return ticketService.listMine(customer);
    }

    @GetMapping
    public List<SupportTicket> list(
            @RequestParam SupportTicketTarget target,
            @RequestParam(required = false) String restaurantId,
            @RequestParam(defaultValue = "OPEN") SupportTicketStatus status
    ) {
        if (currentUser.isRestaurant()) {
            target = SupportTicketTarget.RESTAURANT;
            restaurantId = currentUser.requireRestaurantId();
        } else if (!currentUser.isAdmin()) {
            throw new ForbiddenOperationException("Bạn không được xem danh sách phiếu hỗ trợ");
        }
        return ticketService.list(target, restaurantId, status);
    }

    @GetMapping("/{ticketId}/messages")
    public List<SupportMessageResponse> messages(@PathVariable String ticketId) {
        SupportTicket ticket = ticketService.findById(ticketId);
        requireTicketAccess(ticket);
        return ticketService.listMessages(ticketId);
    }

    @PostMapping("/{ticketId}/messages")
    public SupportMessageResponse sendMessage(
            @PathVariable String ticketId,
            @RequestBody @Valid SupportMessageRequest request
    ) {
        User sender = currentUser.requireUser();
        SupportTicket ticket = ticketService.findById(ticketId);
        requireTicketAccess(ticket);
        return ticketService.sendMessage(ticket, sender, currentUser.role(), request.message());
    }

    @PatchMapping("/{ticketId}/resolve")
    public SupportTicketResponse resolve(@PathVariable String ticketId) {
        SupportTicket ticket = ticketService.findById(ticketId);
        if (currentUser.isRestaurant()) {
            if (ticket.getTarget() != SupportTicketTarget.RESTAURANT) {
                throw new ForbiddenOperationException("Nhà hàng không được xử lý phiếu dành cho quản trị viên");
            }
            currentUser.requireRestaurantAccess(ticket.getRestaurantId());
        } else if (!currentUser.isAdmin()) {
            throw new ForbiddenOperationException("Bạn không được xử lý phiếu hỗ trợ này");
        }
        return ticketService.response(ticketService.resolve(
                ticketId, currentUser.role(), currentUser.businessActorId()
        ));
    }

    private void requireTicketAccess(SupportTicket ticket) {
        User user = currentUser.requireUser();
        if (currentUser.isAdmin()) return;
        if (currentUser.isCustomer()) {
            if (!ticketService.belongsToCustomer(ticket, user)) {
                throw new ForbiddenOperationException("Bạn không được xem cuộc trao đổi này");
            }
            return;
        }
        if (currentUser.isRestaurant()) {
            if (ticket.getTarget() != SupportTicketTarget.RESTAURANT) {
                throw new ForbiddenOperationException("Phiếu này không thuộc nhà hàng");
            }
            currentUser.requireRestaurantAccess(ticket.getRestaurantId());
            return;
        }
        throw new ForbiddenOperationException("Bạn không được truy cập phiếu hỗ trợ này");
    }
}
