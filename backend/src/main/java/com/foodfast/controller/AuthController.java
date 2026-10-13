package com.foodfast.controller;

import com.foodfast.entity.User;
import com.foodfast.repository.UserRepository;
import com.foodfast.service.AuthService;
import com.foodfast.security.CurrentUserService;
import com.foodfast.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("username", user.getUsername());
            userMap.put("role", user.getRole());
            userMap.put("restaurantId", user.getRestaurantId());
            userMap.put("name", user.getName());
            userMap.put("email", user.getEmail());
            userMap.put("phone", user.getPhone());
            userMap.put("orderCount", user.getOrderCount());
            userMap.put("createdAt", user.getCreatedAt());

            Map<String, Object> response = new HashMap<>();
            response.put("ok", true);
            response.put("token", jwtService.createToken(user));
            response.put("user", userMap);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("ok", false, "message", "Sai tên đăng nhập hoặc mật khẩu"));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me() {
        User user = currentUserService.requireUser();
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("username", user.getUsername());
        userMap.put("role", user.getRole());
        userMap.put("restaurantId", user.getRestaurantId());
        userMap.put("name", user.getName());
        userMap.put("email", user.getEmail());
        userMap.put("phone", user.getPhone());
        userMap.put("orderCount", user.getOrderCount());
        userMap.put("createdAt", user.getCreatedAt());
        return ResponseEntity.ok(userMap);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            if (!StringUtils.hasText(request.password) || !StringUtils.hasText(request.fullName) ||
                !StringUtils.hasText(request.email) ||
                !StringUtils.hasText(request.phone)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("ok", false, "message", "Vui lòng điền đầy đủ thông tin"));
            }

            User user = authService.register(
                    request.username,
                    request.password,
                    request.fullName,
                    request.email,
                    request.phone
            );

            Map<String, Object> response = new HashMap<>();
            response.put("ok", true);
            response.put("data", user);
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
        public String username;
        public String password;
        public String fullName;
        public String email;
        public String phone;
    }
}

