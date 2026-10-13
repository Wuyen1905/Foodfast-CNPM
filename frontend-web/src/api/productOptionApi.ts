import apiClient from '../config/axios';

export type ProductOptionSelectionType = 'SINGLE' | 'MULTIPLE';

export interface ProductOption {
  id: string;
  groupId?: string;
  name: string;
  priceDelta: number;
  displayOrder: number;
  active: boolean;
}

export interface ProductOptionGroup {
  id: string;
  productId?: string;
  name: string;
  selectionType: ProductOptionSelectionType;
  required: boolean;
  minSelections: number;
  maxSelections: number;
  displayOrder: number;
  active: boolean;
  options: ProductOption[];
}

export const getProductOptionGroups = async (productId: string): Promise<ProductOptionGroup[]> => {
  const response = await apiClient.get(`/products/${productId}/option-groups`);
  return response.data;
};

export const getProductOptionGroupsForManagement = async (
  productId: string,
): Promise<ProductOptionGroup[]> => {
  const response = await apiClient.get(`/products/${productId}/option-groups/manage`);
  return response.data;
};

export const replaceProductOptionGroups = async (
  productId: string,
  groups: ProductOptionGroup[],
): Promise<ProductOptionGroup[]> => {
  const response = await apiClient.put(`/products/${productId}/option-groups`, {
    groups,
  });
  return response.data;
};
