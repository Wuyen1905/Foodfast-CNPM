import React, { createContext, useContext, useState, useEffect, useRef, ReactNode } from "react";
import { User, UserRole, AuthContextValue, RegisterPayload } from "../types/auth";
import { getCurrentUser, login as apiLogin, register as apiRegister } from "../api/authApi";
import { realtimeSocket } from "../realtime/socket";

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

const clearStoredSession = () => {
  sessionStorage.removeItem("auth_user");
  sessionStorage.removeItem("token");
  sessionStorage.removeItem("role");
};

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  // Track whether user was EVER set — so logout clears localStorage but initial null does not
  const hadUser = useRef(false);

  // Khôi phục phiên bằng cách hỏi backend; không tin role/user bị sửa trong localStorage.
  useEffect(() => {
    const restoreSession = async () => {
      const saved = sessionStorage.getItem("auth_user");
      const token = sessionStorage.getItem("token");
      if (!token) {
        if (saved || sessionStorage.getItem("role")) clearStoredSession();
        setLoading(false);
        return;
      }
      try {
        const verifiedUser = await getCurrentUser();
        setUser(verifiedUser as User);
        sessionStorage.setItem("auth_user", JSON.stringify(verifiedUser));
        sessionStorage.setItem("role", verifiedUser.role);
      } catch (error) {
        console.warn("[AuthContext] Phiên đăng nhập không còn hợp lệ", error);
        clearStoredSession();
      } finally {
        setLoading(false);
      }
    };
    void restoreSession();
  }, []);

  useEffect(() => {
    if (user) {
      hadUser.current = true;
      sessionStorage.setItem("auth_user", JSON.stringify(user));
    } else if (hadUser.current) {
      // Only clear when user actively logs out (was previously set), not on initial mount
      clearStoredSession();
    }
  }, [user]);

  const login = async (username: string, password: string) => {
    // Do NOT call setLoading here — it unmounts BrowserRouter and causes flicker.
    // Login.tsx manages its own busy/loading state.
    try {
      const response = await apiLogin({ username, password });

      if (response.ok && response.user && response.token) {
        const user: User = {
          id: response.user.id,
          name: response.user.name,
          username: response.user.username,
          role: response.user.role as UserRole,
          phone: response.user.phone,
          email: response.user.email,
          restaurantId: response.user.restaurantId,
          orderCount: response.user.orderCount,
          createdAt: response.user.createdAt
        };

        // Lưu theo phiên tab trước khi setUser để ProtectedRoute nhìn thấy ngay.
        sessionStorage.setItem("token", response.token);
        sessionStorage.setItem("auth_user", JSON.stringify(user));
        sessionStorage.setItem("role", user.role);

        setUser(user);
        return { ok: true, user, token: response.token };
      }

      return { ok: false, message: response.message || "Sai tên đăng nhập hoặc mật khẩu" };
    } catch (error: any) {
      console.error("💥 [AuthContext] Login error:", error);
      const message = error.response?.data?.message || error.message || "Có lỗi xảy ra, vui lòng thử lại";
      return { ok: false, message };
    }
  };

  const register = async (payload: RegisterPayload) => {
    try {
      // Validate payload
      if (!payload.email || !payload.fullName || !payload.phone || !payload.password) {
        return { ok: false, message: "Vui lòng điền đầy đủ thông tin" };
      }

      // Call backend API
      const response = await apiRegister({
        password: payload.password,
        fullName: payload.fullName.trim(),
        email: payload.email.trim().toLowerCase(),
        phone: payload.phone
      });

      if (response.ok && response.data) {
        const newUser: User = {
          id: response.data.id,
          name: response.data.name,
          username: response.data.username,
          email: response.data.email,
          phone: response.data.phone,
          role: response.data.role as UserRole || 'customer',
          orderCount: response.data.orderCount || 0,
          createdAt: response.data.createdAt || Date.now()
        };

        return { ok: true, data: newUser };
      }

      return { ok: false, message: response.message || "Có lỗi xảy ra, vui lòng thử lại" };
    } catch (error: any) {
      console.error("💥 [AuthContext] Registration error:", error);
      const message = error.response?.data?.message || error.message || "Có lỗi xảy ra, vui lòng thử lại";
      return { ok: false, message };
    }
  };

  const logout = () => {
    // Xóa đồng bộ trước khi dashboard chuyển trang để phiên cũ không bị khôi phục.
    realtimeSocket.disconnect();
    clearStoredSession();
    hadUser.current = false;
    setUser(null);
  };
  const isAdmin = () => user?.role === "admin";
  const isRestaurant = () => user?.role === "restaurant";
  const isCustomer = () => user?.role === "customer";
  const setPhone = (phone: string) => user && setUser({ ...user, phone });

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, isAdmin, isRestaurant, isCustomer, setPhone }}>
      {loading ? (
        <div style={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          height: '100vh',
          fontSize: '18px',
          color: '#666'
        }}>
          Đang tải...
        </div>
      ) : (
        children
      )}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextValue => {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    console.error("useAuth must be used inside AuthProvider");
    // Return a safe fallback instead of throwing
    return {
      user: null,
      loading: false,
      login: async () => ({ ok: false, message: "Chức năng đăng nhập chưa sẵn sàng" }),
      register: async () => ({ ok: false, message: "Chức năng đăng ký chưa sẵn sàng" }),
      logout: () => {},
      isAdmin: () => false,
      isRestaurant: () => false,
      isCustomer: () => false,
      setPhone: () => {}
    };
  }
  return ctx;
};
