package com.foodfast.security;

import com.foodfast.entity.User;
import com.foodfast.repository.UserRepository;
import com.foodfast.service.RestaurantIdentityPolicy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

/**
 * Authenticates STOMP connections and limits subscriptions to the current
 * customer, restaurant or administrator. The SockJS handshake stays public,
 * but no STOMP session can be established without a valid JWT.
 */
@Component
public class WebSocketSecurityInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public WebSocketSecurityInterceptor(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticate(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        } else if (StompCommand.SEND.equals(accessor.getCommand())) {
            throw new AccessDeniedException("Ứng dụng khách không được tự phát sự kiện realtime");
        }

        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authorization = firstHeader(accessor, "Authorization", "authorization");
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            throw new AccessDeniedException("Phiên realtime yêu cầu đăng nhập");
        }

        JwtService.Claims claims;
        try {
            claims = jwtService.verify(authorization.substring(BEARER_PREFIX.length()).trim());
        } catch (IllegalArgumentException exception) {
            throw new AccessDeniedException("Phiên realtime không hợp lệ hoặc đã hết hạn", exception);
        }

        User user = userRepository.findById(claims.userId())
                .orElseThrow(() -> new AccessDeniedException("Tài khoản realtime không còn tồn tại"));
        if ("suspended".equalsIgnoreCase(user.getRole())) {
            throw new AccessDeniedException("Tài khoản đã bị tạm ngưng");
        }

        String role = user.getRole().trim().toUpperCase(Locale.ROOT);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user.getId(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );
        authentication.setDetails(user);
        accessor.setUser(authentication);
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        Authentication authentication = accessor.getUser() instanceof Authentication current
                ? current
                : null;
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Phiên realtime yêu cầu đăng nhập");
        }

        String destination = accessor.getDestination();
        if (!StringUtils.hasText(destination)) {
            throw new AccessDeniedException("Kênh realtime không hợp lệ");
        }

        User user = authentication.getDetails() instanceof User currentUser ? currentUser : null;
        if (user == null) {
            throw new AccessDeniedException("Không xác định được người dùng realtime");
        }

        if (destination.startsWith("/user/queue/")) {
            return;
        }

        if (destination.equals("/topic/admin/orders")
                || destination.equals("/topic/admin/deliveries")) {
            requireRole(user, "admin");
            return;
        }

        String prefix = "/topic/restaurants/";
        if (destination.startsWith(prefix)) {
            String remainder = destination.substring(prefix.length());
            int separator = remainder.indexOf('/');
            String restaurantId = separator < 0 ? "" : remainder.substring(0, separator);
            String resource = separator < 0 ? "" : remainder.substring(separator + 1);
            if (!(resource.equals("orders") || resource.equals("deliveries"))) {
                throw new AccessDeniedException("Kênh realtime nhà hàng không hợp lệ");
            }
            if ("admin".equalsIgnoreCase(user.getRole())) {
                return;
            }
            if (!"restaurant".equalsIgnoreCase(user.getRole())
                    || !RestaurantIdentityPolicy.refersToSameRestaurant(user.getRestaurantId(), restaurantId)) {
                throw new AccessDeniedException("Không được theo dõi dữ liệu realtime của nhà hàng khác");
            }
            return;
        }

        throw new AccessDeniedException("Kênh realtime không được phép");
    }

    private void requireRole(User user, String role) {
        if (!role.equalsIgnoreCase(user.getRole())) {
            throw new AccessDeniedException("Không có quyền theo dõi kênh realtime này");
        }
    }

    private String firstHeader(StompHeaderAccessor accessor, String... names) {
        for (String name : names) {
            String value = accessor.getFirstNativeHeader(name);
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
