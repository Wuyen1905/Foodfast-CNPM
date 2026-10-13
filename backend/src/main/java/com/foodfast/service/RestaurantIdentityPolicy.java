package com.foodfast.service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Defines the canonical restaurant IDs used by the backend while preserving
 * compatibility with legacy aliases that older web clients stored in orders.
 */
public final class RestaurantIdentityPolicy {

    public static final String SWEET_DREAMS_ID = "rest_2";
    public static final String ALOHA_ID = "restaurant_2";

    private RestaurantIdentityPolicy() {
    }

    public static String canonicalize(String restaurantId) {
        if (restaurantId == null) {
            return null;
        }

        String trimmed = restaurantId.trim();
        String normalized = trimmed.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "sweetdreams", "rest_2" -> SWEET_DREAMS_ID;
            case "aloha", "restaurant_2" -> ALOHA_ID;
            default -> trimmed;
        };
    }

    public static boolean refersToSameRestaurant(String first, String second) {
        return Objects.equals(canonicalize(first), canonicalize(second));
    }

    public static List<String> equivalentIds(String restaurantId) {
        String canonical = canonicalize(restaurantId);
        if (SWEET_DREAMS_ID.equals(canonical)) {
            return List.of(SWEET_DREAMS_ID, "sweetdreams");
        }
        if (ALOHA_ID.equals(canonical)) {
            return List.of(ALOHA_ID, "aloha");
        }
        return canonical == null ? List.of() : List.of(canonical);
    }

    public static String productLabel(String restaurantId) {
        String canonical = canonicalize(restaurantId);
        if (SWEET_DREAMS_ID.equals(canonical)) return "SweetDreams";
        if (ALOHA_ID.equals(canonical)) return "Aloha";
        return canonical;
    }
}
