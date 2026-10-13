import React, { createContext, useContext, useState, useEffect, ReactNode } from "react";
import { User, AdminAuthContextValue } from "../types/auth";

const AdminAuthContext = createContext<AdminAuthContextValue | undefined>(undefined);

export const AdminAuthProvider = ({ children }: { children: ReactNode }) => {
  const [admin, setAdmin] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  // Initialize admin from localStorage on mount
  useEffect(() => {
    const restoreAdmin = async () => {
      const saved = sessionStorage.getItem("admin_auth");
      const token = sessionStorage.getItem("token");
      if (!saved || !token) {
        setLoading(false);
        return;
      }
      try {
        const { getCurrentUser } = await import('../api/authApi');
        const user = await getCurrentUser();
        if (user.role === 'admin') setAdmin(user);
        else throw new Error('Phiên hiện tại không phải quản trị viên');
      } catch (error) {
        console.warn('Admin session is no longer valid:', error);
        sessionStorage.removeItem('admin_auth');
        sessionStorage.removeItem('auth_user');
        sessionStorage.removeItem('token');
        sessionStorage.removeItem('role');
      } finally {
        setLoading(false);
      }
    };
    void restoreAdmin();
  }, []);

  useEffect(() => {
    if (admin) sessionStorage.setItem("admin_auth", JSON.stringify(admin));
    else sessionStorage.removeItem("admin_auth");
  }, [admin]);

  const login = async (username: string, password: string) => {
    setLoading(true);
    try {
      const { login: loginApi } = await import('../api/authApi');
      const response = await loginApi({ username, password });
      
      if (response.ok && response.user && response.token && response.user.role === 'admin') {
        sessionStorage.setItem('token', response.token);
        sessionStorage.setItem('auth_user', JSON.stringify(response.user));
        sessionStorage.setItem('role', response.user.role);
        setAdmin(response.user);
        setLoading(false);
        return { ok: true };
      }
      
      setLoading(false);
      return { ok: false, message: response.message || "Thông tin đăng nhập quản trị không đúng" };
    } catch (error) {
      console.error('Admin login error:', error);
      setLoading(false);
      return { ok: false, message: "Đăng nhập thất bại. Vui lòng thử lại." };
    }
  };

  const logout = () => {
    sessionStorage.removeItem('admin_auth');
    sessionStorage.removeItem('auth_user');
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('role');
    setAdmin(null);
  };

  return (
    <AdminAuthContext.Provider value={{ admin, loading, login, logout }}>
      {loading ? (
        <div style={{ 
          display: 'flex', 
          justifyContent: 'center', 
          alignItems: 'center', 
          height: '100vh',
          fontSize: '18px',
          color: '#666'
        }}>
          Đang tải trang quản trị...
        </div>
      ) : (
        children
      )}
    </AdminAuthContext.Provider>
  );
};

export const useAdminAuth = () => {
  const ctx = useContext(AdminAuthContext);
  if (!ctx) {
    console.error("useAdminAuth must be used inside AdminAuthProvider");
    return {
      admin: null,
      loading: false,
      login: async () => ({ ok: false, message: "Chức năng đăng nhập quản trị chưa sẵn sàng" }),
      logout: () => {}
    };
  }
  return ctx;
};
