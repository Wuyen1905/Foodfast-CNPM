// Unified products data source - now fetches from backend API
import { getAllProducts as fetchAllProducts } from '@/services/menuService';

export interface Product {
  id: string;
  name: string;
  price: number;
  category: string;
  image: string;
  imageUrl?: string;
  tag?: string;
  restaurant: "SweetDreams" | "Aloha";
  available: boolean;
  manuallyAvailable?: boolean;
  customizable?: boolean;
  dailyLimit?: number | null;
  soldToday?: number;
  remainingToday?: number | null;
  soldOutToday?: boolean;
  description?: string;
  ingredients?: string[];
  preparationTime?: number;
}

/**
 * Fetches all products from the backend API
 * @returns Promise<Product[]> - Array of all products
 */
export const getAllProducts = async (): Promise<Product[]> => {
  try {
    return await fetchAllProducts();
  } catch (error) {
    console.error('Error fetching products from API:', error);
    return [];
  }
};

// Helper function to get product image
export const getProductImage = (product: Product): string => {
  return product.image || '/images/default-dish.jpg';
};

// Export getAllProducts as default for backward compatibility
export default getAllProducts;
