import axios from 'axios';

// Base URL for the Spring Boot backend API
// Use environment variable or the local backend used by the web app.
const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ??
  "http://localhost:8080/api";

// Create axios instance with default config
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor
apiClient.interceptors.request.use(
  (config) => {
    // Mỗi tab giữ một phiên riêng để khách hàng, nhà hàng và admin có thể
    // cùng vận hành mà không ghi đè token của nhau.
    const token = sessionStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor
apiClient.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    // Handle common errors
    if (error.response?.status === 401) {
      const requestUrl = String(error.config?.url || '');
      const isLoginAttempt = requestUrl.includes('/auth/login');

      // Sai thông tin trên chính form đăng nhập phải được hiển thị tại form,
      // không tải lại trang. Các API khác mới kết thúc phiên hiện tại.
      if (!isLoginAttempt) {
        sessionStorage.removeItem('auth_user');
        sessionStorage.removeItem('admin_auth');
        sessionStorage.removeItem('token');
        sessionStorage.removeItem('role');

        const adminRoute = window.location.pathname.startsWith('/admin');
        const managerRoute = window.location.pathname.startsWith('/manager');
        const loginPath = adminRoute ? '/admin/login' : managerRoute ? '/manager/login' : '/login';
        if (window.location.pathname !== loginPath) {
          window.location.assign(loginPath);
        }
      }
    }
    return Promise.reject(error);
  }
);

export default apiClient;

