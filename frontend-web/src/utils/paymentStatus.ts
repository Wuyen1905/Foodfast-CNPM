import { normalizePaymentStatusValue } from '@/types/status';

export type PaymentStatusInfo = {
  label: string;
  bg: string;
  color: string;
};

export const normalizePaymentStatus = (status?: string): 'pending' | 'paid' | 'failed' => {
  const normalized = normalizePaymentStatusValue(status);
  if (normalized === 'paid' || normalized === 'refunded' || normalized === 'partial_refunded') return 'paid';
  if (normalized === 'failed' || normalized === 'expired') return 'failed';
  return 'pending';
};

export const getPaymentStatusInfo = (
  paymentMethod?: string,
  paymentStatus?: string,
): PaymentStatusInfo => {
  const rawMethod = paymentMethod?.trim().toLowerCase() || 'cod';
  const method = rawMethod === 'cash' ? 'cod' : rawMethod;
  const status = normalizePaymentStatus(paymentStatus);

  const detailedStatus = normalizePaymentStatusValue(paymentStatus);

  if (detailedStatus === 'refund_pending') {
    return { label: 'Đang chờ hoàn tiền', bg: '#fef0c7', color: '#93370d' };
  }
  if (detailedStatus === 'partial_refund_pending') {
    return { label: 'Đang chờ hoàn phần chênh lệch', bg: '#fef0c7', color: '#93370d' };
  }
  if (detailedStatus === 'refunded') {
    return { label: 'Đã hoàn tiền toàn bộ', bg: '#e0f2fe', color: '#075985' };
  }
  if (detailedStatus === 'partial_refunded') {
    return { label: 'Đã hoàn tiền một phần', bg: '#e0f2fe', color: '#075985' };
  }
  if (detailedStatus === 'expired') {
    return { label: 'Phiên thanh toán đã hết hạn', bg: '#ffdddd', color: '#b42318' };
  }

  if (status === 'paid') {
    return { label: 'Đã thanh toán', bg: '#d7f5de', color: '#176b2c' };
  }

  if (status === 'failed') {
    return { label: 'Thanh toán thất bại', bg: '#ffdddd', color: '#b42318' };
  }

  if (method === 'cod') {
    return { label: 'Thanh toán khi nhận hàng', bg: '#e9edf3', color: '#4b5563' };
  }

  return { label: 'Chờ thanh toán', bg: '#fff0cc', color: '#8a5200' };
};
