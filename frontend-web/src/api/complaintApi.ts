import apiClient from '@/config/axios';

export type ComplaintCategory = 'MISSING_ITEM' | 'WRONG_ITEM' | 'DAMAGED_ITEM' | 'LATE_DELIVERY' | 'OTHER';
export type ComplaintStatus = 'OPEN' | 'REVIEWING' | 'RESOLVED' | 'REJECTED';
export type ComplaintActorRole = 'CUSTOMER' | 'RESTAURANT' | 'ADMIN';

export interface ComplaintHistoryItem {
  id: number;
  action: string;
  actorRole: string;
  actorId: string;
  fromStatus?: string;
  toStatus: string;
  note?: string;
  createdAt: number;
}

export interface Complaint {
  id: string;
  orderId: string;
  restaurantId: string;
  customerId: string;
  category: ComplaintCategory;
  description: string;
  evidenceUrls: string[];
  status: ComplaintStatus;
  handlerRole?: string;
  handlerId?: string;
  resolution?: string;
  createdAt: number;
  updatedAt: number;
  resolvedAt?: number;
  history: ComplaintHistoryItem[];
}

export const COMPLAINT_CATEGORY_LABELS: Record<ComplaintCategory, string> = {
  MISSING_ITEM: 'Thiếu món',
  WRONG_ITEM: 'Giao sai món',
  DAMAGED_ITEM: 'Món bị đổ, hỏng',
  LATE_DELIVERY: 'Giao trễ',
  OTHER: 'Vấn đề khác',
};

export const COMPLAINT_STATUS_LABELS: Record<ComplaintStatus, string> = {
  OPEN: 'Mới tiếp nhận',
  REVIEWING: 'Đang xử lý',
  RESOLVED: 'Đã có kết quả',
  REJECTED: 'Đã từ chối',
};

export async function createComplaint(body: {
  orderId: string;
  category: ComplaintCategory;
  description: string;
  evidenceUrls: string[];
}): Promise<Complaint> {
  return (await apiClient.post('/complaints', body)).data;
}

export async function uploadComplaintEvidence(file: File): Promise<string> {
  const data = new FormData();
  data.append('file', file);
  const response = await apiClient.post('/complaints/evidence', data, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return response.data.url;
}

export async function openComplaintEvidence(url: string): Promise<void> {
  if (url.startsWith('http://') || url.startsWith('https://')) {
    window.open(url, '_blank', 'noopener,noreferrer');
    return;
  }
  const target = window.open('', '_blank');
  const endpoint = url.startsWith('/api/') ? url.slice(4) : url;
  try {
    const response = await apiClient.get(endpoint, { responseType: 'blob' });
    const objectUrl = URL.createObjectURL(response.data);
    if (target) target.location.href = objectUrl;
    window.setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000);
  } catch (error) {
    target?.close();
    throw error;
  }
}

export async function listComplaints(status?: ComplaintStatus): Promise<Complaint[]> {
  return (await apiClient.get('/complaints', { params: { status } })).data;
}

export async function reviewComplaint(
  complaintId: string,
  body: {
    action: 'START_REVIEW' | 'RESOLVE' | 'REJECT';
    note?: string;
  },
): Promise<Complaint> {
  return (await apiClient.patch(`/complaints/${complaintId}/review`, body)).data;
}
