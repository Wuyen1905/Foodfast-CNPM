export const ORDER_STATUSES = [
  'Pending',
  'Confirmed',
  'In Progress',
  'Ready',
  'Delivering',
  'Delivered',
  'Cancelled',
] as const;

export type OrderStatus = (typeof ORDER_STATUSES)[number];

export const DELIVERY_STATUSES = [
  'ASSIGNED',
  'DELIVERING',
  'DELIVERED',
  'FAILED',
  'CANCELLED',
] as const;

export type DeliveryStatus = (typeof DELIVERY_STATUSES)[number];

export const PAYMENT_STATUSES = [
  'pending',
  'paid',
  'failed',
  'expired',
  'refund_pending',
  'partial_refund_pending',
  'refunded',
  'partial_refunded',
] as const;

export type PaymentStatus = (typeof PAYMENT_STATUSES)[number];

export function parseOrderStatusValue(status?: string): OrderStatus | undefined {
  const normalized = status?.trim().toLowerCase().replace(/[-_]+/g, ' ') || 'pending';
  const aliases: Record<string, OrderStatus> = {
    pending: 'Pending',
    'đang chờ phê duyệt': 'Pending',
    'chờ xác nhận': 'Pending',
    confirmed: 'Confirmed',
    'đã xác nhận': 'Confirmed',
    'nhà hàng đã nhận': 'Confirmed',
    preparing: 'In Progress',
    processing: 'In Progress',
    'in progress': 'In Progress',
    'đang xử lý': 'In Progress',
    'đang chuẩn bị': 'In Progress',
    ready: 'Ready',
    'sẵn sàng': 'Ready',
    'sẵn sàng giao': 'Ready',
    delivering: 'Delivering',
    'en route': 'Delivering',
    'đang giao': 'Delivering',
    'đang giao hàng': 'Delivering',
    delivered: 'Delivered',
    completed: 'Delivered',
    'đã giao': 'Delivered',
    'hoàn tất': 'Delivered',
    cancelled: 'Cancelled',
    canceled: 'Cancelled',
    'đã hủy': 'Cancelled',
  };
  return aliases[normalized];
}

export function normalizeOrderStatusValue(status?: string): OrderStatus {
  return parseOrderStatusValue(status) || 'Pending';
}

export function normalizePaymentStatusValue(status?: string): PaymentStatus {
  const normalized = status?.trim().toLowerCase().replace(/\s+/g, '_') || 'pending';
  if (normalized === 'completed' || normalized === 'success' || normalized === 'successful') {
    return 'paid';
  }
  if (normalized === 'failure' || normalized === 'cancelled' || normalized === 'canceled') {
    return 'failed';
  }
  if (normalized === 'đang_chờ_phê_duyệt') {
    return 'pending';
  }
  return PAYMENT_STATUSES.includes(normalized as PaymentStatus)
    ? normalized as PaymentStatus
    : 'pending';
}
