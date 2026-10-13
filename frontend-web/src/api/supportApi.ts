import apiClient from '@/config/axios';

export interface SupportChatInput {
  message: string;
  orderId?: string;
}

export interface SupportChatResponse {
  reply: string;
  source: string;
  intent: string;
  handoffTarget?: 'RESTAURANT' | 'ADMIN';
  orderId?: string;
  supportTicketId?: string;
  suggestions?: string[];
  requiresOrder?: boolean;
}

export type SupportIssueType =
  | 'PAYMENT'
  | 'ACCOUNT'
  | 'ORDER_CHANGE'
  | 'FOOD_ISSUE'
  | 'DELIVERY'
  | 'OTHER';

export type SupportTicketStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED';

export interface SupportTicket {
  id: string;
  orderId?: string;
  restaurantId?: string;
  customerName?: string;
  intent: string;
  message: string;
  target: 'RESTAURANT' | 'ADMIN';
  status: SupportTicketStatus;
  priority?: 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';
  department?: 'CUSTOMER_SERVICE' | 'OPERATIONS' | 'FINANCE';
  assignedAdminName?: string;
  resolutionNote?: string;
  createdAt: number;
  updatedAt?: number;
  resolvedAt?: number;
}

export interface SupportMessage {
  id: number;
  ticketId: string;
  senderRole: 'CUSTOMER' | 'RESTAURANT' | 'ADMIN' | 'SYSTEM';
  senderName: string;
  message: string;
  createdAt: number;
}

export interface CreateSupportContactInput {
  issueType: SupportIssueType;
  orderId?: string;
  message: string;
  aiTranscript?: string;
}

export async function sendSupportMessage(input: SupportChatInput): Promise<SupportChatResponse> {
  const response = await apiClient.post('/support/chat', input);
  return response.data;
}

export async function createSupportContact(
  input: CreateSupportContactInput,
): Promise<SupportTicket> {
  const response = await apiClient.post('/support/tickets/contact', input);
  return response.data;
}

export async function listMySupportTickets(): Promise<SupportTicket[]> {
  const response = await apiClient.get('/support/tickets/mine');
  return Array.isArray(response.data) ? response.data : [];
}

export async function listSupportTickets(
  target: 'RESTAURANT' | 'ADMIN',
  restaurantId?: string,
): Promise<SupportTicket[]> {
  const params = { target, ...(restaurantId ? { restaurantId } : {}) };
  const [open, inProgress] = await Promise.all([
    apiClient.get('/support/tickets', { params: { ...params, status: 'OPEN' } }),
    apiClient.get('/support/tickets', { params: { ...params, status: 'IN_PROGRESS' } }),
  ]);
  return [...(Array.isArray(open.data) ? open.data : []),
    ...(Array.isArray(inProgress.data) ? inProgress.data : [])]
    .sort((a, b) => (b.updatedAt || b.createdAt) - (a.updatedAt || a.createdAt));
}

export async function listSupportMessages(ticketId: string): Promise<SupportMessage[]> {
  const response = await apiClient.get(`/support/tickets/${ticketId}/messages`);
  return Array.isArray(response.data) ? response.data : [];
}

export async function sendTicketMessage(
  ticketId: string,
  message: string,
): Promise<SupportMessage> {
  const response = await apiClient.post(`/support/tickets/${ticketId}/messages`, { message });
  return response.data;
}

export async function resolveSupportTicket(ticketId: string): Promise<SupportTicket> {
  const response = await apiClient.patch(`/support/tickets/${ticketId}/resolve`);
  return response.data;
}
