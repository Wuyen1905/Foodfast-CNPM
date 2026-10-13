import { describe, expect, it } from 'vitest';
import { getStatusLabel } from '@/utils/orderStatus';

describe('hiển thị trạng thái đơn hàng bằng tiếng Việt', () => {
  it('chuyển mọi biến thể trạng thái từ API sang nhãn tiếng Việt', () => {
    const cases: Record<string, string> = {
      Pending: 'Chờ xác nhận',
      CONFIRMED: 'Nhà hàng đã nhận',
      'In Progress': 'Đang chuẩn bị',
      ready: 'Sẵn sàng giao',
      Delivering: 'Đang giao',
      DELIVERED: 'Đã giao',
      cancelled: 'Đã hủy',
    };

    Object.entries(cases).forEach(([status, expected]) => {
      expect(getStatusLabel(status)).toBe(expected);
    });
  });

  it('không để trạng thái kỹ thuật lạ rò ra giao diện', () => {
    expect(getStatusLabel('UNKNOWN_INTERNAL_STATUS')).toBe('Không xác định');
  });
});
