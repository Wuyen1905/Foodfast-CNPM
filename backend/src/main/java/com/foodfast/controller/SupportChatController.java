package com.foodfast.controller;

import com.foodfast.dto.SupportChatRequest;
import com.foodfast.dto.SupportChatResponse;
import com.foodfast.entity.User;
import com.foodfast.security.CurrentUserService;
import com.foodfast.service.SupportChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/support")
public class SupportChatController {

    private final SupportChatService supportChatService;
    private final CurrentUserService currentUser;

    public SupportChatController(
            SupportChatService supportChatService,
            CurrentUserService currentUser
    ) {
        this.supportChatService = supportChatService;
        this.currentUser = currentUser;
    }

    @PostMapping("/chat")
    public SupportChatResponse chat(@RequestBody @Valid SupportChatRequest request) {
        User customer = currentUser.requireUser();
        return supportChatService.reply(request, customer.getId(), customer.getName(), customer.getPhone());
    }
}
