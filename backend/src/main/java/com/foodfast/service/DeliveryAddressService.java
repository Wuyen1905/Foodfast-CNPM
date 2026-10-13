package com.foodfast.service;

import com.foodfast.dto.DeliveryAddressRequest;
import com.foodfast.entity.DeliveryAddress;
import com.foodfast.repository.DeliveryAddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
public class DeliveryAddressService {

    private final DeliveryAddressRepository repository;

    public DeliveryAddressService(DeliveryAddressRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<DeliveryAddress> list(String userId) {
        return repository.findByUserIdOrderByDefaultAddressDescUpdatedAtDesc(requireUserId(userId));
    }

    @Transactional
    public DeliveryAddress create(String userId, DeliveryAddressRequest request) {
        String ownerId = requireUserId(userId);
        List<DeliveryAddress> existing = repository.findByUserIdOrderByDefaultAddressDescUpdatedAtDesc(ownerId);
        boolean makeDefault = request.defaultAddress() || existing.isEmpty();
        if (makeDefault) clearDefault(existing);

        long now = System.currentTimeMillis();
        DeliveryAddress address = new DeliveryAddress();
        address.setId("ADDR-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        address.setUserId(ownerId);
        apply(address, request);
        address.setDefaultAddress(makeDefault);
        address.setCreatedAt(now);
        address.setUpdatedAt(now);
        return repository.save(address);
    }

    @Transactional
    public DeliveryAddress update(String userId, String addressId, DeliveryAddressRequest request) {
        String ownerId = requireUserId(userId);
        DeliveryAddress address = requireOwned(ownerId, addressId);
        if (request.defaultAddress()) {
            clearDefault(repository.findByUserIdOrderByDefaultAddressDescUpdatedAtDesc(ownerId));
        }
        apply(address, request);
        address.setDefaultAddress(request.defaultAddress() || address.isDefaultAddress());
        address.setUpdatedAt(System.currentTimeMillis());
        return repository.save(address);
    }

    @Transactional
    public DeliveryAddress makeDefault(String userId, String addressId) {
        String ownerId = requireUserId(userId);
        DeliveryAddress selected = requireOwned(ownerId, addressId);
        clearDefault(repository.findByUserIdOrderByDefaultAddressDescUpdatedAtDesc(ownerId));
        selected.setDefaultAddress(true);
        selected.setUpdatedAt(System.currentTimeMillis());
        return repository.save(selected);
    }

    @Transactional
    public void delete(String userId, String addressId) {
        String ownerId = requireUserId(userId);
        DeliveryAddress address = requireOwned(ownerId, addressId);
        boolean wasDefault = address.isDefaultAddress();
        repository.delete(address);
        repository.flush();
        if (wasDefault) {
            List<DeliveryAddress> remaining = repository.findByUserIdOrderByDefaultAddressDescUpdatedAtDesc(ownerId);
            if (!remaining.isEmpty()) {
                DeliveryAddress replacement = remaining.get(0);
                replacement.setDefaultAddress(true);
                replacement.setUpdatedAt(System.currentTimeMillis());
                repository.save(replacement);
            }
        }
    }

    private void clearDefault(List<DeliveryAddress> addresses) {
        boolean changed = false;
        for (DeliveryAddress address : addresses) {
            if (address.isDefaultAddress()) {
                address.setDefaultAddress(false);
                changed = true;
            }
        }
        if (changed) repository.saveAll(addresses);
    }

    private DeliveryAddress requireOwned(String userId, String addressId) {
        return repository.findByIdAndUserId(requireText(addressId, "Mã địa chỉ không hợp lệ"), userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy địa chỉ trong sổ địa chỉ của bạn"));
    }

    private void apply(DeliveryAddress address, DeliveryAddressRequest request) {
        address.setLabel(request.label().trim());
        address.setRecipientName(request.recipientName().trim());
        address.setPhone(request.phone().trim());
        address.setAddressLine(request.addressLine().trim());
    }

    private String requireUserId(String userId) {
        return requireText(userId, "Không xác định được khách hàng");
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) throw new IllegalArgumentException(message);
        return value.trim();
    }
}
