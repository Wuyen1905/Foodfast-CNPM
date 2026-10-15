import apiClient from '../config/axios';

export type SettlementStatus = 'PENDING_REVIEW' | 'PENDING_PAYMENT' | 'PAID';

export interface SettlementOrder {
  orderId: string;
  foodAmount: number;
  restaurantTaxAmount: number;
  restaurantDiscountAmount: number;
  platformDiscountAmount: number;
  commissionAmount: number;
  refundAmount: number;
  payableAmount: number;
}

export interface RestaurantSettlement {
  id: string;
  restaurantId: string;
  restaurantName: string;
  periodStart: string;
  periodEnd: string;
  status: SettlementStatus;
  orderCount: number;
  foodAmount: number;
  restaurantTaxAmount: number;
  restaurantDiscountAmount: number;
  platformDiscountAmount: number;
  commissionRateBps: number;
  commissionAmount: number;
  refundAmount: number;
  adjustmentAmount: number;
  payableAmount: number;
  createdBy: string;
  createdAt: string;
  submittedBy?: string;
  submittedAt?: string;
  paidBy?: string;
  paidAt?: string;
  paymentReference?: string;
  orders: SettlementOrder[];
}

export interface SettlementPreview {
  restaurantId: string;
  restaurantName: string;
  eligibleOrderCount: number;
  foodAmount: number;
  restaurantTaxAmount: number;
  restaurantDiscountAmount: number;
  platformDiscountAmount: number;
  commissionAmount: number;
  refundAmount: number;
  payableAmount: number;
  orderIds: string[];
  alerts: string[];
}

export interface SettlementDashboard {
  pendingReviewCount: number;
  pendingPaymentCount: number;
  paidCount: number;
  pendingPaymentAmount: number;
  paidAmount: number;
  previews: SettlementPreview[];
  settlements: RestaurantSettlement[];
  alerts: string[];
}

export const getSettlementDashboard = async (periodStart: string, periodEnd: string) => {
  const response = await apiClient.get<SettlementDashboard>('/manager/settlements/dashboard', {
    params: { periodStart, periodEnd },
  });
  return response.data;
};

export const createSettlement = async (input: {
  restaurantId: string;
  periodStart: string;
  periodEnd: string;
}) => {
  const response = await apiClient.post<RestaurantSettlement>('/manager/settlements', input);
  return response.data;
};

export const submitSettlement = async (settlementId: string) => {
  const response = await apiClient.patch<RestaurantSettlement>(
    `/manager/settlements/${encodeURIComponent(settlementId)}/submit`,
  );
  return response.data;
};

export const paySettlement = async (settlementId: string, paymentReference: string) => {
  const response = await apiClient.patch<RestaurantSettlement>(
    `/manager/settlements/${encodeURIComponent(settlementId)}/pay`,
    { paymentReference },
  );
  return response.data;
};
