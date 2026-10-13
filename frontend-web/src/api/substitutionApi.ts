import apiClient from '../config/axios';

export type SubstitutionStatus =
  | 'PENDING_CUSTOMER'
  | 'ACCEPTED'
  | 'REJECTED'
  | 'EXPIRED'
  | 'CANCELLED';

export interface SubstitutionProposal {
  id: number;
  orderId: string;
  orderItemId: number;
  restaurantId: string;
  customerId: string;
  originalProductId?: string;
  originalProductName: string;
  originalUnitPrice: number;
  replacementProductId: string;
  replacementProductName: string;
  replacementUnitPrice: number;
  reason?: string;
  status: SubstitutionStatus;
  proposedAt: string;
  expiresAt: string;
  respondedAt?: string;
}

export const listSubstitutions = async (orderId: string): Promise<SubstitutionProposal[]> => {
  const response = await apiClient.get(`/orders/${orderId}/substitutions`);
  return response.data;
};

export const proposeSubstitution = async (orderId: string, body: {
  orderItemId: number;
  replacementProductId: string;
  reason: string;
  expiresInMinutes?: number;
}): Promise<SubstitutionProposal> => {
  const response = await apiClient.post(`/orders/${orderId}/substitutions`, body);
  return response.data;
};

export const respondToSubstitution = async (
  orderId: string,
  proposalId: number,
  accept: boolean,
): Promise<SubstitutionProposal> => {
  const response = await apiClient.post(`/orders/${orderId}/substitutions/${proposalId}/respond`, {
    accept,
  });
  return response.data;
};
