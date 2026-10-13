package com.foodfast.service;

import com.foodfast.entity.User;
import com.foodfast.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern VIETNAM_PHONE_PATTERN = Pattern.compile("^0\\d{9}$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User authenticate(String username, String password) {
        if (username == null || username.isBlank() || password == null) {
            throw new RuntimeException("Thông tin đăng nhập không hợp lệ");
        }

        String identity = username.trim();
        String normalizedIdentity = identity.toLowerCase(Locale.ROOT);
        Optional<User> matchedUser = userRepository.findByUsernameIgnoreCase(normalizedIdentity);
        if (matchedUser.isEmpty()) {
            matchedUser = userRepository.findByEmailIgnoreCase(normalizedIdentity);
        }
        if (matchedUser.isEmpty()) {
            String normalizedPhone = normalizePhoneOrNull(identity);
            if (normalizedPhone != null) {
                matchedUser = userRepository.findByPhone(normalizedPhone);
            }
        }

        User user = matchedUser.orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if ("suspended".equalsIgnoreCase(user.getRole())) {
            throw new RuntimeException("Tài khoản đã bị tạm ngưng");
        }

        String storedPassword = user.getPassword();
        boolean encoded = storedPassword != null && storedPassword.startsWith("$2");
        boolean matches = encoded
                ? passwordEncoder.matches(password, storedPassword)
                : storedPassword != null && storedPassword.equals(password);
        if (!matches) {
            throw new RuntimeException("Mật khẩu không đúng");
        }

        // Tự nâng cấp dữ liệu cũ còn mật khẩu thô ngay lần đăng nhập đầu tiên.
        if (!encoded) {
            user.setPassword(passwordEncoder.encode(password));
        }
        user.setLastLoginAt(System.currentTimeMillis());
        userRepository.save(user);

        return user;
    }

    @Transactional
    public User register(String username, String password, String name, String email, String phone) {
        String normalizedName = name == null ? "" : name.trim();
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        String normalizedUsername = username == null || username.isBlank()
                ? normalizedEmail
                : username.trim().toLowerCase(Locale.ROOT);
        String normalizedPhone = normalizePhoneOrNull(phone);

        if (normalizedName.length() < 2) {
            throw new IllegalArgumentException("Họ tên phải có ít nhất 2 ký tự");
        }
        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new IllegalArgumentException("Email không hợp lệ");
        }
        if (normalizedPhone == null) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 6 ký tự");
        }

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }

        if (userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại");
        }

        if (userRepository.existsByPhone(normalizedPhone)) {
            throw new IllegalArgumentException("Số điện thoại đã được sử dụng");
        }

        User user = new User();
        user.setId("CUS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT));
        user.setUsername(normalizedUsername);
        user.setPassword(passwordEncoder.encode(password));
        user.setName(normalizedName);
        user.setEmail(normalizedEmail);
        user.setPhone(normalizedPhone);
        user.setRole("customer");
        user.setOrderCount(0);
        user.setCreatedAt(System.currentTimeMillis());

        try {
            // Flush inside the transaction so a concurrent duplicate is returned as
            // a clear registration error instead of becoming an HTTP 500 at commit time.
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException(
                    "Email, số điện thoại hoặc tên đăng nhập đã được sử dụng",
                    exception
            );
        }
    }

    private String normalizePhoneOrNull(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }

        String normalized = phone.trim().replaceAll("[\\s.()-]", "");
        if (normalized.startsWith("+84")) {
            normalized = "0" + normalized.substring(3);
        } else if (normalized.startsWith("84") && normalized.length() == 11) {
            normalized = "0" + normalized.substring(2);
        }

        return VIETNAM_PHONE_PATTERN.matcher(normalized).matches() ? normalized : null;
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public void updateUserRole(String userId, String newRole) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setRole(newRole);
            userRepository.save(user);
        }
    }
}

