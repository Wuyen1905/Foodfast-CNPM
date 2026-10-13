import apiClient from '../config/axios';

export interface RestaurantAnalytics {
  period: string;
  revenue: number;
  orders: number;
  avgOrderValue: number;
  deliveryTime: number;
}

export interface RestaurantOverview {
  id: string;
  name: string;
  revenue: number;
  ordersToday: number;
  activeDeliveries: number;
  avgDeliveryTime: number;
  avgConfirmationTime: number;
  completionRate: number;
  cancellationRate: number;
  completedToday: number;
  cancelledToday: number;
  rating: number;
  statusCounts: Record<string, number>;
  revenueTrend: Array<{
    date: string;
    label: string;
    value: number;
  }>;
  topItems: Array<{
    name: string;
    orders: number;
    sales: number;
    revenue: number;
  }>;
}

// Get restaurant analytics for a period
export const getRestaurantAnalytics = async (
  restaurantId: string,
  period: 'day' | 'week' | 'month' = 'day'
): Promise<RestaurantAnalytics | null> => {
  try {
    // Use backend analytics endpoint
    const response = await apiClient.get(`/analytics/restaurant/${restaurantId}`, {
      params: { period }
    });
    const analytics = response.data;
    
    // Backend already returns RestaurantAnalytics format
    return {
      period: analytics.period || (period === 'day' ? 'Hôm nay' : period === 'week' ? 'Tuần này' : 'Tháng này'),
      revenue: analytics.revenue || 0,
      orders: analytics.orders || 0,
      avgOrderValue: analytics.avgOrderValue || 0,
      deliveryTime: analytics.deliveryTime ?? 0
    };
  } catch (error) {
    console.error('Failed to get restaurant analytics:', error);
    return null;
  }
};

// Get restaurant overview
export const getRestaurantOverview = async (restaurantId: string): Promise<RestaurantOverview | null> => {
  try {
    // Use backend analytics overview endpoint
    const response = await apiClient.get(`/analytics/restaurant/${restaurantId}/overview`);
    const overview = response.data;
    
    // Backend already returns RestaurantOverview format
    return {
      id: overview.id || restaurantId,
      name: overview.name || '',
      revenue: overview.revenue || 0,
      ordersToday: overview.ordersToday || 0,
      activeDeliveries: overview.activeDeliveries || 0,
      avgDeliveryTime: overview.avgDeliveryTime ?? 0,
      avgConfirmationTime: overview.avgConfirmationTime || 0,
      completionRate: overview.completionRate || 0,
      cancellationRate: overview.cancellationRate || 0,
      completedToday: overview.completedToday || 0,
      cancelledToday: overview.cancelledToday || 0,
      rating: overview.rating || 0,
      statusCounts: overview.statusCounts || {},
      revenueTrend: overview.revenueTrend || [],
      topItems: overview.topItems || []
    };
  } catch (error) {
    console.error('Failed to get restaurant overview:', error);
    return null;
  }
};

