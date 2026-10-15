package com.foodfast.service;

import com.foodfast.entity.Voucher;
import com.foodfast.entity.VoucherDiscountType;
import com.foodfast.entity.VoucherRedemptionStatus;
import com.foodfast.entity.VoucherScope;
import com.foodfast.repository.VoucherRedemptionRepository;
import com.foodfast.repository.VoucherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoucherServiceTest {
    @Mock VoucherRepository voucherRepository;
    @Mock VoucherRedemptionRepository redemptionRepository;
    private VoucherService service;
    private Instant now;

    @BeforeEach
    void setUp() {
        service = new VoucherService(voucherRepository, redemptionRepository);
        now = Instant.parse("2026-10-01T04:00:00Z");
    }

    @Test
    void percentVoucherHonoursMaximumDiscount() {
        Voucher voucher = voucher("WELCOME20", VoucherDiscountType.PERCENT, 20);
        voucher.setMaxDiscount(40_000);
        stubAvailable(voucher);

        VoucherService.VoucherApplication result = service.preview(
                "welcome20", "restaurant_2", "u2", 300_000, now
        );

        assertThat(result.discountAmount()).isEqualTo(40_000);
        assertThat(result.code()).isEqualTo("WELCOME20");
    }

    @Test
    void rejectsVoucherForAnotherRestaurant() {
        Voucher voucher = voucher("SWEET10", VoucherDiscountType.PERCENT, 10);
        voucher.setRestaurantId("rest_2");
        when(voucherRepository.findByCodeIgnoreCase("SWEET10")).thenReturn(Optional.of(voucher));

        assertThatThrownBy(() -> service.preview("SWEET10", "restaurant_2", "u2", 100_000, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không áp dụng cho nhà hàng");
    }

    @Test
    void rejectsOrderBelowMinimum() {
        Voucher voucher = voucher("ALOHA30K", VoucherDiscountType.FIXED, 30_000);
        voucher.setMinOrderAmount(150_000);
        when(voucherRepository.findByCodeIgnoreCase("ALOHA30K")).thenReturn(Optional.of(voucher));

        assertThatThrownBy(() -> service.preview("ALOHA30K", "restaurant_2", "u2", 149_000, now))
                .hasMessageContaining("giá trị tối thiểu");
    }

    @Test
    void rejectsExpiredVoucher() {
        Voucher voucher = voucher("OLD", VoucherDiscountType.FIXED, 10_000);
        voucher.setEndsAt(now.minus(1, ChronoUnit.DAYS));
        when(voucherRepository.findByCodeIgnoreCase("OLD")).thenReturn(Optional.of(voucher));

        assertThatThrownBy(() -> service.preview("OLD", "restaurant_2", "u2", 100_000, now))
                .hasMessageContaining("hết hạn");
    }

    @Test
    void noCodeDoesNotReserveVoucher() {
        VoucherService.VoucherApplication result = service.applyAndReserve(
                null, "restaurant_2", "u2", 100_000, "ORDER-1", now
        );
        assertThat(result.discountAmount()).isZero();
        verify(redemptionRepository, never()).save(any());
    }

    @Test
    void listsCurrentRestaurantVoucherAndExplainsMissingMinimum() {
        Voucher aloha = voucher("ALOHA30K", VoucherDiscountType.FIXED, 30_000);
        aloha.setRestaurantId("restaurant_2");
        aloha.setMinOrderAmount(150_000);
        Voucher otherRestaurant = voucher("SWEET10", VoucherDiscountType.PERCENT, 10);
        otherRestaurant.setRestaurantId("rest_2");
        when(voucherRepository.findAllByOrderByStartsAtDesc())
                .thenReturn(List.of(aloha, otherRestaurant));

        var result = service.listAvailable("Aloha", "u2", 100_000, now);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("ALOHA30K");
        assertThat(result.get(0).eligible()).isFalse();
        assertThat(result.get(0).amountNeeded()).isEqualTo(50_000);
    }

    @Test
    void availableVoucherIncludesEstimatedDiscount() {
        Voucher voucher = voucher("WELCOME20", VoucherDiscountType.PERCENT, 20);
        voucher.setScope(VoucherScope.GLOBAL);
        voucher.setMaxDiscount(30_000);
        when(voucherRepository.findAllByOrderByStartsAtDesc()).thenReturn(List.of(voucher));

        var result = service.listAvailable("restaurant_2", "u2", 200_000, now);

        assertThat(result.get(0).eligible()).isTrue();
        assertThat(result.get(0).estimatedDiscount()).isEqualTo(30_000);
    }

    private void stubAvailable(Voucher voucher) {
        when(voucherRepository.findByCodeIgnoreCase(voucher.getCode())).thenReturn(Optional.of(voucher));
        when(redemptionRepository.countByVoucherCodeAndStatusNot(
                voucher.getCode(), VoucherRedemptionStatus.RELEASED)).thenReturn(0L);
        when(redemptionRepository.countByVoucherCodeAndUserIdAndStatusNot(
                voucher.getCode(), "u2", VoucherRedemptionStatus.RELEASED)).thenReturn(0L);
    }

    private Voucher voucher(String code, VoucherDiscountType type, int value) {
        Voucher voucher = new Voucher();
        voucher.setCode(code);
        voucher.setName("Mã kiểm thử");
        voucher.setDiscountType(type);
        voucher.setDiscountValue(value);
        voucher.setMinOrderAmount(0);
        voucher.setMaxDiscount(0);
        voucher.setStartsAt(now.minus(1, ChronoUnit.DAYS));
        voucher.setEndsAt(now.plus(1, ChronoUnit.DAYS));
        voucher.setUsageLimit(100);
        voucher.setPerUserLimit(1);
        voucher.setActive(true);
        return voucher;
    }
}
