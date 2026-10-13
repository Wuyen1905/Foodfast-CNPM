package com.foodfast.service;

/**
 * @deprecated Use {@link VNPayPaymentService}. Kept temporarily so older
 * references fail clearly during migration instead of using the insecure
 * client-amount flow.
 */
@Deprecated(forRemoval = true)
public final class VNPayService {
    private VNPayService() {
    }
}
