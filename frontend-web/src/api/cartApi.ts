import apiClient from '../config/axios';

export interface CartItem {
  id: number;
  productId: string | number;
  productName: string;
  productImage?: string;
  unitPrice: number;
  quantity: number;
  restaurantCode?: string;
  variantKey: string;
  selectedOptionIds: string[];
  optionSummary?: string;
  itemNote?: string;
}

export const getCart = async (): Promise<CartItem[]> => {
  const response = await apiClient.get('/cart');
  return response.data;
};

export const addToCart = async (body: {
  productId: string | number;
  quantity: number;
  selectedOptionIds?: string[];
  itemNote?: string;
}): Promise<CartItem[]> => {
  const response = await apiClient.post('/cart/add', body);
  return response.data;
};

export const removeFromCart = async (productId: string): Promise<CartItem[]> => {
  const response = await apiClient.delete(`/cart/product/${productId}`);
  return response.data;
};

export const clearCart = async (): Promise<CartItem[]> => {
  const response = await apiClient.delete('/cart/clear');
  return response.data;
};

