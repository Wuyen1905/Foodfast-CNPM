package com.foodfast.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestaurantIdentityPolicyTest {

    @Test
    void legacyAliasesAreConvertedToCanonicalIds() {
        assertEquals("rest_2", RestaurantIdentityPolicy.canonicalize("sweetdreams"));
        assertEquals("restaurant_2", RestaurantIdentityPolicy.canonicalize("Aloha"));
    }

    @Test
    void legacyAndCanonicalIdsReferToSameRestaurant() {
        assertTrue(RestaurantIdentityPolicy.refersToSameRestaurant("sweetdreams", "rest_2"));
        assertTrue(RestaurantIdentityPolicy.refersToSameRestaurant("aloha", "restaurant_2"));
        assertFalse(RestaurantIdentityPolicy.refersToSameRestaurant("sweetdreams", "restaurant_2"));
    }

    @Test
    void readsIncludeCanonicalIdAndLegacyAlias() {
        assertEquals(
                List.of("rest_2", "sweetdreams"),
                RestaurantIdentityPolicy.equivalentIds("rest_2")
        );
    }
}
