import apiClient from '../config/axios';

export interface VoucherApplication {
  code: string;
  name: string;
  discountAmount: number;
}

export interface AvailableVoucher {
  code: string;
  name: string;
  discountType: VoucherDiscountType;
  discountValue: number;
  minOrderAmount: number;
  maxDiscount: number;
  endsAt: string;
  eligible: boolean;
  amountNeeded: number;
  estimatedDiscount: number;
  message: string;
}

export type VoucherDiscountType = 'PERCENT' | 'FIXED';

export interface ManagedVoucher {
  code: string;
  name: string;
  restaurantId?: string | null;
  discountType: VoucherDiscountType;
  discountValue: number;
  minOrderAmount: number;
  maxDiscount: number;
  startsAt: string;
  endsAt: string;
  usageLimit: number;
  perUserLimit: number;
  active: boolean;
  usedCount: number;
}

export interface VoucherUpsertPayload {
  code: string;
  name: string;
  restaurantId?: string | null;
  discountType: VoucherDiscountType;
  discountValue: number;
  minOrderAmount: number;
  maxDiscount: number;
  startsAt: string;
  endsAt: string;
  usageLimit: number;
  perUserLimit: number;
  active: boolean;
}

export const validateVoucher = async (body: {
  code: string;
  restaurantId: string;
  subtotal: number;
}): Promise<VoucherApplication> => {
  const response = await apiClient.post('/vouchers/validate', body);
  return response.data;
};

export const getAvailableVouchers = async (
  restaurantId: string,
  subtotal: number,
): Promise<AvailableVoucher[]> => {
  const response = await apiClient.get('/vouchers/available', {
    params: { restaurantId, subtotal },
  });
  return response.data;
};

export const getManagedVouchers = async (restaurantId?: string): Promise<ManagedVoucher[]> => {
  const response = await apiClient.get('/vouchers/manage', {
    params: restaurantId ? { restaurantId } : undefined,
  });
  return response.data;
};

export const createManagedVoucher = async (
  payload: VoucherUpsertPayload,
): Promise<ManagedVoucher> => {
  const response = await apiClient.post('/vouchers/manage', payload);
  return response.data;
};

export const updateManagedVoucher = async (
  code: string,
  payload: VoucherUpsertPayload,
): Promise<ManagedVoucher> => {
  const response = await apiClient.put(`/vouchers/manage/${encodeURIComponent(code)}`, payload);
  return response.data;
};

export const setManagedVoucherActive = async (
  code: string,
  active: boolean,
): Promise<ManagedVoucher> => {
  const response = await apiClient.patch(
    `/vouchers/manage/${encodeURIComponent(code)}/active`,
    { active },
  );
  return response.data;
};
