package com.foodfast.repository;

import com.foodfast.entity.ProductOptionGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductOptionGroupRepository extends JpaRepository<ProductOptionGroup, String> {
    List<ProductOptionGroup> findByProduct_IdAndActiveTrueOrderByDisplayOrderAscNameAsc(String productId);
    List<ProductOptionGroup> findByProduct_IdOrderByDisplayOrderAscNameAsc(String productId);
    void deleteByProduct_Id(String productId);
}
