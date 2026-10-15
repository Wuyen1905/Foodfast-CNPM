package com.foodfast.service;

import com.foodfast.dto.VoucherUpsertRequest;
import com.foodfast.entity.Voucher;
import com.foodfast.entity.VoucherDiscountType;
import com.foodfast.entity.VoucherScope;
import com.foodfast.exception.ForbiddenOperationException;
import com.foodfast.repository.RestaurantRepository;
import com.foodfast.repository.VoucherRedemptionRepository;
import com.foodfast.repository.VoucherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class VoucherManagementServiceTest {

    @Mock VoucherRepository voucherRepository;
    @Mock VoucherRedemptionRepository redemptionRepository;
    @Mock RestaurantRepository restaurantRepository;

    private VoucherManagementService service;
    private Instant now;

    @BeforeEach
    void setUp() {
        service = new VoucherManagementService(
                voucherRepository, redemptionRepository, restaurantRepository
        );
        now = Instant.parse("2026-10-07T01:00:00Z");
        lenient().when(voucherRepository.save(any(Voucher.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void restaurantCannotSpoofVoucherOwner() {
        when(restaurantRepository.existsById("restaurant_2")).thenReturn(true);
        when(voucherRepository.findByCodeIgnoreCase("ALOHA10")).thenReturn(Optional.empty());

        VoucherUpsertRequest request = request("ALOHA10", "rest_2", VoucherDiscountType.PERCENT, 10);
        var created = service.create(request, "restaurant_2", false);

        assertThat(created.restaurantId()).isEqualTo("restaurant_2");
        assertThat(created.code()).isEqualTo("ALOHA10");
    }

    @Test
    void restaurantCannotEditAnotherRestaurantsVoucher() {
        Voucher voucher = voucher("SWEET10", "rest_2");
        when(voucherRepository.findByCodeIgnoreCase("SWEET10")).thenReturn(Optional.of(voucher));

        assertThatThrownBy(() -> service.update(
                "SWEET10",
                request("SWEET10", "rest_2", VoucherDiscountType.PERCENT, 10),
                "restaurant_2",
                false
        )).isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("không có quyền");
    }

    @Test
    void adminMayCreateSystemWideVoucher() {
        when(voucherRepository.findByCodeIgnoreCase("GLOBAL20")).thenReturn(Optional.empty());

        var created = service.create(
                request("GLOBAL20", null, VoucherDiscountType.PERCENT, 20),
                null,
                true
        );

        assertThat(created.restaurantId()).isNull();
    }

    @Test
    void rejectsInvalidPercentAndUsageRules() {
        when(voucherRepository.findByCodeIgnoreCase("BAD150")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(
                request("BAD150", null, VoucherDiscountType.PERCENT, 150), null, true
        )).hasMessageContaining("100%");

        VoucherUpsertRequest invalidUsage = new VoucherUpsertRequest(
                "LIMIT", "Giới hạn", null, VoucherScope.GLOBAL, VoucherDiscountType.FIXED, 10_000,
                0, 0, now, now.plus(2, ChronoUnit.DAYS), 1, 2, true
        );
        when(voucherRepository.findByCodeIgnoreCase("LIMIT")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(invalidUsage, null, true))
                .hasMessageContaining("không được vượt tổng");
    }

    private VoucherUpsertRequest request(
            String code,
            String restaurantId,
            VoucherDiscountType type,
            int value
    ) {
        return new VoucherUpsertRequest(
                code, "Chương trình " + code, restaurantId,
                restaurantId == null ? VoucherScope.GLOBAL : VoucherScope.RESTAURANT,
                type, value,
                50_000, type == VoucherDiscountType.PERCENT ? 40_000 : 0,
                now, now.plus(30, ChronoUnit.DAYS), 100, 1, true
        );
    }

    private Voucher voucher(String code, String restaurantId) {
        Voucher voucher = new Voucher();
        voucher.setCode(code);
        voucher.setName(code);
        voucher.setRestaurantId(restaurantId);
        voucher.setDiscountType(VoucherDiscountType.PERCENT);
        voucher.setDiscountValue(10);
        voucher.setMinOrderAmount(0);
        voucher.setMaxDiscount(20_000);
        voucher.setStartsAt(now);
        voucher.setEndsAt(now.plus(30, ChronoUnit.DAYS));
        voucher.setUsageLimit(100);
        voucher.setPerUserLimit(1);
        voucher.setActive(true);
        return voucher;
    }
}
