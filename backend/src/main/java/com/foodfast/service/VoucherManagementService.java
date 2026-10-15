package com.foodfast.service;

import com.foodfast.dto.VoucherManagementResponse;
import com.foodfast.dto.VoucherUpsertRequest;
import com.foodfast.entity.Voucher;
import com.foodfast.entity.VoucherDiscountType;
import com.foodfast.entity.VoucherRedemptionStatus;
import com.foodfast.entity.VoucherScope;
import com.foodfast.exception.ForbiddenOperationException;
import com.foodfast.repository.RestaurantRepository;
import com.foodfast.repository.VoucherRedemptionRepository;
import com.foodfast.repository.VoucherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class VoucherManagementService {

    private final VoucherRepository voucherRepository;
    private final VoucherRedemptionRepository redemptionRepository;
    private final RestaurantRepository restaurantRepository;

    public VoucherManagementService(
            VoucherRepository voucherRepository,
            VoucherRedemptionRepository redemptionRepository,
            RestaurantRepository restaurantRepository
    ) {
        this.voucherRepository = voucherRepository;
        this.redemptionRepository = redemptionRepository;
        this.restaurantRepository = restaurantRepository;
    }

    @Transactional(readOnly = true)
    public List<VoucherManagementResponse> list(String restaurantId, boolean admin) {
        List<Voucher> vouchers;
        if (admin && !StringUtils.hasText(restaurantId)) {
            vouchers = voucherRepository.findAllByOrderByStartsAtDesc();
        } else {
            String target = requireRestaurant(restaurantId);
            vouchers = voucherRepository.findByRestaurantIdOrderByStartsAtDesc(target);
        }
        return vouchers.stream().map(this::toResponse).toList();
    }

    @Transactional
    public VoucherManagementResponse create(
            VoucherUpsertRequest request,
            String authenticatedRestaurantId,
            boolean admin
    ) {
        String code = normalizeCode(request == null ? null : request.code());
        if (voucherRepository.findByCodeIgnoreCase(code).isPresent()) {
            throw new IllegalArgumentException("Mã ưu đãi đã tồn tại");
        }
        Voucher voucher = new Voucher();
        voucher.setCode(code);
        apply(voucher, request, authenticatedRestaurantId, admin);
        return toResponse(voucherRepository.save(voucher));
    }

    @Transactional
    public VoucherManagementResponse update(
            String code,
            VoucherUpsertRequest request,
            String authenticatedRestaurantId,
            boolean admin
    ) {
        Voucher voucher = requireVoucher(code);
        requireOwner(voucher, authenticatedRestaurantId, admin);
        apply(voucher, request, authenticatedRestaurantId, admin);
        return toResponse(voucherRepository.save(voucher));
    }

    @Transactional
    public VoucherManagementResponse setActive(
            String code,
            boolean active,
            String authenticatedRestaurantId,
            boolean admin
    ) {
        Voucher voucher = requireVoucher(code);
        requireOwner(voucher, authenticatedRestaurantId, admin);
        voucher.setActive(active);
        return toResponse(voucherRepository.save(voucher));
    }

    private void apply(
            Voucher voucher,
            VoucherUpsertRequest request,
            String authenticatedRestaurantId,
            boolean admin
    ) {
        if (request == null) throw new IllegalArgumentException("Thiếu thông tin mã ưu đãi");
        String name = requireText(request.name(), "Tên chương trình ưu đãi");
        VoucherDiscountType type = request.discountType();
        if (type == null) throw new IllegalArgumentException("Vui lòng chọn loại giảm giá");
        int value = nonNegative(request.discountValue(), "Giá trị giảm");
        if (value <= 0) throw new IllegalArgumentException("Giá trị giảm phải lớn hơn 0");
        if (type == VoucherDiscountType.PERCENT && value > 100) {
            throw new IllegalArgumentException("Phần trăm giảm không được vượt quá 100%");
        }
        int minimum = nonNegative(request.minOrderAmount(), "Giá trị đơn tối thiểu");
        int maximumDiscount = nonNegative(request.maxDiscount(), "Mức giảm tối đa");
        int usageLimit = nonNegative(request.usageLimit(), "Tổng lượt sử dụng");
        int perUserLimit = request.perUserLimit() == null ? 1 : request.perUserLimit();
        if (perUserLimit <= 0) throw new IllegalArgumentException("Lượt dùng mỗi khách phải lớn hơn 0");
        if (usageLimit > 0 && perUserLimit > usageLimit) {
            throw new IllegalArgumentException("Lượt dùng mỗi khách không được vượt tổng lượt sử dụng");
        }
        Instant startsAt = request.startsAt();
        Instant endsAt = request.endsAt();
        if (startsAt == null || endsAt == null || !endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu");
        }

        VoucherScope scope = request.scope();
        if (scope == null) {
            scope = StringUtils.hasText(request.restaurantId())
                    ? VoucherScope.RESTAURANT : VoucherScope.GLOBAL;
        }
        if (!admin && scope == VoucherScope.GLOBAL) {
            throw new ForbiddenOperationException("Chỉ quản trị viên được tạo mã áp dụng toàn hệ thống");
        }

        String targetRestaurant;
        if (admin) {
            targetRestaurant = scope == VoucherScope.RESTAURANT
                    ? requireRestaurant(request.restaurantId()) : null;
        } else {
            targetRestaurant = requireRestaurant(authenticatedRestaurantId);
            scope = VoucherScope.RESTAURANT;
        }

        voucher.setName(name);
        voucher.setRestaurantId(targetRestaurant);
        voucher.setScope(scope);
        voucher.setDiscountType(type);
        voucher.setDiscountValue(value);
        voucher.setMinOrderAmount(minimum);
        voucher.setMaxDiscount(type == VoucherDiscountType.PERCENT ? maximumDiscount : 0);
        voucher.setStartsAt(startsAt);
        voucher.setEndsAt(endsAt);
        voucher.setUsageLimit(usageLimit);
        voucher.setPerUserLimit(perUserLimit);
        voucher.setActive(request.active() == null || request.active());
    }

    private Voucher requireVoucher(String code) {
        return voucherRepository.findByCodeIgnoreCase(normalizeCode(code))
                .orElseThrow(() -> new IllegalArgumentException("Mã ưu đãi không tồn tại"));
    }

    private void requireOwner(Voucher voucher, String authenticatedRestaurantId, boolean admin) {
        if (admin) return;
        if (!StringUtils.hasText(voucher.getRestaurantId())
                || !RestaurantIdentityPolicy.refersToSameRestaurant(
                        voucher.getRestaurantId(), authenticatedRestaurantId)) {
            throw new ForbiddenOperationException("Nhà hàng không có quyền sửa mã ưu đãi này");
        }
    }

    private String requireRestaurant(String restaurantId) {
        String canonical = RestaurantIdentityPolicy.canonicalize(restaurantId);
        if (!StringUtils.hasText(canonical) || !restaurantRepository.existsById(canonical)) {
            throw new IllegalArgumentException("Nhà hàng không tồn tại");
        }
        return canonical;
    }

    private VoucherManagementResponse toResponse(Voucher voucher) {
        long used = redemptionRepository.countByVoucherCodeAndStatusNot(
                voucher.getCode(), VoucherRedemptionStatus.RELEASED
        );
        return VoucherManagementResponse.from(voucher, used);
    }

    private String normalizeCode(String code) {
        String normalized = requireText(code, "Mã ưu đãi").toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z0-9_-]{3,40}")) {
            throw new IllegalArgumentException("Mã ưu đãi chỉ gồm 3-40 chữ in hoa, số, dấu gạch ngang hoặc gạch dưới");
        }
        return normalized;
    }

    private String requireText(String value, String field) {
        if (!StringUtils.hasText(value)) throw new IllegalArgumentException(field + " không được để trống");
        return value.trim();
    }

    private int nonNegative(Integer value, String field) {
        if (value == null || value < 0) throw new IllegalArgumentException(field + " không hợp lệ");
        return value;
    }
}
