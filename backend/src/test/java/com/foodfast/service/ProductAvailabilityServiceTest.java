package com.foodfast.service;

import com.foodfast.entity.Product;
import com.foodfast.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductAvailabilityServiceTest {

    private ProductRepository repository;
    private ProductAvailabilityService service;

    @BeforeEach
    void setUp() {
        repository = mock(ProductRepository.class);
        service = new ProductAvailabilityService(repository);
    }

    @Test
    void exposesDailySoldAndRemainingQuantity() {
        Product product = product(10, true);
        when(repository.countReservedQuantity(eq("P-1"), any(Instant.class), any(Instant.class)))
                .thenReturn(7L);

        Product result = service.snapshot(product);

        assertThat(result.getSoldToday()).isEqualTo(7);
        assertThat(result.getRemainingToday()).isEqualTo(3);
        assertThat(result.isSoldOutToday()).isFalse();
    }

    @Test
    void rejectsQuantityGreaterThanRemainingDailyLimit() {
        Product product = product(10, true);
        when(repository.findByIdForUpdate("P-1")).thenReturn(Optional.of(product));
        when(repository.countReservedQuantity(eq("P-1"), any(Instant.class), any(Instant.class)))
                .thenReturn(9L);

        assertThatThrownBy(() -> service.requireOrderable("P-1", 2, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("chỉ còn 1 phần");
    }

    @Test
    void rejectsManuallyPausedProductEvenWithoutDailyLimit() {
        Product product = product(null, false);
        when(repository.findById("P-1")).thenReturn(Optional.of(product));
        when(repository.countReservedQuantity(eq("P-1"), any(Instant.class), any(Instant.class)))
                .thenReturn(0L);

        assertThatThrownBy(() -> service.requireOrderable("P-1", 1, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tạm ngừng bán");
    }

    private Product product(Integer dailyLimit, boolean available) {
        Product product = new Product();
        product.setId("P-1");
        product.setName("Món thử nghiệm");
        product.setAvailable(available);
        product.setDailyLimit(dailyLimit);
        return product;
    }
}
