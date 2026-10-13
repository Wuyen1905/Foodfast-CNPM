import {
  normalizeOrderStatusValue,
  type OrderStatus,
} from '@/types/status';

export type { OrderStatus } from '@/types/status';

export const ORDER_STATUS_OPTIONS: { value: OrderStatus; label: string }[] = [
  { value: 'Pending', label: 'Chờ xác nhận' },
  { value: 'Confirmed', label: 'Đã xác nhận' },
  { value: 'In Progress', label: 'Đang chuẩn bị' },
  { value: 'Ready', label: 'Sẵn sàng giao' },
  { value: 'Delivering', label: 'Nhân viên đang giao' },
  { value: 'Delivered', label: 'Đã giao' },
  { value: 'Cancelled', label: 'Đã hủy' },
];

// Forward progression order used to prevent moving an order backward.
// "cancelled" is excluded here since it's always offered as an exit option.
export const ORDER_STATUS_FLOW: OrderStatus[] = [
  'Pending', 'Confirmed', 'In Progress', 'Ready', 'Delivering', 'Delivered',
];

export function normalizeStatus(x: string): OrderStatus {
  return normalizeOrderStatusValue(x);
}
