package com.foodfast.e2e;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodfast.entity.Restaurant;
import com.foodfast.repository.RestaurantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MultiRestaurantOrderGroupE2ETest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private RestaurantRepository restaurantRepository;

    @Test
    @Timeout(30)
    void checkoutSplitsCartAndKeepsRestaurantOrdersIsolated() throws Exception {
        keepRestaurantOpen("rest_2");
        keepRestaurantOpen("restaurant_2");
        String customerToken = login("user", "user123");
        String sweetDreamsToken = login("sweetdreams", "sweet123");
        String alohaToken = login("aloha_restaurant", "aloha123");
        String requestKey = "multi-restaurant-" + UUID.randomUUID();

        String request = """
                {
                  "customerPhone":"0123456789",
                  "address":"123 Đường Đa Nhà Hàng, TP.HCM",
                  "paymentMethod":"cod",
                  "items":[
                    {"productId":"sd-2","quantity":1},
                    {"productId":"ak-3","quantity":1}
                  ]
                }
                """;

        mockMvc.perform(post("/api/order-groups/quote")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders.length()").value(2))
                .andExpect(jsonPath("$.subtotal").value(124_000))
                .andExpect(jsonPath("$.shippingFee").value(30_000))
                .andExpect(jsonPath("$.taxAmount").value(0))
                .andExpect(jsonPath("$.total").value(154_000));

        String createdBody = mockMvc.perform(post("/api/order-groups")
                        .header("Authorization", bearer(customerToken))
                        .header("Idempotency-Key", requestKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderCount").value(2))
                .andExpect(jsonPath("$.orders.length()").value(2))
                .andExpect(jsonPath("$.orders[0].orderGroupId").isNotEmpty())
                .andExpect(jsonPath("$.orders[1].orderGroupId").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> group = readMap(createdBody);
        String groupId = String.valueOf(group.get("id"));
        List<Map<String, Object>> orders = objectMapper.convertValue(
                group.get("orders"), new TypeReference<>() { }
        );
        assertThat(orders).hasSize(2);
        assertThat(orders).extracting(item -> item.get("orderGroupId"))
                .containsOnly(groupId);
        assertThat(orders).extracting(item -> item.get("restaurantId"))
                .containsExactlyInAnyOrder("rest_2", "restaurant_2");
        assertThat(orders).extracting(item -> ((Number) item.get("shippingFee")).intValue())
                .containsOnly(15_000);

        Map<String, Object> sweetDreamsOrder = orderFor(orders, "rest_2");
        Map<String, Object> alohaOrder = orderFor(orders, "restaurant_2");
        String sweetDreamsOrderId = String.valueOf(sweetDreamsOrder.get("id"));
        String alohaOrderId = String.valueOf(alohaOrder.get("id"));

        mockMvc.perform(get("/api/orders/{id}", sweetDreamsOrderId)
                        .header("Authorization", bearer(sweetDreamsToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/orders/{id}", alohaOrderId)
                        .header("Authorization", bearer(sweetDreamsToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/orders/{id}", alohaOrderId)
                        .header("Authorization", bearer(alohaToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/orders/{id}", sweetDreamsOrderId)
                        .header("Authorization", bearer(alohaToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/order-groups/{id}", groupId)
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders.length()").value(2));

        mockMvc.perform(post("/api/order-groups")
                        .header("Authorization", bearer(customerToken))
                        .header("Idempotency-Key", requestKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(groupId))
                .andExpect(jsonPath("$.orders.length()").value(2));
    }

    private Map<String, Object> orderFor(List<Map<String, Object>> orders, String restaurantId) {
        return orders.stream()
                .filter(item -> restaurantId.equals(item.get("restaurantId")))
                .findFirst()
                .orElseThrow();
    }

    private void keepRestaurantOpen(String restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId).orElseThrow();
        restaurant.setIsActive(true);
        restaurant.setAcceptingOrders(true);
        restaurant.setOpeningTime("00:00");
        restaurant.setClosingTime("00:00");
        restaurantRepository.save(restaurant);
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
