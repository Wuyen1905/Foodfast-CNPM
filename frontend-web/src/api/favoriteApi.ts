import apiClient from '@/config/axios';
import type { Product } from '@/api/productApi';

export const getFavoriteProducts = async (): Promise<Product[]> => {
  const response = await apiClient.get('/favorites');
  return response.data;
};

export const getFavoriteIds = async (): Promise<string[]> => {
  const response = await apiClient.get('/favorites/ids');
  return response.data;
};

export const addFavorite = async (productId: string): Promise<string[]> => {
  const response = await apiClient.post(`/favorites/${encodeURIComponent(productId)}`);
  return response.data;
};

export const removeFavorite = async (productId: string): Promise<string[]> => {
  const response = await apiClient.delete(`/favorites/${encodeURIComponent(productId)}`);
  return response.data;
};
