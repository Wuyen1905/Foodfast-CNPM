/**
 * Admin Dashboard Type Definitions
 */

export type RestaurantStatus = 'PENDING' | 'ACTIVE' | 'SUSPENDED' | 'CLOSED';
export type CustomerAccountStatus = 'Active' | 'Suspended';

export interface AdminRestaurant {
  id: string;
  name: string;
  category: string;
  status: RestaurantStatus;
  statusReason?: string;
  statusUpdatedAt?: number;
  ownerId: string;
  ownerName: string;
  totalOrders: number;
  totalRevenue: number; // in VND
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
  totalSpend: number; // in VND
  accountStatus: CustomerAccountStatus;
  createdAt: number;
  lastOrderDate?: number;
  lastLoginAt?: number;
  suspensionReason?: string;
  accountStatusUpdatedAt?: number;
}

export interface SystemLog {
  id: string;
  timestamp: number;
  adminId: string;
  adminName: string;
  action: 'restaurant_approved' | 'restaurant_suspended' | 'restaurant_activated' | 
          'customer_suspended' | 'customer_activated' | 'courier_deactivated' |
          'emergency_override' | 'order_status_changed';
  targetType: 'restaurant' | 'customer' | 'courier' | 'order';
  targetId: string;
  targetName: string;
  details: string;
  severity: 'info' | 'warning' | 'critical';
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

