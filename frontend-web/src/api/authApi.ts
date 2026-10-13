import apiClient from '../config/axios';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  password: string;
  fullName: string;
  email: string;
  phone: string;
}

export interface LoginResponse {
  ok: boolean;
  user?: any;
  token?: string;
  message?: string;
}

export interface RegisterResponse {
  ok: boolean;
  data?: {
    id: string;
    name: string;
    username: string;
    email?: string;
    phone?: string;
    role: string;
    orderCount?: number;
    createdAt?: number;
  };
  message?: string;
}

export const login = async (body: LoginRequest): Promise<LoginResponse> => {
  const response = await apiClient.post('/auth/login', body);
  return response.data;
};

export const register = async (body: RegisterRequest): Promise<RegisterResponse> => {
  const response = await apiClient.post('/auth/register', body);
  return response.data;
};

export const getCurrentUser = async (): Promise<any> => {
  const response = await apiClient.get('/auth/me');
  return response.data;
};

