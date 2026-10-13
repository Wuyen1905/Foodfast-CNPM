import { parseOrderStatusValue, type OrderStatus } from '@/types/status';

export const ORDER_STATUS_LABELS: Record<OrderStatus, string> = {
  Pending: 'Chờ xác nhận',
  Confirmed: 'Nhà hàng đã nhận',
  'In Progress': 'Đang chuẩn bị',
  Ready: 'Sẵn sàng giao',
  Delivering: 'Đang giao',
  Delivered: 'Đã giao',
  Cancelled: 'Đã hủy',
};

export const ORDER_STATUS_COLORS: Record<OrderStatus, { bg: string; color: string }> = {
  Pending: { bg: '#FFF9C4', color: '#F57F17' },
  Confirmed: { bg: '#BBDEFB', color: '#1565C0' },
  'In Progress': { bg: '#FFE0B2', color: '#E65100' },
  Ready: { bg: '#C8E6C9', color: '#2E7D32' },
  Delivering: { bg: '#DCEDC8', color: '#558B2F' },
  Delivered: { bg: '#A5D6A7', color: '#1B5E20' },
  Cancelled: { bg: '#FFCDD2', color: '#C62828' },
};

export function getStatusLabel(status: string): string {
  const normalized = parseOrderStatusValue(status);
  return normalized ? ORDER_STATUS_LABELS[normalized] : 'Không xác định';
}

export function getStatusColors(status: string): { bg: string; color: string } {
  const normalized = parseOrderStatusValue(status);
  return normalized
    ? ORDER_STATUS_COLORS[normalized]
    : { bg: '#E0E0E0', color: '#616161' };
}
