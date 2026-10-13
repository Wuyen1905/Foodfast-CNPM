package com.foodfast.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodfast.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class JwtService {

    private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final long expirationSeconds;

    public JwtService(
            ObjectMapper objectMapper,
            @Value("${auth.jwt.secret}") String secret,
            @Value("${auth.jwt.expirationMinutes:480}") long expirationMinutes
    ) {
        this.objectMapper = objectMapper;
        if (secret == null || secret.isBlank()) {
            byte[] generatedSecret = new byte[48];
            new SecureRandom().nextBytes(generatedSecret);
            this.secret = generatedSecret;
            System.err.println("[AUTH] AUTH_JWT_SECRET chưa được đặt; đang dùng khóa ngẫu nhiên cho phiên chạy hiện tại.");
        } else {
            if (secret.length() < 32) {
                throw new IllegalStateException("AUTH_JWT_SECRET phải có ít nhất 32 ký tự");
            }
            this.secret = secret.getBytes(StandardCharsets.UTF_8);
        }
        this.expirationSeconds = Math.max(15, expirationMinutes) * 60;
    }

    public String createToken(User user) {
        try {
            Instant now = Instant.now();
            Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sub", user.getId());
            payload.put("role", user.getRole());
            payload.put("restaurantId", user.getRestaurantId());
            payload.put("iat", now.getEpochSecond());
            payload.put("exp", now.plusSeconds(expirationSeconds).getEpochSecond());

            String encodedHeader = encodeJson(header);
            String encodedPayload = encodeJson(payload);
            String signingInput = encodedHeader + "." + encodedPayload;
            return signingInput + "." + BASE64_URL.encodeToString(sign(signingInput));
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể tạo phiên đăng nhập", exception);
        }
    }

    public Claims verify(String token) {
        try {
            String[] parts = token == null ? new String[0] : token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Token không đúng định dạng");
            }

            String signingInput = parts[0] + "." + parts[1];
            byte[] providedSignature = BASE64_URL_DECODER.decode(parts[2]);
            if (!MessageDigest.isEqual(sign(signingInput), providedSignature)) {
                throw new IllegalArgumentException("Chữ ký token không hợp lệ");
            }

            Map<String, Object> header = objectMapper.readValue(
                    BASE64_URL_DECODER.decode(parts[0]), new TypeReference<>() { });
            if (!"HS256".equals(header.get("alg"))) {
                throw new IllegalArgumentException("Thuật toán token không hợp lệ");
            }

            Map<String, Object> payload = objectMapper.readValue(
                    BASE64_URL_DECODER.decode(parts[1]), new TypeReference<>() { });
            String userId = String.valueOf(payload.getOrDefault("sub", "")).trim();
            long expiresAt = number(payload.get("exp"));
            if (userId.isBlank() || expiresAt <= Instant.now().getEpochSecond()) {
                throw new IllegalArgumentException("Phiên đăng nhập đã hết hạn");
            }
            return new Claims(userId, expiresAt);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Token không hợp lệ", exception);
        }
    }

    private String encodeJson(Map<String, Object> value) throws Exception {
        return BASE64_URL.encodeToString(objectMapper.writeValueAsBytes(value));
    }

    private byte[] sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    }

    private long number(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    public record Claims(String userId, long expiresAtEpochSecond) { }
}
