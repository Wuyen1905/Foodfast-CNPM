import { createVNPayUrl, getVNPayAvailability, verifyVNPayReturn } from '../vnpay';
import { afterEach, describe, expect, it, vi } from 'vitest';

describe('VNPay Service', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('checks whether VNPay is configured before creating an order', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ enabled: false, message: 'VNPay chưa được cấu hình' }),
    }));

    await expect(getVNPayAvailability()).resolves.toEqual({
      enabled: false,
      message: 'VNPay chưa được cấu hình',
    });
  });

  it('asks the backend to create a signed payment URL using only the persisted order id', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ url: 'https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?signed=1' }),
    });
    vi.stubGlobal('fetch', fetchMock);

    await expect(createVNPayUrl('ORDER-123')).resolves.toContain('sandbox.vnpayment.vn');
    expect(fetchMock).toHaveBeenCalledWith('/api/payment/vnpay/create', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({ orderId: 'ORDER-123' }),
    }));
  });

  it('delegates Return URL verification to the backend', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        verified: true,
        success: true,
        orderId: 'ORDER-123',
        paymentStatus: 'paid',
        transactionId: '999',
        amount: 100000,
        responseCode: '00',
        message: 'Thanh toán VNPay thành công.',
      }),
    });
    vi.stubGlobal('fetch', fetchMock);

    const params = new URLSearchParams({ vnp_TxnRef: 'FF123', vnp_SecureHash: 'signed' });
    const result = await verifyVNPayReturn(params);

    expect(result.success).toBe(true);
    expect(result.paymentStatus).toBe('paid');
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/payment/vnpay/return?vnp_TxnRef=FF123&vnp_SecureHash=signed',
      expect.any(Object),
    );
  });
});
