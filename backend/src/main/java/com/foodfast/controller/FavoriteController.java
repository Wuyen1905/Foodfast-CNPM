package com.foodfast.controller;

import com.foodfast.entity.Product;
import com.foodfast.security.CurrentUserService;
import com.foodfast.service.FavoriteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final CurrentUserService currentUserService;

    public FavoriteController(FavoriteService favoriteService, CurrentUserService currentUserService) {
        this.favoriteService = favoriteService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<Product> list() {
        return favoriteService.list(currentUserService.requireUser().getId());
    }

    @GetMapping("/ids")
    public List<String> listIds() {
        return favoriteService.listIds(currentUserService.requireUser().getId());
    }

    @PostMapping("/{productId}")
    public List<String> add(@PathVariable String productId) {
        return favoriteService.add(currentUserService.requireUser().getId(), productId);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<List<String>> remove(@PathVariable String productId) {
        return ResponseEntity.ok(favoriteService.remove(currentUserService.requireUser().getId(), productId));
    }
}
