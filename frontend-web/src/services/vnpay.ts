export interface VNPayAvailability {
  enabled: boolean;
  message: string;
}

export interface VNPayPaymentResult {
  verified: boolean;
  success: boolean;
  orderId?: string;
  paymentStatus?: 'pending' | 'paid' | 'failed' | 'expired';
  transactionId?: string;
  amount?: number;
  responseCode?: string;
  message: string;
}

async function readJson(response: Response): Promise<any> {
  try {
    return await response.json();
  } catch {
    return {};
  }
}

function paymentError(message: string, disabled = false): Error & { vnpayDisabled?: boolean } {
  const error = new Error(message) as Error & { vnpayDisabled?: boolean };
  if (disabled) {
    error.vnpayDisabled = true;
  }
  return error;
}

function authHeaders(): Record<string, string> {
  if (typeof sessionStorage === 'undefined') return {};
  const token = sessionStorage.getItem('token');
  return token ? { Authorization: `Bearer ${token}` } : {};
}

export async function getVNPayAvailability(): Promise<VNPayAvailability> {
  const response = await fetch('/api/payment/vnpay/status', {
    headers: { Accept: 'application/json' },
  });
  const data = await readJson(response);
  if (!response.ok) {
    throw paymentError(data.message || 'Không thể kiểm tra trạng thái VNPay');
  }
  return data as VNPayAvailability;
}

/**
 * Create VNPay payment URL via backend API
 * This function calls the backend to generate the payment URL
 */
export async function createVNPayUrl(orderId: string, idempotencyKey?: string): Promise<string> {
  const key = idempotencyKey || `vnpay-${orderId}-${crypto.randomUUID()}`;
  const res = await fetch("/api/payment/vnpay/create", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "Idempotency-Key": key,
      ...authHeaders(),
    },
    body: JSON.stringify({ orderId })
  });

  const data = await readJson(res);

  if (data.enabled === false) {
    throw paymentError(data.message || "VNPay chưa khả dụng", true);
  }

  if (!res.ok) throw paymentError(data.message || "Không thể tạo phiên thanh toán VNPay");
  return data.url;
}

export async function reconcileVNPayOrder(orderId: string): Promise<VNPayPaymentResult> {
  const response = await fetch(`/api/payment/vnpay/orders/${encodeURIComponent(orderId)}/status`, {
    headers: { Accept: 'application/json', ...authHeaders() },
  });
  const data = await readJson(response);
  if (!response.ok) {
    throw paymentError(data.message || 'Không thể kiểm tra trạng thái thanh toán');
  }
  return data as VNPayPaymentResult;
}

export async function verifyVNPayReturn(params: URLSearchParams): Promise<VNPayPaymentResult> {
  const response = await fetch(`/api/payment/vnpay/return?${params.toString()}`, {
    headers: { Accept: 'application/json' },
  });
  const data = await readJson(response);
  if (!response.ok) {
    throw paymentError(data.message || 'Không thể xác minh kết quả VNPay');
  }
  return data as VNPayPaymentResult;
}

