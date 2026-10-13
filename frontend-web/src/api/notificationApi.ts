import apiClient from '@/config/axios';

export interface CustomerNotification {
  id: string;
  userId: string;
  orderId?: string;
  type: string;
  title: string;
  message: string;
  actionUrl?: string;
  read: boolean;
  createdAt: number;
}

export interface CustomerNotificationPage {
  notifications: CustomerNotification[];
  unreadCount: number;
  total: number;
}

export async function getCustomerNotifications(): Promise<CustomerNotificationPage> {
  const response = await apiClient.get('/customer-notifications');
  return response.data;
}

export async function markCustomerNotificationRead(id: string): Promise<CustomerNotification> {
  const response = await apiClient.patch(`/customer-notifications/${id}/read`);
  return response.data;
}

export async function markAllCustomerNotificationsRead(): Promise<number> {
  const response = await apiClient.patch('/customer-notifications/read-all');
  return response.data.updated;
}
