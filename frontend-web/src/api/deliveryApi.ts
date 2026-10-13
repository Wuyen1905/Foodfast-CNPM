import apiClient from '@/config/axios';
import type { DeliveryStatus } from '@/types/status';

export type { DeliveryStatus } from '@/types/status';

export interface RestaurantCourier {
  id: string;
  restaurantId: string;
  name: string;
  phone: string;
  vehicleType?: string;
  licensePlate?: string;
  active: boolean;
  createdAt: number;
  updatedAt: number;
}

export interface DeliveryDetails {
  id: string;
  orderId: string;
  restaurantId: string;
  courierId: string;
  courierName: string;
  vehicleType?: string;
  licensePlate?: string;
  status: DeliveryStatus;
  estimatedDeliveryAt?: number;
  deliveryNote?: string;
  failureReason?: string;
  assignedAt: number;
  startedAt?: number;
  deliveredAt?: number;
  completionSource?: 'CUSTOMER' | 'RESTAURANT_OVERRIDE';
  completionNote?: string;
  failedAt?: number;
  cancelledAt?: number;
  updatedAt: number;
}

export interface CreateCourierInput {
  name: string;
  phone: string;
  vehicleType?: string;
  licensePlate?: string;
}

export interface AssignDeliveryInput {
  orderId: string;
  courierId: string;
  estimatedDeliveryAt?: number;
  deliveryNote?: string;
}

export async function listCouriers(
  restaurantId: string,
  activeOnly = false,
): Promise<RestaurantCourier[]> {
  const response = await apiClient.get(`/restaurants/${restaurantId}/couriers`, {
    params: { activeOnly },
  });
  return response.data;
}

export async function createCourier(
  restaurantId: string,
  input: CreateCourierInput,
): Promise<RestaurantCourier> {
  const response = await apiClient.post(`/restaurants/${restaurantId}/couriers`, input);
  return response.data;
}

export async function updateCourier(
  courierId: string,
  input: Partial<CreateCourierInput> & { active?: boolean },
): Promise<RestaurantCourier> {
  const response = await apiClient.patch(`/couriers/${courierId}`, input);
  return response.data;
}

export async function deactivateCourier(courierId: string): Promise<void> {
  await apiClient.delete(`/couriers/${courierId}`);
}

export async function assignDelivery(input: AssignDeliveryInput): Promise<DeliveryDetails> {
  const response = await apiClient.post('/deliveries/assign', input);
  return response.data;
}

export async function getDeliveryForOrder(orderId: string): Promise<DeliveryDetails | null> {
  try {
    const response = await apiClient.get(`/deliveries/order/${orderId}`);
    return response.data;
  } catch (error: any) {
    if (error?.response?.status === 404) return null;
    throw error;
  }
}

export async function listRestaurantDeliveries(restaurantId: string): Promise<DeliveryDetails[]> {
  const response = await apiClient.get(`/restaurants/${restaurantId}/deliveries`);
  return response.data;
}

export async function startDelivery(assignmentId: string): Promise<DeliveryDetails> {
  const response = await apiClient.post(`/deliveries/${assignmentId}/start`);
  return response.data;
}

export async function confirmDeliveryReceived(
  assignmentId: string,
): Promise<DeliveryDetails> {
  const response = await apiClient.post(`/deliveries/${assignmentId}/customer-confirm`);
  return response.data;
}

export async function completeDeliveryByRestaurantOverride(
  restaurantId: string,
  assignmentId: string,
  reason: string,
): Promise<DeliveryDetails> {
  const response = await apiClient.post(
    `/restaurants/${restaurantId}/deliveries/${assignmentId}/complete-override`,
    { reason },
  );
  return response.data;
}

export async function failDelivery(assignmentId: string, reason: string): Promise<DeliveryDetails> {
  const response = await apiClient.post(`/deliveries/${assignmentId}/fail`, { reason });
  return response.data;
}

export async function cancelDelivery(assignmentId: string): Promise<DeliveryDetails> {
  const response = await apiClient.post(`/deliveries/${assignmentId}/cancel`);
  return response.data;
}
