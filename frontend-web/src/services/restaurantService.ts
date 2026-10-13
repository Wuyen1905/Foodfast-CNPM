// Restaurant Service - Individual restaurant data management
import { getRestaurantOverview as getAnalyticsOverview, getRestaurantAnalytics as getAnalytics } from '@/api/analyticsApi';
import { getOrdersByRestaurant } from '@/api/orderApi';
import apiClient from '@/config/axios';

export interface Order {
  id: string;
  status: 'Đang chuẩn bị' | 'Đang giao hàng' | 'Hoàn thành' | 'Hủy';
  total: number;
  customerName: string;
  customerPhone: string;
  items: OrderItem[];
  orderTime: string;
  estimatedDelivery?: string;
}

export interface OrderItem {
  name: string;
  quantity: number;
  price: number;
}

export interface RestaurantOverview {
  id: string;
  name: string;
  revenue: number;
  ordersToday: number;
  activeDeliveries: number;
  avgDeliveryTime: number;
  rating: number;
  topItems: Array<{
    name: string;
    orders: number;
    revenue: number;
  }>;
}

// Helper function to normalize restaurant ID
const normalizeRestaurantId = (id: string): string => {
  const map: Record<string, string> = {
    'sweetdreams': 'rest_2',
    'rest_2': 'rest_2',
    'aloha': 'restaurant_2',
    'restaurant_2': 'restaurant_2'
  };
  return map[id?.toLowerCase()] || id;
};
// Map backend order status to frontend status
const mapOrderStatus = (status: string): Order['status'] => {
  const statusMap: Record<string, Order['status']> = {
    'Pending': 'Đang chuẩn bị',
    'Confirmed': 'Đang chuẩn bị',
    'In Progress': 'Đang chuẩn bị',
    'Ready': 'Đang chuẩn bị',
    'Delivering': 'Đang giao hàng',
    'Delivered': 'Hoàn thành',
    'Cancelled': 'Hủy'
  };
  return statusMap[status] || 'Đang chuẩn bị';
};

/**
 * Get restaurant overview data
 */
export const getRestaurantOverview = async (id: string): Promise<RestaurantOverview | null> => {
  const normalizedId = normalizeRestaurantId(id);
  const overview = await getAnalyticsOverview(normalizedId);
  
  if (!overview) return null;
  
  return {
    id: overview.id,
    name: overview.name,
    revenue: overview.revenue,
    ordersToday: overview.ordersToday,
    activeDeliveries: overview.activeDeliveries,
    avgDeliveryTime: overview.avgDeliveryTime,
    rating: overview.rating,
    topItems: overview.topItems
  };
};

/**
 * Get restaurant orders
 */
export const getRestaurantOrders = async (id: string): Promise<Order[]> => {
  const normalizedId = normalizeRestaurantId(id);
  const orders = await getOrdersByRestaurant(normalizedId);
  
  return orders.map((o: any) => {
    const orderDate = new Date(o.createdAt);
    const hours = String(orderDate.getHours()).padStart(2, '0');
    const minutes = String(orderDate.getMinutes()).padStart(2, '0');
    
    return {
      id: o.id,
      status: mapOrderStatus(o.status),
      total: o.total,
      customerName: o.name || o.customerName,
      customerPhone: o.phone || o.customerPhone,
      items: o.items?.map((item: any) => ({
        name: item.name,
        quantity: item.qty || item.quantity,
        price: item.price
      })) || [],
      orderTime: `${hours}:${minutes}`,
      estimatedDelivery: undefined
    };
  });
};

/**
 * Update order status
 */
export const updateOrderStatus = async (id: string, orderId: string, status: Order['status']): Promise<boolean> => {
  try {
    // Map frontend status to backend status
    const backendStatus = status === 'Đang chuẩn bị' ? 'Confirmed' :
                         status === 'Đang giao hàng' ? 'Delivering' :
                         status === 'Hoàn thành' ? 'Delivered' :
                         'Cancelled';
    
    await apiClient.patch(`/orders/${orderId}`, { status: backendStatus });
    return true;
  } catch (error) {
    console.error('Failed to update order status:', error);
    return false;
  }
};

/**
 * Get restaurant analytics for different periods
 */
export const getRestaurantAnalytics = async (id: string, period: 'day' | 'week' | 'month' = 'day') => {
  const normalizedId = normalizeRestaurantId(id);
  const analytics = await getAnalytics(normalizedId, period);

  if (!analytics) return null;

  return {
    period: analytics.period,
    revenue: analytics.revenue,
    orders: analytics.orders,
    avgOrderValue: analytics.avgOrderValue,
    deliveryTime: analytics.deliveryTime
  };
};
