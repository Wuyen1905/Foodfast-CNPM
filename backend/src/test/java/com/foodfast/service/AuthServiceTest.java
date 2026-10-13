package com.foodfast.service;

import com.foodfast.entity.User;
import com.foodfast.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private UserRepository userRepository;
    private AuthService authService;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder(4);
        authService = new AuthService(userRepository, passwordEncoder);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void registerNormalizesCustomerIdentity() {
        User user = authService.register(
                null,
                "secret1",
                "  Nguyễn Văn A  ",
                "  Customer@Example.COM ",
                "+84 912 345 678"
        );

        assertThat(user.getId()).startsWith("CUS-");
        assertThat(user.getUsername()).isEqualTo("customer@example.com");
        assertThat(user.getEmail()).isEqualTo("customer@example.com");
        assertThat(user.getPhone()).isEqualTo("0912345678");
        assertThat(user.getName()).isEqualTo("Nguyễn Văn A");
        assertThat(user.getRole()).isEqualTo("customer");
        assertThat(user.getPassword()).startsWith("$2");
        assertThat(passwordEncoder.matches("secret1", user.getPassword())).isTrue();
    }

    @Test
    void customerCanAuthenticateWithEmail() {
        User user = customer();
        when(userRepository.findByUsernameIgnoreCase("customer@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("customer@example.com")).thenReturn(Optional.of(user));

        assertThat(authService.authenticate("Customer@Example.com", "secret1")).isSameAs(user);
    }

    @Test
    void customerCanAuthenticateWithNormalizedPhone() {
        User user = customer();
        when(userRepository.findByUsernameIgnoreCase("+84912345678")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("+84912345678")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("0912345678")).thenReturn(Optional.of(user));

        assertThat(authService.authenticate("+84912345678", "secret1")).isSameAs(user);
    }

    @Test
    void duplicateEmailIsRejectedIgnoringCase() {
        when(userRepository.existsByEmailIgnoreCase("customer@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                "customer@example.com",
                "secret1",
                "Nguyễn Văn A",
                "Customer@Example.com",
                "0912345678"
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Email đã được sử dụng");
    }

    @Test
    void invalidPhoneIsRejected() {
        assertThatThrownBy(() -> authService.register(
                "customer@example.com",
                "secret1",
                "Nguyễn Văn A",
                "customer@example.com",
                "123"
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Số điện thoại không hợp lệ");
    }

    @Test
    void duplicatePhoneIsRejected() {
        when(userRepository.existsByPhone("0912345678")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                "another@example.com",
                "secret1",
                "Nguyễn Văn B",
                "another@example.com",
                "0912345678"
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Số điện thoại đã được sử dụng");
    }

    @Test
    void databaseUniquenessRaceReturnsBusinessError() {
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        assertThatThrownBy(() -> authService.register(
                "race@example.com",
                "secret1",
                "Nguyễn Văn C",
                "race@example.com",
                "0909999999"
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Email, số điện thoại hoặc tên đăng nhập đã được sử dụng");
    }

    private User customer() {
        User user = new User();
        user.setId("CUS-TEST");
        user.setUsername("customer@example.com");
        user.setEmail("customer@example.com");
        user.setPhone("0912345678");
        user.setPassword("secret1");
        user.setName("Nguyễn Văn A");
        user.setRole("customer");
        return user;
    }
}
