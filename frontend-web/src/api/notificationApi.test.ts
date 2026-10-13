import { beforeEach, describe, expect, it, vi } from 'vitest';

import apiClient from '@/config/axios';
import {
  getCustomerNotifications,
  markAllCustomerNotificationsRead,
  markCustomerNotificationRead,
} from './notificationApi';

vi.mock('@/config/axios', () => ({
  default: {
    get: vi.fn(),
    patch: vi.fn(),
  },
}));

describe('customer notification API', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads the signed-in customer notification center', async () => {
    const payload = { notifications: [], unreadCount: 0, total: 0 };
    vi.mocked(apiClient.get).mockResolvedValue({ data: payload });

    await expect(getCustomerNotifications()).resolves.toEqual(payload);
    expect(apiClient.get).toHaveBeenCalledWith('/customer-notifications');
  });

  it('marks only the selected notification as read', async () => {
    const notification = {
      id: 'notification-1',
      userId: 'customer-1',
      type: 'ORDER_READY',
      title: 'Đơn đã sẵn sàng',
      message: 'Đơn đang chờ phân công người giao.',
      read: true,
      createdAt: 1,
    };
    vi.mocked(apiClient.patch).mockResolvedValue({ data: notification });

    await expect(markCustomerNotificationRead(notification.id)).resolves.toEqual(notification);
    expect(apiClient.patch).toHaveBeenCalledWith('/customer-notifications/notification-1/read');
  });

  it('returns the number of notifications marked read', async () => {
    vi.mocked(apiClient.patch).mockResolvedValue({ data: { updated: 3 } });

    await expect(markAllCustomerNotificationsRead()).resolves.toBe(3);
    expect(apiClient.patch).toHaveBeenCalledWith('/customer-notifications/read-all');
  });
});
