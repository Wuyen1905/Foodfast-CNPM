package com.foodfast.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodfast.entity.User;
import com.foodfast.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ManagerAccountIntegrationTest {

    private static final String CREATED_USERNAME = "manager_integration_test";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @AfterEach
    void cleanup() {
        userRepository.findByUsername(CREATED_USERNAME).ifPresent(userRepository::delete);
        userRepository.findById("manager_1").ifPresent(user -> {
            user.setAccountStatus("active");
            user.setSuspensionReason(null);
            userRepository.save(user);
        });
    }

    @Test
    void demoManagerCanLoginButCannotUseAdminEndpoints() throws Exception {
        String token = login("quanly", "quanly123");

        mockMvc.perform(get("/api/admin/managers")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void demoManagersShareTheSameShiftPermissions() throws Exception {
        String firstShiftToken = login("quanly", "quanly123");
        String secondShiftToken = login("quanly1", "quanly123");
        String query = "?periodStart=2026-10-01T00:00:00Z&periodEnd=2026-11-01T00:00:00Z";

        mockMvc.perform(get("/api/manager/settlements/dashboard" + query)
                        .header("Authorization", "Bearer " + firstShiftToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingReviewCount").isNumber())
                .andExpect(jsonPath("$.settlements").isArray());

        mockMvc.perform(get("/api/manager/settlements/dashboard" + query)
                        .header("Authorization", "Bearer " + secondShiftToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingReviewCount").isNumber())
                .andExpect(jsonPath("$.settlements").isArray());

        mockMvc.perform(get("/api/admin/managers")
                        .header("Authorization", "Bearer " + secondShiftToken))
                .andExpect(status().isForbidden());

        String expectedPermissions = "ORDER_MANAGEMENT,CUSTOMER_SUPPORT,PAYMENT_RECONCILIATION,RESTAURANT_SETTLEMENT";
        assertThat(userRepository.findByUsername("quanly").orElseThrow().getManagerPermissions())
                .isEqualTo(expectedPermissions);
        assertThat(userRepository.findByUsername("quanly1").orElseThrow().getManagerPermissions())
                .isEqualTo(expectedPermissions);
    }

    @Test
    void adminCanCreateManagerWithBcryptPasswordAndPermissions() throws Exception {
        String adminToken = login("admin", "admin123");

        mockMvc.perform(post("/api/admin/managers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"manager_integration_test",
                                  "password":"manager123",
                                  "name":"Quản lý kiểm thử",
                                  "email":"manager.integration@foodfast.local",
                                  "phone":"0900000098",
                                  "permissions":["ORDER_MANAGEMENT","CUSTOMER_SUPPORT"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("manager"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.password").doesNotExist());

        User stored = userRepository.findByUsername(CREATED_USERNAME).orElseThrow();
        assertThat(stored.getPassword()).startsWith("$2");
        assertThat(passwordEncoder.matches("manager123", stored.getPassword())).isTrue();
        assertThat(stored.getManagerPermissions()).contains("ORDER_MANAGEMENT", "CUSTOMER_SUPPORT");
    }

    @Test
    void lockedManagerCannotLoginAndCanBeReactivated() throws Exception {
        String adminToken = login("admin", "admin123");

        mockMvc.perform(patch("/api/admin/managers/manager_1/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false,\"reason\":\"Tạm khóa để kiểm thử\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"quanly\",\"password\":\"quanly123\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/admin/managers/manager_1/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":true,\"reason\":\"Mở lại sau kiểm thử\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    private String login(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.path("token").asText();
    }
}
