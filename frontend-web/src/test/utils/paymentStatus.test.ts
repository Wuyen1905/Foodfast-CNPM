import { describe, expect, it } from 'vitest';
import { getPaymentStatusInfo, normalizePaymentStatus } from '../../utils/paymentStatus';

describe('paymentStatus', () => {
  it('chuẩn hóa trạng thái thành công từ dữ liệu cũ', () => {
    expect(normalizePaymentStatus('completed')).toBe('paid');
    expect(normalizePaymentStatus('PAID')).toBe('paid');
  });

  it('hiển thị COD là thanh toán khi nhận hàng', () => {
    expect(getPaymentStatusInfo('cod', 'pending').label).toBe('Thanh toán khi nhận hàng');
    expect(getPaymentStatusInfo('cash', 'pending').label).toBe('Thanh toán khi nhận hàng');
  });

  it('phân biệt VNPay đang chờ, thành công và thất bại', () => {
    expect(getPaymentStatusInfo('vnpay', 'pending').label).toBe('Chờ thanh toán');
    expect(getPaymentStatusInfo('vnpay', 'paid').label).toBe('Đã thanh toán');
    expect(getPaymentStatusInfo('vnpay', 'failed').label).toBe('Thanh toán thất bại');
  });
});
