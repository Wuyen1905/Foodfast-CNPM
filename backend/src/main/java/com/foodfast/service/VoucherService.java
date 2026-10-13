package com.foodfast.service;

import com.foodfast.dto.AvailableVoucherResponse;
import com.foodfast.entity.Voucher;
import com.foodfast.entity.VoucherDiscountType;
import com.foodfast.entity.VoucherRedemption;
import com.foodfast.entity.VoucherRedemptionStatus;
import com.foodfast.repository.VoucherRedemptionRepository;
import com.foodfast.repository.VoucherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class VoucherService {
    private final VoucherRepository voucherRepository;
    private final VoucherRedemptionRepository redemptionRepository;

    public VoucherService(VoucherRepository voucherRepository, VoucherRedemptionRepository redemptionRepository) {
        this.voucherRepository = voucherRepository;
        this.redemptionRepository = redemptionRepository;
    }

    @Transactional(readOnly = true)
    public VoucherApplication preview(String code, String restaurantId, String userId, int subtotal, Instant now) {
        return validate(code, restaurantId, userId, subtotal, now, false);
    }

    @Transactional(readOnly = true)
    public List<AvailableVoucherResponse> listAvailable(
            String restaurantId,
            String userId,
            int subtotal,
            Instant now
    ) {
        if (!StringUtils.hasText(restaurantId)) {
            throw new IllegalArgumentException("Không xác định được nhà hàng của giỏ hàng");
        }
        String normalizedUserId = requireUser(userId);
        int safeSubtotal = Math.max(0, subtotal);
        Instant current = now == null ? Instant.now() : now;

        return voucherRepository.findAllByOrderByStartsAtDesc().stream()
                .filter(Voucher::isActive)
                .filter(voucher -> !current.isBefore(voucher.getStartsAt())
                        && !current.isAfter(voucher.getEndsAt()))
                .filter(voucher -> !StringUtils.hasText(voucher.getRestaurantId())
                        || RestaurantIdentityPolicy.refersToSameRestaurant(
                                voucher.getRestaurantId(), restaurantId
                        ))
                .map(voucher -> availableResponse(voucher, normalizedUserId, safeSubtotal))
                .filter(response -> response != null)
                .sorted(Comparator
                        .comparing(AvailableVoucherResponse::eligible).reversed()
                        .thenComparing(AvailableVoucherResponse::amountNeeded)
                        .thenComparing(AvailableVoucherResponse::endsAt))
                .toList();
    }

    @Transactional
    public VoucherApplication applyAndReserve(
            String code,
            String restaurantId,
            String userId,
            int subtotal,
            String orderId,
            Instant now
    ) {
        if (!StringUtils.hasText(code)) return VoucherApplication.none();
        VoucherApplication application = validate(code, restaurantId, userId, subtotal, now, true);
        VoucherRedemption redemption = new VoucherRedemption();
        redemption.setVoucherCode(application.code());
        redemption.setOrderId(orderId);
        redemption.setUserId(requireUser(userId));
        redemptionRepository.save(redemption);
        return application;
    }

    @Transactional
    public void releaseForOrder(String orderId) {
        redemptionRepository.findByOrderId(orderId).ifPresent(redemption -> {
            redemption.setStatus(VoucherRedemptionStatus.RELEASED);
            redemptionRepository.save(redemption);
        });
    }

    private VoucherApplication validate(
            String code,
            String restaurantId,
            String userId,
            int subtotal,
            Instant now,
            boolean lockForReservation
    ) {
        if (!StringUtils.hasText(code)) throw new IllegalArgumentException("Vui lòng nhập mã ưu đãi");
        if (subtotal <= 0) throw new IllegalArgumentException("Giỏ hàng không hợp lệ để áp dụng ưu đãi");
        String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
        Voucher voucher = (lockForReservation
                ? voucherRepository.findByCodeForUpdate(normalizedCode)
                : voucherRepository.findByCodeIgnoreCase(normalizedCode))
                .orElseThrow(() -> new IllegalArgumentException("Mã ưu đãi không tồn tại"));
        Instant current = now == null ? Instant.now() : now;
        if (!voucher.isActive()) throw new IllegalArgumentException("Mã ưu đãi đã ngừng hoạt động");
        if (current.isBefore(voucher.getStartsAt())) throw new IllegalArgumentException("Mã ưu đãi chưa bắt đầu");
        if (current.isAfter(voucher.getEndsAt())) throw new IllegalArgumentException("Mã ưu đãi đã hết hạn");
        if (StringUtils.hasText(voucher.getRestaurantId())
                && !RestaurantIdentityPolicy.refersToSameRestaurant(voucher.getRestaurantId(), restaurantId)) {
            throw new IllegalArgumentException("Mã ưu đãi không áp dụng cho nhà hàng này");
        }
        if (subtotal < voucher.getMinOrderAmount()) {
            throw new IllegalArgumentException("Đơn hàng chưa đạt giá trị tối thiểu " + voucher.getMinOrderAmount() + " đ");
        }
        long totalUsage = redemptionRepository.countByVoucherCodeAndStatusNot(
                voucher.getCode(), VoucherRedemptionStatus.RELEASED
        );
        if (voucher.getUsageLimit() > 0 && totalUsage >= voucher.getUsageLimit()) {
            throw new IllegalArgumentException("Mã ưu đãi đã hết lượt sử dụng");
        }
        String normalizedUserId = requireUser(userId);
        long userUsage = redemptionRepository.countByVoucherCodeAndUserIdAndStatusNot(
                voucher.getCode(), normalizedUserId, VoucherRedemptionStatus.RELEASED
        );
        if (userUsage >= voucher.getPerUserLimit()) {
            throw new IllegalArgumentException("Bạn đã dùng hết lượt của mã ưu đãi này");
        }
        int discount = calculateDiscount(voucher, subtotal);
        return new VoucherApplication(voucher.getCode(), voucher.getName(), discount);
    }

    private String requireUser(String userId) {
        if (!StringUtils.hasText(userId)) throw new IllegalArgumentException("Vui lòng đăng nhập để dùng mã ưu đãi");
        return userId.trim();
    }

    private AvailableVoucherResponse availableResponse(
            Voucher voucher,
            String userId,
            int subtotal
    ) {
        long totalUsage = redemptionRepository.countByVoucherCodeAndStatusNot(
                voucher.getCode(), VoucherRedemptionStatus.RELEASED
        );
        if (voucher.getUsageLimit() > 0 && totalUsage >= voucher.getUsageLimit()) return null;

        long userUsage = redemptionRepository.countByVoucherCodeAndUserIdAndStatusNot(
                voucher.getCode(), userId, VoucherRedemptionStatus.RELEASED
        );
        if (userUsage >= voucher.getPerUserLimit()) return null;

        int amountNeeded = Math.max(0, voucher.getMinOrderAmount() - subtotal);
        boolean eligible = amountNeeded == 0 && subtotal > 0;
        int estimatedDiscount = eligible ? calculateDiscount(voucher, subtotal) : 0;
        String message = eligible
                ? "Có thể áp dụng cho đơn hàng này"
                : "Mua thêm " + amountNeeded + " đ để sử dụng";
        return new AvailableVoucherResponse(
                voucher.getCode(), voucher.getName(), voucher.getDiscountType(),
                voucher.getDiscountValue(), voucher.getMinOrderAmount(), voucher.getMaxDiscount(),
                voucher.getEndsAt(), eligible, amountNeeded, estimatedDiscount, message
        );
    }

    private int calculateDiscount(Voucher voucher, int subtotal) {
        int discount = voucher.getDiscountType() == VoucherDiscountType.PERCENT
                ? (int) Math.floor(subtotal * (voucher.getDiscountValue() / 100.0))
                : voucher.getDiscountValue();
        if (voucher.getMaxDiscount() > 0) discount = Math.min(discount, voucher.getMaxDiscount());
        return Math.min(Math.max(0, discount), subtotal);
    }

    public record VoucherApplication(String code, String name, int discountAmount) {
        public static VoucherApplication none() { return new VoucherApplication(null, null, 0); }
    }
}
