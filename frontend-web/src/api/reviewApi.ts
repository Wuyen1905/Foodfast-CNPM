import apiClient from '@/config/axios';

export interface Review {
  id: string;
  orderId: string;
  userId: string;
  restaurantId: string;
  rating: number;
  comment?: string;
  createdAt: number;
  updatedAt: number;
}

export const getOrderReview = async (orderId: string): Promise<Review | null> => {
  try {
    const response = await apiClient.get(`/orders/${orderId}/review`);
    return response.data;
  } catch (error: any) {
    if (error?.response?.status === 404) return null;
    throw error;
  }
};

export const createOrderReview = async (
  orderId: string,
  body: { rating: number; comment?: string },
): Promise<Review> => {
  const response = await apiClient.post(`/orders/${orderId}/review`, body);
  return response.data;
};

export const getRestaurantReviews = async (restaurantId: string): Promise<Review[]> => {
  const response = await apiClient.get(`/restaurants/${restaurantId}/reviews`);
  return response.data;
};
