package com.foodfast.security;

import com.foodfast.entity.User;
import com.foodfast.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketSecurityInterceptorTest {

    @Mock private JwtService jwtService;
    @Mock private UserRepository userRepository;
    @Mock private MessageChannel channel;

    private WebSocketSecurityInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new WebSocketSecurityInterceptor(jwtService, userRepository);
    }

    @Test
    void connectRequiresBearerToken() {
        assertThatThrownBy(() -> interceptor.preSend(message(StompCommand.CONNECT, null, null), channel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Phiên realtime yêu cầu đăng nhập");
    }

    @Test
    void connectUsesVerifiedBackendIdentity() {
        User customer = user("CUSTOMER-1", "customer", null);
        when(jwtService.verify("valid-token")).thenReturn(new JwtService.Claims("CUSTOMER-1", 9_999_999_999L));
        when(userRepository.findById("CUSTOMER-1")).thenReturn(Optional.of(customer));

        Message<?> result = interceptor.preSend(
                message(StompCommand.CONNECT, null, "Bearer valid-token"),
                channel
        );
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);

        assertThat(accessor.getUser()).isNotNull();
        assertThat(accessor.getUser().getName()).isEqualTo("CUSTOMER-1");
    }

    @Test
    void restaurantCannotSubscribeToAnotherRestaurant() {
        User restaurant = user("OWNER-1", "restaurant", "rest_2");
        assertThatThrownBy(() -> interceptor.preSend(
                subscribedMessage(restaurant, "/topic/restaurants/restaurant_2/orders"),
                channel
        )).isInstanceOf(AccessDeniedException.class)
          .hasMessage("Không được theo dõi dữ liệu realtime của nhà hàng khác");
    }

    @Test
    void restaurantCanSubscribeOnlyToItsScopedTopic() {
        User restaurant = user("OWNER-1", "restaurant", "rest_2");
        Message<?> message = subscribedMessage(restaurant, "/topic/restaurants/rest_2/orders");

        assertThat(interceptor.preSend(message, channel)).isSameAs(message);
    }

    @Test
    void globalOrderTopicIsAlwaysRejected() {
        User admin = user("ADMIN-1", "admin", null);
        assertThatThrownBy(() -> interceptor.preSend(
                subscribedMessage(admin, "/topic/orders"),
                channel
        )).isInstanceOf(AccessDeniedException.class)
          .hasMessage("Kênh realtime không được phép");
    }

    private Message<byte[]> message(StompCommand command, String destination, String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (destination != null) accessor.setDestination(destination);
        if (authorization != null) accessor.setNativeHeader("Authorization", authorization);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<byte[]> subscribedMessage(User user, String destination) {
        Message<byte[]> message = message(StompCommand.SUBSCRIBE, destination, null);
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user.getId(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase()))
                );
        authentication.setDetails(user);
        accessor.setUser(authentication);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private User user(String id, String role, String restaurantId) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setRestaurantId(restaurantId);
        return user;
    }
}
