package com.foodfast.e2e;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodfast.entity.Restaurant;
import com.foodfast.repository.RestaurantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API-level end-to-end acceptance test for the core FoodFast business journey.
 * It crosses authentication, checkout, restaurant processing, delivery,
 * customer confirmation, history and durable customer notifications.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderDeliveryE2ETest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private RestaurantRepository restaurantRepository;

    @Test
    void customerCanOrderAndRestaurantCanDeliverWithCompleteAuditTrail() throws Exception {
        Restaurant restaurant = restaurantRepository.findById("rest_2").orElseThrow();
        restaurant.setIsActive(true);
        restaurant.setAcceptingOrders(true);
        restaurant.setOpeningTime("00:00");
        restaurant.setClosingTime("00:00");
        restaurantRepository.save(restaurant);

        String customerToken = login("user", "user123");
        String restaurantToken = login("sweetdreams", "sweet123");
        String requestKey = "e2e-" + UUID.randomUUID();

        String createdBody = mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(customerToken))
                        .header("Idempotency-Key", requestKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerPhone":"0123456789",
                                  "address":"123 Đường E2E, TP.HCM",
                                  "restaurantId":"rest_2",
                                  "paymentMethod":"cod",
                                  "items":[{"productId":"sd-2","quantity":1}]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("Pending"))
                .andReturn().getResponse().getContentAsString();
        String orderId = String.valueOf(readMap(createdBody).get("id"));

        patchStatus(orderId, "CONFIRMED", restaurantToken, "Confirmed");
        patchStatus(orderId, "PREPARING", restaurantToken, "In Progress");
        patchStatus(orderId, "READY", restaurantToken, "Ready");

        String assignedBody = mockMvc.perform(post("/api/deliveries/assign")
                        .header("Authorization", bearer(restaurantToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "orderId", orderId,
                                "courierId", "COURIER-SD-001",
                                "estimatedDeliveryAt", System.currentTimeMillis() + 30 * 60 * 1000L,
                                "deliveryNote", "Gọi khách trước khi đến"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andReturn().getResponse().getContentAsString();
        String assignmentId = String.valueOf(readMap(assignedBody).get("id"));

        mockMvc.perform(post("/api/deliveries/{id}/start", assignmentId)
                        .header("Authorization", bearer(restaurantToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERING"));

        mockMvc.perform(post("/api/deliveries/{id}/customer-confirm", assignmentId)
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"))
                .andExpect(jsonPath("$.completionSource").value("CUSTOMER"));

        String historyBody = mockMvc.perform(get("/api/orders/{id}/history", orderId)
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> history = objectMapper.readValue(historyBody, new TypeReference<>() { });
        assertThat(history).extracting(item -> item.get("toStatus"))
                .containsExactly("Pending", "Confirmed", "In Progress", "Ready", "Delivering", "Delivered");
        assertThat(history.get(history.size() - 1).get("source")).isEqualTo("CUSTOMER_CONFIRMATION");

        String notificationsBody = mockMvc.perform(get("/api/customer-notifications")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Map<String, Object> notificationPage = readMap(notificationsBody);
        List<Map<String, Object>> notifications = objectMapper.convertValue(
                notificationPage.get("notifications"), new TypeReference<>() { }
        );
        assertThat(((Number) notificationPage.get("unreadCount")).intValue()).isPositive();
        assertThat(notifications).anyMatch(item -> "DELIVERY_ASSIGNED".equals(item.get("type")));
        assertThat(notifications).anyMatch(item -> "ORDER_DELIVERED".equals(item.get("type")));

        String notificationId = String.valueOf(notifications.get(0).get("id"));
        mockMvc.perform(patch("/api/customer-notifications/{id}/read", notificationId)
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));

        mockMvc.perform(patch("/api/customer-notifications/read-all")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").isNumber());

        mockMvc.perform(get("/api/customer-notifications")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));
    }

    private void patchStatus(String orderId, String statusValue, String token, String expected) throws Exception {
        mockMvc.perform(patch("/api/orders/{id}", orderId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", statusValue))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(expected));
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
        return String.valueOf(readMap(response).get("token"));
    }

    private Map<String, Object> readMap(String body) throws Exception {
        return objectMapper.readValue(body, new TypeReference<>() { });
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
