import apiClient from '../config/axios';
import type { SystemLog } from '../types/admin';

export type RestaurantLifecycleStatus = 'PENDING' | 'ACTIVE' | 'SUSPENDED' | 'CLOSED';
export type SupportPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';
export type SupportDepartment = 'CUSTOMER_SERVICE' | 'OPERATIONS' | 'FINANCE';
export type SupportTicketStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED';

export interface AdminRestaurant {
  id: string;
  name: string;
  category: string;
  status: RestaurantLifecycleStatus;
  statusReason?: string;
  statusUpdatedAt?: number;
  ownerId: string;
  ownerName: string;
  totalOrders: number;
  totalRevenue: number;
  rating: number;
  courierCount: number;
  location: string;
  createdAt: number;
}

export interface AdminCustomer {
  id: string;
  name: string;
  phone: string;
  email: string;
  totalOrders: number;
  totalSpend: number;
  accountStatus: 'Active' | 'Suspended';
  createdAt: number;
  lastOrderDate?: number;
  lastLoginAt?: number;
  suspensionReason?: string;
  accountStatusUpdatedAt?: number;
}

export interface AdminStats {
  totalCustomers: number;
  totalRestaurants: number;
  activeRestaurants: number;
  pendingRestaurants: number;
  totalOrders: number;
  totalRevenue: number;
  totalCouriers: number;
  activeCouriers: number;
  activeDeliveries: number;
  failedDeliveries: number;
}

export interface AdminOverview {
  ordersToday: number;
  revenueToday: number;
  revenueWeek: number;
  revenueMonth: number;
  failedDeliveries: number;
  failedDeliveryOrderIds: string[];
  paymentReconciliationNeeded: number;
  reconciliationOrderIds: string[];
  openComplaints: number;
  pendingRefunds: number;
  pendingRestaurants: number;
  openSupportTickets: number;
  overdueSupportTickets: number;
}

export interface AdminAnalyticsSummary {
  totalOrders: number;
  completedOrders: number;
  cancelledOrders: number;
  netRevenue: number;
  averageOrderValue: number;
  completionRate: number;
  cancellationRate: number;
  onTimeDeliveryRate: number | null;
  averageConfirmationMinutes: number | null;
  averagePreparationMinutes: number | null;
  averageDeliveryMinutes: number | null;
}

export interface AdminAnalyticsReport {
  days: 7 | 30 | 90;
  from: number;
  to: number;
  summary: AdminAnalyticsSummary;
  trend: Array<{ date: string; label: string; orders: number; completed: number; cancelled: number; revenue: number }>;
  statusBreakdown: Record<string, number>;
  paymentBreakdown: Record<string, number>;
  cancellationReasons: Array<{ code: string; count: number }>;
  restaurants: Array<{
    id: string; name: string; orders: number; completed: number; cancelled: number;
    revenue: number; completionRate: number; cancellationRate: number;
  }>;
}

export interface AdminOrderSummary {
  id: string;
  restaurantId?: string;
  restaurant?: string;
  status: string;
  paymentMethod?: string;
  paymentStatus?: string;
  total: number;
  createdAt: number;
}

export interface AdminRestaurantDetails {
  id: string;
  name: string;
  category?: string;
  location?: string;
  status: RestaurantLifecycleStatus;
  statusReason?: string;
  statusUpdatedAt?: number;
  rating?: number;
  totalOrders: number;
  deliveredOrders: number;
  cancelledOrders: number;
  failedDeliveries: number;
  totalRevenue: number;
  cancellationRate: number;
  complaintCount: number;
  openComplaintCount: number;
  recentOrders: AdminOrderSummary[];
}

export interface AdminCustomerDetails extends AdminCustomer {
  username?: string;
  orders: AdminOrderSummary[];
  payments: Array<{ orderId: string; method?: string; status?: string; amount: number; transactionId?: string; createdAt: number }>;
  complaints: Array<{ id: string; orderId: string; category: string; status: string; refundStatus?: string; approvedRefundAmount?: number; createdAt: number }>;
  addresses: Array<{ id: string; label?: string; recipientName: string; phone: string; addressLine: string; defaultAddress: boolean }>;
}

export interface SupportHistoryItem {
  id?: number;
  action: string;
  actorRole?: string;
  actorId?: string;
  note?: string;
  fromStatus?: string;
  toStatus?: string;
  createdAt: number;
}

export interface AdminSupportQueueItem {
  type: 'SUPPORT_TICKET' | 'COMPLAINT';
  id: string;
  orderId?: string;
  restaurantId?: string;
  customerName?: string;
  summary: string;
  status: string;
  priority: SupportPriority;
  department: SupportDepartment;
  assignedId?: string;
  assignedName?: string;
  slaDueAt?: number;
  overdue: boolean;
  refundStatus?: string;
  approvedRefundAmount?: number;
  createdAt: number;
  history: SupportHistoryItem[];
}

const normalizeRestaurantStatus = (value: unknown): RestaurantLifecycleStatus => {
  const status = String(value || '').toUpperCase();
  if (status === 'ACTIVE' || status === 'PENDING' || status === 'SUSPENDED' || status === 'CLOSED') return status;
  if (status === 'INACTIVE') return 'SUSPENDED';
  return 'PENDING';
};

export const getAllRestaurants = async (): Promise<AdminRestaurant[]> => {
  const { data } = await apiClient.get('/admin/restaurants');
  const restaurants = Array.isArray(data) ? data : [];
  return restaurants.map((r: any) => ({
    id: r.id, name: r.name, category: r.category || 'Chưa phân loại',
    status: normalizeRestaurantStatus(r.status), statusReason: r.statusReason || undefined,
    statusUpdatedAt: r.statusUpdatedAt || undefined, ownerId: r.ownerId,
    ownerName: r.ownerName || 'Không xác định', totalOrders: r.totalOrders || 0,
    totalRevenue: r.totalRevenue || 0, rating: r.rating || 0,
    courierCount: r.courierCount || 0, location: r.location || 'Chưa cập nhật',
    createdAt: r.createdAt || Date.now()
  }));
};

export const getAllCustomers = async (): Promise<AdminCustomer[]> => {
  const { data } = await apiClient.get('/admin/customers');
  const customers = Array.isArray(data) ? data : [];
  return customers.map((c: any) => ({
    id: c.id, name: c.name, phone: c.phone || '', email: c.email || '',
    totalOrders: c.totalOrders || 0, totalSpend: c.totalSpend || 0,
    accountStatus: c.accountStatus || 'Active', createdAt: c.createdAt || Date.now(),
    lastOrderDate: c.lastOrderDate || undefined, lastLoginAt: c.lastLoginAt || undefined,
    suspensionReason: c.suspensionReason || undefined,
    accountStatusUpdatedAt: c.accountStatusUpdatedAt || undefined
  }));
};

export const getAdminStats = async (): Promise<AdminStats> => {
  const { data } = await apiClient.get('/admin/stats');
  return {
    totalCustomers: data.totalCustomers || 0, totalRestaurants: data.totalRestaurants || 0,
    activeRestaurants: data.activeRestaurants || 0, pendingRestaurants: data.pendingRestaurants || 0,
    totalOrders: data.totalOrders || 0, totalRevenue: data.totalRevenue || 0,
    totalCouriers: data.totalCouriers || 0, activeCouriers: data.activeCouriers || 0,
    activeDeliveries: data.activeDeliveries || 0, failedDeliveries: data.failedDeliveries || 0
  };
};

export const getAdminOverview = async (): Promise<AdminOverview> => {
  const { data } = await apiClient.get('/admin/overview');
  return {
    ordersToday: data.ordersToday || 0, revenueToday: data.revenueToday || 0,
    revenueWeek: data.revenueWeek || 0, revenueMonth: data.revenueMonth || 0,
    failedDeliveries: data.failedDeliveries || 0,
    failedDeliveryOrderIds: data.failedDeliveryOrderIds || [],
    paymentReconciliationNeeded: data.paymentReconciliationNeeded || 0,
    reconciliationOrderIds: data.reconciliationOrderIds || [],
    openComplaints: data.openComplaints || 0, pendingRefunds: data.pendingRefunds || 0,
    pendingRestaurants: data.pendingRestaurants || 0, openSupportTickets: data.openSupportTickets || 0,
    overdueSupportTickets: data.overdueSupportTickets || 0
  };
};

export const getAdminAnalytics = async (days: 7 | 30 | 90): Promise<AdminAnalyticsReport> => {
  const { data } = await apiClient.get('/admin/analytics', { params: { days } });
  return data;
};

export const updateRestaurantLifecycle = async (id: string, status: RestaurantLifecycleStatus, reason: string): Promise<void> => {
  await apiClient.patch(`/admin/restaurants/${id}/lifecycle`, { status, reason });
};

export const getRestaurantDetails = async (id: string): Promise<AdminRestaurantDetails> => {
  const { data } = await apiClient.get(`/admin/restaurants/${id}/details`);
  return data;
};

export const getCustomerDetails = async (id: string): Promise<AdminCustomerDetails> => {
  const { data } = await apiClient.get(`/admin/customers/${id}/details`);
  return data;
};

export const getAdminSupportQueue = async (): Promise<AdminSupportQueueItem[]> => {
  const { data } = await apiClient.get('/admin/support-queue');
  return Array.isArray(data) ? data : [];
};

export const updateAdminSupportTicket = async (
  id: string,
  request: { status?: SupportTicketStatus; priority?: SupportPriority; department?: SupportDepartment; assignToMe?: boolean; note?: string }
): Promise<void> => {
  await apiClient.patch(`/admin/support-tickets/${id}`, request);
};

export const updateRestaurantStatus = async (id: string, status: 'active' | 'inactive', reason: string): Promise<boolean> => {
  try { await updateRestaurantLifecycle(id, status === 'active' ? 'ACTIVE' : 'SUSPENDED', reason); return true; }
  catch { return false; }
};

export const suspendCustomer = async (customerId: string, reason: string): Promise<boolean> => {
  try { await apiClient.patch(`/admin/users/${customerId}/suspend`, { reason }); return true; }
  catch { return false; }
};

export const reactivateCustomer = async (customerId: string, reason: string): Promise<boolean> => {
  try { await apiClient.patch(`/admin/users/${customerId}/reactivate`, { reason }); return true; }
  catch { return false; }
};

export const getAuditLogs = async (limit = 100): Promise<SystemLog[]> => {
  const response = await apiClient.get('/admin/audit-logs', { params: { limit } });
  return Array.isArray(response.data) ? response.data.map((log: any) => ({ ...log, id: String(log.id) })) : [];
};
