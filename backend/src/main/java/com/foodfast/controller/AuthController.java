package com.foodfast.controller;

import com.foodfast.entity.User;
import com.foodfast.repository.UserRepository;
import com.foodfast.service.AuthService;
import com.foodfast.security.CurrentUserService;
import com.foodfast.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://192.168.0.100:5173",
        "http://192.168.0.100:5174",
        "http://192.168.0.100:5175",
        "http://localhost:8081"
})
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final CurrentUserService currentUserService;

    public AuthController(
            AuthService authService,
            UserRepository userRepository,
            JwtService jwtService,
            CurrentUserService currentUserService
    ) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(users);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            User user = authService.authenticate(request.username, request.password);

            Map<String, Object> response = new HashMap<>();
            response.put("ok", true);
            response.put("token", jwtService.createToken(user));
            response.put("user", safeUser(user));

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("ok", false, "message", "Sai tên đăng nhập hoặc mật khẩu"));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me() {
        User user = currentUserService.requireUser();
        return ResponseEntity.ok(safeUser(user));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = authService.register(
                    request.password,
                    request.fullName,
                    request.email,
                    request.phone
            );

            Map<String, Object> response = new HashMap<>();
            response.put("ok", true);
            response.put("data", safeUser(user));
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("ok", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("ok", false, "message", "Có lỗi xảy ra, vui lòng thử lại"));
        }
    }

    public static class LoginRequest {
        public String username;
        public String password;
    }

    public static class RegisterRequest {
        @NotBlank(message = "Vui lòng nhập mật khẩu")
        @Size(min = 8, max = 64, message = "Mật khẩu phải từ 8 đến 64 ký tự")
        @Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d)\\S+$", message = "Mật khẩu phải có ít nhất một chữ và một số, không chứa khoảng trắng")
        public String password;

        @NotBlank(message = "Vui lòng nhập họ tên")
        @Size(min = 2, max = 100, message = "Họ tên phải từ 2 đến 100 ký tự")
        @Pattern(regexp = "^[\\p{L}\\p{M}][\\p{L}\\p{M} .'-]*$", message = "Họ tên chỉ được gồm chữ, khoảng trắng, dấu nháy hoặc gạch nối")
        public String fullName;

        @NotBlank(message = "Vui lòng nhập email")
        @Size(max = 100, message = "Email tối đa 100 ký tự")
        @Email(message = "Email không hợp lệ")
        public String email;

        @NotBlank(message = "Vui lòng nhập số điện thoại")
        @Size(max = 20, message = "Số điện thoại không hợp lệ")
        public String phone;
    }

    private Map<String, Object> safeUser(User user) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("role", user.getRole());
        result.put("restaurantId", user.getRestaurantId());
        result.put("name", user.getName());
        result.put("email", user.getEmail());
        result.put("phone", user.getPhone());
        result.put("orderCount", user.getOrderCount());
        result.put("createdAt", user.getCreatedAt());
        return result;
    }
}

