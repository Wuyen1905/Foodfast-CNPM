import apiClient from '../config/axios';
import type { OrderStatus, PaymentStatus } from '@/types/status';

export interface CreateOrderRequest {
  customerPhone: string; // Backend expects customerPhone
  address: string;
  restaurant?: string;
  restaurantId?: string;
  items: Array<{
    productId?: string;
    name?: string;
    quantity: number;
    price?: number;
    selectedOptionIds?: string[];
    itemNote?: string;
  }>;
  paymentMethod?: string;
  paymentSessionId?: string;
  note?: string;
  voucherCode?: string;
}

export interface OrderQuote {
  restaurantId: string;
  restaurantName: string;
  items: Array<{
    productId: string;
    name: string;
    quantity: number;
    unitPrice: number;
    lineTotal: number;
    selectedOptions: Array<{
      optionId: string;
      groupName: string;
      optionName: string;
      priceDelta: number;
    }>;
    itemNote?: string;
  }>;
  subtotal: number;
  shippingFee: number;
  taxAmount: number;
  discountAmount: number;
  total: number;
  voucherCode?: string;
  voucherName?: string;
}

export type CancellationActor = 'CUSTOMER' | 'RESTAURANT' | 'ADMIN';

export type CancellationReasonCode =
  | 'CUSTOMER_ORDERED_BY_MISTAKE'
  | 'CUSTOMER_WRONG_ADDRESS'
  | 'CUSTOMER_CHANGED_MIND'
  | 'CUSTOMER_WAIT_TOO_LONG'
  | 'RESTAURANT_OUT_OF_STOCK'
  | 'RESTAURANT_OVERLOADED'
  | 'RESTAURANT_CLOSED'
  | 'RESTAURANT_CANNOT_DELIVER'
  | 'OTHER';

export interface CancelOrderRequest {
  reasonCode: CancellationReasonCode;
  detail?: string;
}

export interface Order {
  id: string;
  orderGroupId?: string;
  groupPosition?: number;
  name: string; // Maps to customerName in backend
  phone: string; // Maps to customerPhone in backend
  customerName?: string;
  customerPhone?: string;
  customerEmail?: string;
  address: string;
  restaurant?: string;
  restaurantId?: string;
  userId?: string;
  status: OrderStatus;
  total: number;
  items: Array<{
    id?: string;
    productId?: string;
    name: string;
    qty?: number; // Frontend format
    quantity?: number; // Alternative format
    price: number;
    productName?: string;
    productImage?: string;
    itemNote?: string;
    selectedOptions?: Array<{
      optionId?: string;
      groupName: string;
      optionName: string;
      priceDelta: number;
    }>;
    originalProductId?: string;
    originalProductName?: string;
    substitutionProposalId?: number;
  }>;
  createdAt: number; // Long timestamp
  updatedAt: number; // Long timestamp
  paymentMethod?: string;
  paymentStatus?: PaymentStatus;
  vnpayTransactionId?: string;
  confirmedAt?: number;
  paidAt?: number;
  preparingAt?: number;
  readyAt?: number;
  deliveringAt?: number;
  deliveredAt?: number;
  cancelledAt?: number;
  internalNotes?: string;
  confirmedBy?: string;
  cancellationActor?: CancellationActor;
  cancellationActorId?: string;
  cancellationReasonCode?: CancellationReasonCode;
  cancellationReasonDetail?: string;
  subtotal?: number;
  shippingFee?: number;
  taxAmount?: number;
  discountAmount?: number;
  voucherCode?: string;
  voucherName?: string;
  partialRefundAmount?: number;
  paymentAllocatedAmount?: number;
  refundTransactionReference?: string;
  refundCompletedAt?: number;
  refundNote?: string;
}

export interface CreateOrderGroupRequest {
  customerPhone: string;
  address: string;
  note?: string;
  paymentMethod?: string;
  items: CreateOrderRequest['items'];
  voucherCodesByRestaurant?: Record<string, string>;
}

export interface OrderGroupQuote {
  orders: OrderQuote[];
  subtotal: number;
  shippingFee: number;
  taxAmount: number;
  discountAmount: number;
  total: number;
}

export interface OrderGroup {
  id: string;
  userId: string;
  customerName: string;
  customerPhone: string;
  customerEmail?: string;
  address: string;
  note?: string;
  paymentMethod: string;
  paymentStatus: PaymentStatus;
  subtotal: number;
  shippingFee: number;
  taxAmount: number;
  discountAmount: number;
  total: number;
  payableAmount: number;
  partialRefundAmount: number;
  refundedAmount: number;
  vnpayTransactionId?: string;
  paidAt?: number;
  orderCount: number;
  createdAt: number;
  updatedAt: number;
  orders: Order[];
}

export interface OrderStatusHistory {
  id: number;
  orderId: string;
  fromStatus?: OrderStatus;
  toStatus: OrderStatus;
  actorId?: string;
  actorRole: 'CUSTOMER' | 'RESTAURANT' | 'ADMIN' | 'SYSTEM';
  source: string;
  reason?: string;
  createdAt: number;
}

export const quoteOrder = async (body: CreateOrderRequest): Promise<OrderQuote> => {
  const response = await apiClient.post('/orders/quote', body);
  return response.data;
};

export const createOrder = async (body: CreateOrderRequest, idempotencyKey: string): Promise<Order> => {
  const response = await apiClient.post('/orders', body, {
    headers: { 'Idempotency-Key': idempotencyKey },
  });
  return response.data;
};

export const quoteOrderGroup = async (body: CreateOrderGroupRequest): Promise<OrderGroupQuote> => {
  const response = await apiClient.post('/order-groups/quote', body);
  return response.data;
};

export const createOrderGroup = async (
  body: CreateOrderGroupRequest,
  idempotencyKey: string,
): Promise<OrderGroup> => {
  const response = await apiClient.post('/order-groups', body, {
    headers: { 'Idempotency-Key': idempotencyKey },
  });
  return response.data;
};

export const getOrderGroups = async (): Promise<OrderGroup[]> => {
  const response = await apiClient.get('/order-groups');
  return response.data;
};

export const getOrderGroup = async (id: string): Promise<OrderGroup> => {
  const response = await apiClient.get(`/order-groups/${encodeURIComponent(id)}`);
  return response.data;
};

export const getOrders = async (params?: {
  phone?: string;
  restaurant?: string;
  paymentSessionId?: string;
}): Promise<Order[]> => {
  const response = await apiClient.get('/orders', { params });
  return response.data;
};

export const getOrder = async (id: string): Promise<Order> => {
  const response = await apiClient.get(`/orders/${id}`);
  return response.data;
};

export const updateOrderStatus = async (id: string, status: OrderStatus): Promise<Order> => {
  const backendStatus = status === 'In Progress' ? 'PREPARING' : status;
  const response = await apiClient.patch(`/orders/${id}`, { status: backendStatus });
  return response.data;
};

export const getOrderStatusHistory = async (id: string): Promise<OrderStatusHistory[]> => {
  const response = await apiClient.get(`/orders/${id}/history`);
  return response.data;
};

export const cancelOrder = async (id: string, body: CancelOrderRequest): Promise<Order> => {
  const response = await apiClient.post(`/orders/${id}/cancel`, body);
  return response.data;
};

export const completeOrderRefund = async (
  id: string,
  transactionReference: string,
  note?: string,
): Promise<Order> => {
  const response = await apiClient.patch(
    `/admin/payments/orders/${encodeURIComponent(id)}/refund/complete`,
    { transactionReference, note },
  );
  return response.data;
};

export const getOrdersByUser = async (userId: string): Promise<Order[]> => {
  const response = await apiClient.get(`/orders?userId=${userId}`);
  return response.data;
};

export const getOrdersByRestaurant = async (restaurantId: string): Promise<Order[]> => {
  const response = await apiClient.get(`/orders?restaurant=${restaurantId}`);
  return response.data;
};

