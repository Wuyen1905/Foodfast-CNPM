package com.foodfast.service;

import com.foodfast.entity.Favorite;
import com.foodfast.entity.Product;
import com.foodfast.repository.FavoriteRepository;
import com.foodfast.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;

    public FavoriteService(FavoriteRepository favoriteRepository, ProductRepository productRepository) {
        this.favoriteRepository = favoriteRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<Product> list(String userId) {
        List<Favorite> favorites = favoriteRepository.findByUserIdOrderByCreatedAtDesc(requireUserId(userId));
        Map<String, Product> products = new LinkedHashMap<>();
        productRepository.findAllById(favorites.stream().map(Favorite::getProductId).toList())
                .forEach(product -> products.put(product.getId(), product));
        return favorites.stream()
                .map(favorite -> products.get(favorite.getProductId()))
                .filter(product -> product != null)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> listIds(String userId) {
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(requireUserId(userId)).stream()
                .map(Favorite::getProductId)
                .toList();
    }

    @Transactional
    public List<String> add(String userId, String productId) {
        String ownerId = requireUserId(userId);
        String normalizedProductId = requireProduct(productId).getId();
        if (favoriteRepository.findByUserIdAndProductId(ownerId, normalizedProductId).isEmpty()) {
            Favorite favorite = new Favorite();
            favorite.setUserId(ownerId);
            favorite.setProductId(normalizedProductId);
            favorite.setCreatedAt(System.currentTimeMillis());
            favoriteRepository.save(favorite);
        }
        return listIds(ownerId);
    }

    @Transactional
    public List<String> remove(String userId, String productId) {
        String ownerId = requireUserId(userId);
        favoriteRepository.deleteByUserIdAndProductId(ownerId, requireText(productId, "Mã món ăn không hợp lệ"));
        return listIds(ownerId);
    }

    private Product requireProduct(String productId) {
        return productRepository.findById(requireText(productId, "Mã món ăn không hợp lệ"))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy món ăn"));
    }

    private String requireUserId(String userId) {
        return requireText(userId, "Không xác định được khách hàng");
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) throw new IllegalArgumentException(message);
        return value.trim();
    }
}
