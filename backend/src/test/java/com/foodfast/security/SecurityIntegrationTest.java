package com.foodfast.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodfast.entity.Restaurant;
import com.foodfast.repository.RestaurantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Test
    void protectedApiRejectsAnonymousAndCustomerCannotBecomeAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isUnauthorized());

        String customerToken = login("user", "user123");
        mockMvc.perform(get("/api/admin/stats")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("u2"))
                .andExpect(jsonPath("$.role").value("customer"));
    }

    @Test
    void cartIgnoresSpoofedUserIdAndUsesJwtOwner() throws Exception {
        String customerToken = login("user", "user123");

        mockMvc.perform(post("/api/cart/add")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "u4",
                                  "productId": "sd-2",
                                  "quantity": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value("u2"));
    }

    @Test
    void cartAcceptsProductImageUrlLongerThanLegacyColumnLimit() throws Exception {
        String customerToken = login("user", "user123");

        mockMvc.perform(post("/api/cart/add")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "sd-1",
                                  "quantity": 1,
                                  "selectedOptionIds": ["sd-1-sugar"]
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void restaurantCannotReadAnotherRestaurantsDeliveries() throws Exception {
        String sweetDreamsToken = login("sweetdreams", "sweet123");

        mockMvc.perform(get("/api/restaurants/rest_1/deliveries")
                        .header("Authorization", "Bearer " + sweetDreamsToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void promotionAndToppingManagementAreScopedToRestaurant() throws Exception {
        String sweetDreamsToken = login("sweetdreams", "sweet123");
        String customerToken = login("user", "user123");

        mockMvc.perform(get("/api/vouchers/manage")
                        .header("Authorization", "Bearer " + sweetDreamsToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("SWEET10"));

        mockMvc.perform(get("/api/vouchers/manage")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/vouchers/manage/ALOHA30K/active")
                        .header("Authorization", "Bearer " + sweetDreamsToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/products/sd-1/option-groups/manage")
                        .header("Authorization", "Bearer " + sweetDreamsToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/products/ak-2/option-groups/manage")
                        .header("Authorization", "Bearer " + sweetDreamsToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotReadAnotherCustomersOrder() throws Exception {
        String otherCustomerToken = login("user1", "user1123");

        mockMvc.perform(get("/api/orders/ORDER-001")
                        .header("Authorization", "Bearer " + otherCustomerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void restaurantCannotUpdateAnotherRestaurantsOrder() throws Exception {
        String sweetDreamsToken = login("sweetdreams", "sweet123");

        mockMvc.perform(patch("/api/orders/ORDER-002")
                        .header("Authorization", "Bearer " + sweetDreamsToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"CONFIRMED"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void restaurantCannotOverwriteServerOwnedOrderAmounts() throws Exception {
        String sweetDreamsToken = login("sweetdreams", "sweet123");

        mockMvc.perform(patch("/api/orders/ORDER-003")
                        .header("Authorization", "Bearer " + sweetDreamsToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"total":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("READ_ONLY_ORDER_FIELD"));
    }

    @Test
    void checkoutUsesServerPriceAndReplaysSameIdempotencyKey() throws Exception {
        // Opening equals closing means 24/7. The security test must not depend on
        // the wall-clock time at which the test suite happens to run.
        Restaurant restaurant = restaurantRepository.findById("rest_2").orElseThrow();
        restaurant.setOpeningTime("00:00");
        restaurant.setClosingTime("00:00");
        restaurant.setIsActive(true);
        restaurant.setAcceptingOrders(true);
        restaurantRepository.save(restaurant);

        String customerToken = login("user", "user123");
        String request = """
                {
                  "customerPhone":"0123456789",
                  "address":"123 Đường kiểm thử, TP.HCM",
                  "restaurantId":"rest_2",
                  "paymentMethod":"cod",
                  "items":[{
                    "productId":"sd-2",
                    "quantity":1,
                    "price":1,
                    "name":"Giá giả từ trình duyệt"
                  }]
                }
                """;

        String firstResponse = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", "security-price-test-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].price").value(55000))
                .andExpect(jsonPath("$.subtotal").value(55000))
                .andExpect(jsonPath("$.total").value(70000))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String orderId = String.valueOf(objectMapper.readValue(
                firstResponse,
                new TypeReference<Map<String, Object>>() { }
        ).get("id"));

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + customerToken)
                        .header("Idempotency-Key", "security-price-test-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId));
    }

    private String login(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Map<String, Object> body = objectMapper.readValue(response, new TypeReference<>() { });
        return String.valueOf(body.get("token"));
    }
}
