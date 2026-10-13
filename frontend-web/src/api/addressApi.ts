import apiClient from '@/config/axios';

export interface DeliveryAddress {
  id: string;
  userId: string;
  label: string;
  recipientName: string;
  phone: string;
  addressLine: string;
  defaultAddress: boolean;
  createdAt: number;
  updatedAt: number;
}

export type DeliveryAddressInput = Pick<
  DeliveryAddress,
  'label' | 'recipientName' | 'phone' | 'addressLine' | 'defaultAddress'
>;

export const getAddresses = async (): Promise<DeliveryAddress[]> => {
  const response = await apiClient.get('/addresses');
  return response.data;
};

export const createAddress = async (input: DeliveryAddressInput): Promise<DeliveryAddress> => {
  const response = await apiClient.post('/addresses', input);
  return response.data;
};

export const updateAddress = async (
  addressId: string,
  input: DeliveryAddressInput,
): Promise<DeliveryAddress> => {
  const response = await apiClient.put(`/addresses/${encodeURIComponent(addressId)}`, input);
  return response.data;
};

export const makeDefaultAddress = async (addressId: string): Promise<DeliveryAddress> => {
  const response = await apiClient.put(`/addresses/${encodeURIComponent(addressId)}/default`);
  return response.data;
};

export const deleteAddress = async (addressId: string): Promise<void> => {
  await apiClient.delete(`/addresses/${encodeURIComponent(addressId)}`);
};
