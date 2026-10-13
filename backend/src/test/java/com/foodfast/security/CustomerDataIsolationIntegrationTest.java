package com.foodfast.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerDataIsolationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void favoritesAndAddressesAlwaysUseJwtCustomer() throws Exception {
        String firstCustomer = login("user", "user123");
        String secondCustomer = login("user1", "user1123");

        mockMvc.perform(post("/api/favorites/sd-2")
                        .header("Authorization", "Bearer " + firstCustomer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasItem("sd-2")));

        mockMvc.perform(get("/api/favorites/ids")
                        .header("Authorization", "Bearer " + secondCustomer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(hasItem("sd-2"))));

        String created = mockMvc.perform(post("/api/addresses")
                        .header("Authorization", "Bearer " + firstCustomer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId":"u4",
                                  "label":"Nhà riêng",
                                  "recipientName":"Khách hàng mẫu",
                                  "phone":"0123456789",
                                  "addressLine":"123 Đường kiểm thử, Quận 1, TP.HCM",
                                  "defaultAddress":true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value("u2"))
                .andExpect(jsonPath("$.defaultAddress").value(true))
                .andReturn().getResponse().getContentAsString();

        String addressId = String.valueOf(objectMapper.readValue(
                created,
                new TypeReference<Map<String, Object>>() { }
        ).get("id"));

        mockMvc.perform(get("/api/addresses")
                        .header("Authorization", "Bearer " + secondCustomer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem(addressId))));

        mockMvc.perform(put("/api/addresses/{id}", addressId)
                        .header("Authorization", "Bearer " + secondCustomer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "label":"Chiếm quyền",
                                  "recipientName":"Người khác",
                                  "phone":"0912345678",
                                  "addressLine":"999 Đường không được phép, TP.HCM",
                                  "defaultAddress":true
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/api/addresses/{id}", addressId)
                        .header("Authorization", "Bearer " + secondCustomer))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/addresses")
                        .header("Authorization", "Bearer " + firstCustomer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(addressId)));
    }

    private String login(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Map<String, Object> body = objectMapper.readValue(response, new TypeReference<>() { });
        return String.valueOf(body.get("token"));
    }
}
